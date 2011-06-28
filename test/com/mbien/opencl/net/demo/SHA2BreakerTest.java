/*
 * Created on Saturday, June 18 2011 20:02
 */
package com.mbien.opencl.net.demo;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLProgram;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.junit.Test;

/**
 *
 * @author Michael Bien
 */
public class SHA2BreakerTest {


    @Test
    public void containsTest() throws IOException {
        CLContext context = CLContext.create();

        try{
            CLProgram program = context.createProgram(getClass().getResourceAsStream("stringgen.cl")).build();
            System.out.println(program.getBuildLog());
            CLKernel contains = program.createCLKernel("contains");
            CLCommandQueue queue = context.getMaxFlopsDevice().createCommandQueue();

            CLBuffer<ShortBuffer> buffer = context.createShortBuffer(9, Mem.READ_ONLY);
            CLBuffer<ShortBuffer> item = context.createShortBuffer(3, Mem.READ_ONLY);
            CLBuffer<IntBuffer> result = context.createIntBuffer(1, Mem.WRITE_ONLY);

            buffer.getBuffer().put(new short[]{1,2,1, 2,2,2, 3,2,3}).rewind();
            item.getBuffer().put(new short[]{2,2,2}).rewind();
            result.getBuffer().put(0, -1);

            contains.setArgs(buffer, item, item.getNIOCapacity(), result);

            queue.putWriteBuffer(buffer, false);
            queue.putWriteBuffer(item, false);
            queue.putWriteBuffer(result, false);
            queue.put1DRangeKernel(contains, 0, 3, 0);
            queue.putReadBuffer(result, true);

            System.out.println(result.getBuffer().get());

        }finally{
            context.release();
        }


    }

}
