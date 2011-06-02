/*
 * Created on Thursday, May 19 2011 00:51
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.net.remote.RemoteNode;
import java.util.List;
import org.junit.Test;

import static java.lang.System.*;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 */
public class ClientTest {

    @Test
    public void serverInfoTest() throws InterruptedException {

        out.println("server-info-test");

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("server-info-test");

        Thread.sleep(10000);

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
        network.startNode("remote-context-test");

        Thread.sleep(10000);

        List<CLPlatform> platforms = network.getPlatforms();
        assertTrue(!platforms.isEmpty());

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

        network.shutdownNode();
    }


}
