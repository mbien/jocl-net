/*
 * Created on Thursday, May 19 2011 00:51
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.net.remote.RemoteNode;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Michael Bien
 */
public class ClientTest {

    @Test
    public void serverInfoTest() throws InterruptedException {

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("master");

//        Thread.sleep(5000);

        List<RemoteNode> remoteNodes = network.getRemoteNodes();

        System.out.println("grid: "+remoteNodes);

        long time = System.currentTimeMillis();
        for (RemoteNode node : remoteNodes) {

            System.out.println(node);

            CLPlatform[] platforms = node.listPlatforms();
//            CLPlatform[] platforms = CLPlatform.listCLPlatforms();

            for (CLPlatform platform : platforms) {
                System.out.println("    "+platform);

                CLDevice[] devices = platform.listCLDevices();
                for (CLDevice device : devices) {
                    System.out.println("        "+device);
                }

                System.out.println("        create remote context...");
                CLContext context = CLContext.create(platform);
                System.out.println("        "+context.toString());
                System.out.println("        contextID: "+context.ID);
                CLDevice[] devices2 = context.getDevices();
                for (CLDevice device : devices2) {
                    System.out.println("        "+device+" (context)");
                }
                context.release();
            }

        }
        System.out.println("time needed: "+(System.currentTimeMillis()-time));


        network.shutdownNode();
    }
}
