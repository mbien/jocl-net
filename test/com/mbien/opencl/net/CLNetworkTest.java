/*
 * Created on Thursday, May 19 2011 00:51
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.util.CLMultiContext;
import com.mbien.opencl.net.remote.RemoteNode;
import java.util.List;
import java.util.Map;
import org.junit.Test;

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

    @Test
    public void serverInfoTest() throws InterruptedException {

        out.println("server-info-test");

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("test-node");

        waitForNodes(network);

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

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("test-node");

        waitForNodes(network);

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

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("test-node");

        waitForNodes(network);
        List<CLPlatform> platforms = network.getPlatforms();
        CLMultiContext mc = CLMultiContext.create(platforms.toArray(new CLPlatform[platforms.size()]));

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

                for (CLKernel kernel : kernels.values()) {
                    out.println(kernel);
                    kernel.release();
                }

                program.release();

                // buffer test
                CLBuffer<?> buffer = context.createBuffer(64, Mem.READ_WRITE);
                System.out.println(buffer +" size: "+buffer.getCLSize());
                assertEquals(64, buffer.getCLSize());
                buffer.release();
            }
//            }
        }finally{
            mc.release();
        }

        network.shutdownNode();

    }

    private void waitForNodes(CLNetwork network) throws InterruptedException {
        while(network.getRemoteNodes().isEmpty()) {
            Thread.sleep(1000);
        }
    }


}
