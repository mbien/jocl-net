/*
 * Created on Thursday, May 19 2011 21:40
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.CLKernelBinding;
import com.jogamp.opencl.llb.CLProgramBinding;


/**
 * Remotely accessible CLPlatform.
 * @author Michael Bien
 */
public class CLRemotePlatform extends CLPlatform {

    private final CLRemoteContextBinding contextBinding;
    private final CLRemoteProgramBinding programBinding;
    private final CLRemoteKernelBinding kernelBinding;

    private final RemoteNode node;

    public CLRemotePlatform(long id, RemoteNode node, CLRemoteAccessorFactory factory) {
        super(id, factory);
        this.node = node;
        this.contextBinding = new CLRemoteContextBinding(node);
        this.programBinding = new CLRemoteProgramBinding(node);
        this.kernelBinding = new CLRemoteKernelBinding(node);
    }

    @Override
    protected CLDevice createDevice(long id) {
        return new CLRemoteDevice(this, node, id);
    }

    @Override
    protected CLContextBinding getContextBinding() {
        return contextBinding;
    }

    @Override
    protected CLProgramBinding getProgramBinding() {
        return programBinding;
    }

    @Override
    protected CLKernelBinding getKernelBinding() {
        return kernelBinding;
    }

}
