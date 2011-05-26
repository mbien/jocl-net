/*
 * Created on Monday, May 16 2011 20:51
 */
package com.mbien.opencl.net;

import com.mbien.opencl.net.remote.RemoteNode;
import com.mbien.opencl.net.shoal.GMSGridNodeController;
import java.util.List;

/**
 * @author Michael Bien
 */
public abstract class CLNetwork {

    public static CLNetwork createNetwork(String group) {
        return new GMSGridNodeController(group);
    }

    public abstract void startNode(String name);

    public abstract void shutdownNode();

    public abstract LocalNode getLocalNode();

    public abstract List<RemoteNode> getRemoteNodes();

}
