/*
 * Created on Thursday, May 19 2011 21:40
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CLContextBinding;


/**
 *
 * @author Michael Bien
 */
public class CLRemotePlatform extends CLPlatform {

    private final CLRemoteContextBinding contextBinding;

    private final RemoteNode node;

    public CLRemotePlatform(long id, RemoteNode node, CLRemoteAccessorFactory factory) {
        super(id, factory);
        this.node = node;
        this.contextBinding = new CLRemoteContextBinding(node);
    }

    @Override
    protected CLDevice createDevice(long id) {
        return new CLRemoteDevice(this, node, id);
    }

    @Override
    protected CLContextBinding getContextBinding() {
        return contextBinding;
    }

}
