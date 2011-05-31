/*
 * Created on Friday, May 13 2011 00:54
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLPlatform;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.LogManager;

/**
 *
 * @author Michael Bien
 */
public class CLServer {

    public static void main(String[] args) throws IOException {

//        LogManager.getLogManager().readConfiguration(new FileInputStream("/home/mbien/log.config"));

        CLNetwork network = CLNetwork.createNetwork("jocl-net");
        network.startNode("jocl-server-"+InetAddress.getLocalHost().getHostName());
        
        LocalNode node = network.getLocalNode();
        node.startServer(CLPlatform.listCLPlatforms());

    }
    
}
