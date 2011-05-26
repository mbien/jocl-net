/*
 * Created on Friday, May 13 2011 00:54
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.net.shoal.GMSGridNodeController;
import java.io.IOException;
import java.net.InetAddress;

/**
 *
 * @author Michael Bien
 */
public class CLServer {

    public static void main(String[] args) throws IOException {

//        LogManager.getLogManager().readConfiguration(new FileInputStream("/home/mbien/log.config"));

        GridNodeController gnc = new GMSGridNodeController("jocl-grid", "jocl-server-"+InetAddress.getLocalHost().getHostName());
        gnc.startNode();
        
        LocalNode node = gnc.getLocalNode();
        node.startServer(CLPlatform.listCLPlatforms());

    }
    
}
