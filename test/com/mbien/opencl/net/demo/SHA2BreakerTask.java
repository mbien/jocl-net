/*
 * Created on Monday, June 20 2011 15:35
 */
package com.mbien.opencl.net.demo;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.util.concurrent.CLTask;

import static java.util.Arrays.*;

/**
 * SHA-512 breaker task.
 * @author Michael Bien
 */
public class SHA2BreakerTask implements CLTask<SHA2BreakerContext, String> {

    private int iteration;
    private String device;

    @Override
    public String execute(SHA2BreakerContext ctx) {
        
        CLCommandQueue queue = ctx.getQueue();
        StringPermutation perm = new StringPermutation(ctx.PW_LENGTH, (char)ctx.start, ctx.range);
        device = queue.getDevice().getName();

        long delta = System.currentTimeMillis();

        final int WG_SIZE = 0;
        
        while(!Thread.currentThread().isInterrupted()) {

            long round = ctx.nextRound();

            ctx.gen.setArg(3, round); // generator offset
            System.out.println(new String(perm.get(round)) +" / round: "+round);

            queue.put1DRangeKernel(ctx.gen, 0, ctx.HASH_COUNT, WG_SIZE);       // generate strings
            queue.put1DRangeKernel(ctx.pad, 0, ctx.HASH_COUNT, WG_SIZE);       // pad each string to 1024bit length
            queue.put1DRangeKernel(ctx.sha512, 0, ctx.HASH_COUNT, WG_SIZE);    // generate hash for every string
            queue.put1DRangeKernel(ctx.contains, 0, ctx.HASH_COUNT, WG_SIZE);  // check if the buffer contains our pw hash

            queue.putReadBuffer(ctx.resultBuffer, true);

            iteration++;

            // check if we found a matching hash
            if(ctx.resultBuffer.getBuffer().get(0) >= 0) {

                delta = System.currentTimeMillis()-delta;

                System.out.println(delta/1000.0f+"s");
                System.out.println((round/(delta/1000.0f)/1000000)+" million hashes/s");

                queue.putReadBuffer(ctx.permBuffer, true);
                int item = ctx.resultBuffer.getBuffer().get(0);

                StringBuilder password = new StringBuilder();
                for (int i = 0; i < ctx.PW_LENGTH; i++) {
                    int index = i + ctx.PW_LENGTH*item;
                    password.append((char)ctx.permBuffer.getBuffer().get(index));
                }
                return password.toString();
            }

        }

        return null;
    }


    public int getIterations() {
        return iteration;
    }

    public String getDeviceName() {
        return device;
    }

    private static class StringPermutation{

        private final char[] array;
        private final int base;
        private final char start;
        private long value;

        public StringPermutation(int size, char start, int base) {
            this.start = start;
            this.base = base;
            this.array = new char[size];
            fill(array, start);
        }

        public byte[] next() {

            long v = value;
            get(v);
            value++;

            return new String(array).getBytes();

        }

        public byte[] get(long v) {

            for (int i = array.length-1; i >= 0; i--) {
                long k = v/base;
                array[i] = (char) (v-base*k + start);
                v = k;
            }

            return new String(array).getBytes();

        }

        public String current() {
            return new String(array);
        }

    }

}
