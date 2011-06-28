/*
 * Created on Monday, June 20 2011 15:08
 */
package com.mbien.opencl.net.demo;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.jogamp.opencl.util.concurrent.CLQueueContext;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Michael Bien
 */
public class SHA2BreakerContext extends CLQueueContext {

    CLKernel gen;
    CLKernel pad;
    CLKernel sha512;
    CLKernel contains;

    CLBuffer<ShortBuffer> permBuffer;
    CLBuffer<ShortBuffer> padBuffer;
    CLBuffer<ShortBuffer> digestBuffer;
    CLBuffer<ShortBuffer> hashBuffer;
    CLBuffer<IntBuffer> rangeBuffer;
    CLBuffer<IntBuffer> resultBuffer;

    int HASH_COUNT;
    int PW_LENGTH;

    final int start;
    final int range;

    private List<CLResource> resources;

    private static long pos = 0;

    public SHA2BreakerContext(CLCommandQueue queue, byte[] hash, int pwlenght, int start, int range) {
        super(queue);
        this.start = start;
        this.range = range;
        try {
            init(queue, hash, pwlenght);
        } catch (IOException ex) {
            throw new RuntimeException("unable to initialize context.", ex);
        }
    }


    private void init(CLCommandQueue queue, byte[] pwHash, int length) throws IOException {

        HASH_COUNT = 400000;
        PW_LENGTH = length;

        CLContext context = queue.getContext();

        // buffers
        // read/write
        permBuffer    = context.createShortBuffer(PW_LENGTH*HASH_COUNT, Mem.READ_WRITE);
        padBuffer     = context.createShortBuffer(128*HASH_COUNT, Mem.READ_WRITE);
        digestBuffer  = context.createShortBuffer(64*HASH_COUNT, Mem.READ_WRITE);

        // read only
        hashBuffer    = context.createShortBuffer(64, Mem.READ_ONLY);
        rangeBuffer   = context.createIntBuffer(2*HASH_COUNT, Mem.READ_ONLY);

        // output
        resultBuffer  = context.createIntBuffer(1, Mem.WRITE_ONLY);

        // program
        CLProgram program = context.createProgram(
                SHA2Breaker.class.getResourceAsStream("sha512.cl"),
                SHA2Breaker.class.getResourceAsStream("stringgen.cl")
            ).build();

        System.out.println(program.getBuildLog());

        gen = program.createCLKernel("gen", permBuffer, start, PW_LENGTH, 0L, range);
        pad = program.createCLKernel("pad", permBuffer, padBuffer, PW_LENGTH);
        sha512 = program.createCLKernel("sha512", padBuffer, digestBuffer, rangeBuffer);
        contains = program.createCLKernel("contains", digestBuffer, hashBuffer, 64, resultBuffer);

        // initial values
        int offset = 0;
        for (int m = 0; m < HASH_COUNT; m++) {
            rangeBuffer.getBuffer().put(offset);
            rangeBuffer.getBuffer().put(128);
            offset += 128;
        }
        rangeBuffer.getBuffer().rewind();

        for (int i = 0; i < pwHash.length; i++) {
            hashBuffer.getBuffer().put((short)(pwHash[i]&0xff));
            System.out.print(hashBuffer.getBuffer().get(i) +" ");
        }
        System.out.println("");
        hashBuffer.getBuffer().rewind();

        resultBuffer.getBuffer().put(0, -1);

        queue.putWriteBuffer(rangeBuffer, false);
        queue.putWriteBuffer(padBuffer, false);
        queue.putWriteBuffer(hashBuffer, false);
        queue.putWriteBuffer(resultBuffer, false);

        resources = new ArrayList<CLResource>();
        resources.addAll(Arrays.asList(
                (CLResource)program, permBuffer, padBuffer, digestBuffer, hashBuffer, rangeBuffer, resultBuffer));
    }

    public long nextRound() {
        synchronized(SHA2BreakerContext.class) {
            long old = pos;
            pos += HASH_COUNT;
            return old;
        }
    }

    @Override
    public void release() {
        for (CLResource resource : resources) {
            resource.release();
        }
        resources.clear();
    }

    @Override
    public boolean isReleased() {
        return resources.isEmpty();
    }

}
