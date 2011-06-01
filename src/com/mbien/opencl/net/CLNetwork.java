/*
 * Created on Monday, May 16 2011 20:51
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import com.mbien.opencl.net.remote.RemoteNode;
import com.mbien.opencl.net.shoal.GMSGridNodeController;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of nodes in a network.
 * @author Michael Bien
 */
public abstract class CLNetwork {

    public static CLNetwork createNetwork(String group) {
        return new GMSGridNodeController(group);
    }

    /**
     * Connects the node to the network.
     */
    public abstract void startNode(String name);

    /**
     * Disconnects the node.
     */
    public abstract void shutdownNode();

    /**
     * Returns the node representing this JVM instance.
     */
    public abstract LocalNode getLocalNode();

    /**
     * Returns all available nodes of this network.
     */
    public abstract List<RemoteNode> getRemoteNodes();

    /**
     * Returns all available OpenCL platforms of this network.
     */
    public List<CLPlatform> getPlatforms(Filter<CLPlatform>... filters) {

        List<CLPlatform> list = new ArrayList<CLPlatform>();
        List<RemoteNode> nodes = getRemoteNodes();

        for (RemoteNode node : nodes) {

            CLPlatform[] platforms = node.listPlatforms();

            for (CLPlatform platform : platforms) {
                boolean accepted = true;
                for (Filter<CLPlatform> filter : filters) {
                    if(!filter.accept(platform)) {
                        accepted = false;
                        break;
                    }
                }
                if(accepted) {
                    list.add(platform);
                }
            }
        }
        return list;
    }

}
