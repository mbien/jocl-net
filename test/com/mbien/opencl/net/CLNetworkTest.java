/*
 * Created on Thursday, May 19 2011 00:51
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLSampler;
import com.jogamp.opencl.CLSampler.AddressingMode;
import com.jogamp.opencl.CLSampler.FilteringMode;
import com.jogamp.opencl.CLUserEvent;
import com.jogamp.opencl.util.CLMultiContext;
import com.mbien.opencl.net.remote.RemoteNode;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jogamp.opencl.CLVersion.*;
import static com.jogamp.opencl.util.CLPlatformFilters.*;
import static com.jogamp.common.nio.Buffers.*;
import static java.lang.System.*;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 */
public class CLNetworkTest {

    private final static String programSource =
      "kernel void compute(global int* array, int numElements) { \n"
    + "    int index = get_global_id(0);                         \n"
    + "    if (index >= numElements)  {                          \n"
    + "        return;                                           \n"
    + "    }                                                     \n"
    + "    array[index]++;                                       \n"
    + "}                                                         \n";

    private static CLNetwork network;

    @BeforeClass
    public static void init() throws InterruptedException{
        network = CLNetwork.createNetwork("jocl-net");
        network.startNode("test-node-"+System.currentTimeMillis());

        out.println("waiting for nodes to appear..");
        waitForNodes(network);
    }

    @AfterClass
    public static void deinit() {
        network.shutdownNode();
        network = null;
    }

    @Test
    public void serverInfoTest() throws InterruptedException {

        out.println("server-info-test");

        List<RemoteNode> remoteNodes = network.getRemoteNodes();
        out.println("network: "+remoteNodes);

        assertTrue(!remoteNodes.isEmpty());

        long time = System.currentTimeMillis();
        for (RemoteNode node : remoteNodes) {

            out.println(node);

            CLPlatform[] platforms = node.listPlatforms();
//            CLPlatform[] platforms = CLPlatform.listCLPlatforms();

            for (CLPlatform platform : platforms) {

                assertNotNull(platform);

                out.println("    "+platform);

                CLDevice[] devices = platform.listCLDevices();
                assertNotNull(devices);
                assertTrue(devices.length > 0);

                for (CLDevice device : devices) {
                    out.println("        "+device);
                }

            }
        }

        out.println("time needed: "+(System.currentTimeMillis()-time));

        network.shutdownNode();
    }

    @Test
    public void remoteContextTest() throws InterruptedException {

        out.println("remote-context-test");

        List<CLPlatform> platforms = network.getPlatforms();
        assertTrue(!platforms.isEmpty());
//        for (int i = 0; i < 100; i++) {

        for (CLPlatform platform : platforms) {

            out.println("create remote context on "+platform);
            CLContext context = CLContext.create(platform);
            assertNotNull(context);

            try{
                out.println("    "+context);

                CLImageFormat[] formats = context.getSupportedImage2dFormats();
                assertNotNull(formats);
                assertTrue(formats.length > 0);

                out.println("    #2dformats: "+formats.length);
//                for (CLImageFormat format : formats) {
//                    out.println("    "+format);
//                }

                formats = context.getSupportedImage3dFormats();
                assertNotNull(formats);
                assertTrue(formats.length > 0);

                out.println("    #3dformats: "+formats.length);
//                for (CLImageFormat format : formats) {
//                    out.println("    "+format);
//                }


                CLDevice[] devices = context.getDevices();
                assertNotNull(devices);
                assertTrue(devices.length > 0);

                for (CLDevice device : devices) {
                    out.println("    "+device);
                }

            }finally{
                context.release();
            }
        }
//        }

        network.shutdownNode();
    }


