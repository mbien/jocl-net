/*
 * Created on Friday, May 13 2011 00:54
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.tracer.QueuePoller;
import com.mbien.opencl.net.delegate.CLCommandQueueDelegate;
import java.io.IOException;
import java.net.InetAddress;

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

//        CLCommandQueueDelegate delegate = new CLCommandQueueDelegate(CLPlatform.getLowLevelCLInterface());
//        node.setCLQueueBinding(delegate);
//        new QueuePoller(delegate).start();

        node.startServer(CLPlatform.listCLPlatforms());

    }
    
}
