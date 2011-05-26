/*
 * Created on Monday, May 16 2011 20:51
 */
package com.mbien.opencl.net;

import com.mbien.opencl.net.remote.RemoteNode;
import java.util.List;

/**
 *
 * @author Michael Bien
 */
public abstract interface GridNodeController {

    public abstract void startNode();

    public abstract void shutdownNode();

    public abstract LocalNode getLocalNode();

    public abstract List<RemoteNode> getRemoteNodes();

}
