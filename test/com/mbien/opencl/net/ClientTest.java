/*
 * Created on Thursday, May 19 2011 00:51
 */
package com.mbien.opencl.net;

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

        for (RemoteNode node : remoteNodes) {

            System.out.println(node);

            CLPlatform[] platforms = node.listPlatforms();

            for (CLPlatform platform : platforms) {
                System.out.println("    "+platform);

                CLDevice[] devices = platform.listCLDevices();
                for (CLDevice device : devices) {
                    System.out.println("        "+device);
                }
            }

        }


        network.shutdownNode();
    }
}