    @Test
    public void remoteProgramTest() throws InterruptedException {

        out.println("remote-program-test");

        List<CLPlatform> platforms = network.getPlatforms();
        CLMultiContext mc = CLMultiContext.create(platforms.toArray(new CLPlatform[platforms.size()]));

        Random rnd = new Random();
        try{
//            for (int i = 0; i < 100; i++) {

            List<CLContext> contexts = mc.getContexts();
            for (CLContext context : contexts) {
                out.println(context);

                CLProgram program = context.createProgram(programSource);
                out.println(program);

                assertFalse(program.isExecutable());
                program.build();

                assertTrue(program.isExecutable());

                out.println(program.getBuildLog());

                Map<String, CLKernel> kernels = program.createCLKernels();
                assertNotNull(kernels);
                assertFalse(kernels.isEmpty());
                assertNotNull(kernels.containsKey("compute"));

                for (CLKernel kernel : kernels.values()) {
                    out.println(kernel);
                }

                // buffer test
                int size = 1024;
                CLBuffer<?> bufferA = context.createBuffer(size, Mem.READ_WRITE);
                System.out.println(bufferA +" size: "+bufferA.getCLSize());
                assertEquals(size, bufferA.getCLSize());

                CLBuffer<?> bufferB = context.createBuffer(size, Mem.READ_WRITE);

                ByteBuffer reference = newDirectByteBuffer(size);
                while(reference.hasRemaining()) {
                    reference.putInt(rnd.nextInt());
                }
                reference.rewind();

                bufferA = bufferA.cloneWith(reference);

                // commandqueue test
                CLDevice[] devices = context.getDevices();
                for (CLDevice device : devices) {
                    CLCommandQueue queue = device.createCommandQueue();
                    System.out.println(queue);

                    // write, copy read, check
                    queue.putWriteBuffer(bufferA, true);
                    queue.putCopyBuffer(bufferA, bufferB);

                    CLBuffer<ByteBuffer> result = bufferB.cloneWith(newDirectByteBuffer(64));
                    queue.putReadBuffer(result, true);

                    while(result.getBuffer().hasRemaining()) {
                        assertEquals(reference.get(), result.getBuffer().get());
                    }
                    reference.rewind();
                    result.getBuffer().rewind();

                    // execute kernel, check again
                    kernels.get("compute").setArg(0, result).setArg(1, size/4);
                    queue.put1DRangeKernel(kernels.get("compute"), 0, size/4, 0);
                    queue.putReadBuffer(result, true);
                    while(result.getBuffer().hasRemaining()) {
                        assertEquals(reference.getInt()+1, result.getBuffer().getInt());
                    }
                    reference.rewind();

                    queue.release();
                }

                // test if release works
                bufferA.release();
                for (CLKernel kernel : kernels.values()) {
                    kernel.release();
                }
                program.release();
                
            }
//            }
        }finally{
            mc.release();
        }

        network.shutdownNode();

    }
    
    @Test
    public void cl11Test() {

        out.println("cl1.1-test");

        List<CLPlatform> platforms = network.getPlatforms(version(CL_1_1));
        assertFalse(platforms.isEmpty());

        CLMultiContext mc = CLMultiContext.create(platforms.toArray(new CLPlatform[platforms.size()]));

        try{
            for (CLContext context : mc.getContexts()) {
                CLSampler sampler = context.createSampler(AddressingMode.CLAMP, FilteringMode.NEAREST, true);
                System.out.println(sampler);
                assertEquals(AddressingMode.CLAMP, sampler.getAddressingMode());
                assertEquals(FilteringMode.NEAREST, sampler.getFilteringMode());
                sampler.release();

                CLUserEvent event = CLUserEvent.create(context);
                assertEquals(CLEvent.ExecutionStatus.SUBMITTED, event.getStatus());
                event.setComplete();
                assertTrue(event.isComplete());
            }
        }finally{
            mc.release();
        }
    }

    private static void waitForNodes(CLNetwork network) throws InterruptedException {
        while(network.getRemoteNodes().isEmpty()) {
            Thread.sleep(1000);
        }
    }


}
