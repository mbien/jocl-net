/*
 * Created on Thursday, May 19 2011 21:40
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CLBufferBinding;
import com.jogamp.opencl.llb.CLCommandQueueBinding;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.CLEventBinding;
import com.jogamp.opencl.llb.CLImageBinding;
import com.jogamp.opencl.llb.CLKernelBinding;
import com.jogamp.opencl.llb.CLMemObjBinding;
import com.jogamp.opencl.llb.CLProgramBinding;
import com.jogamp.opencl.llb.CLSamplerBinding;


/**
 * Remotely accessible CLPlatform.
 * @author Michael Bien
 */
public class CLRemotePlatform extends CLPlatform {

    private final CLRemoteContextBinding contextBinding;
    private final CLRemoteProgramBinding programBinding;
    private final CLRemoteKernelBinding kernelBinding;
    private final CLRemoteMemoryBinding memoryBinding;
    private final CLRemoteCommandQueueBinding queueBinding;
    private final CLRemoteSamplerBinding samplerBinding;
    private final CLRemoteEventBinding eventBinding;

    private final RemoteNode node;

    public CLRemotePlatform(long id, RemoteNode node, CLRemoteAccessorFactory factory) {
        super(id, factory);
        this.node = node;
        this.contextBinding = new CLRemoteContextBinding(node);
        this.programBinding = new CLRemoteProgramBinding(node);
        this.kernelBinding = new CLRemoteKernelBinding(node);
        this.memoryBinding = new CLRemoteMemoryBinding(node);
        this.queueBinding = new CLRemoteCommandQueueBinding(node);
        this.samplerBinding = new CLRemoteSamplerBinding(node);
        this.eventBinding = new CLRemoteEventBinding(node);
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

    @Override
    protected CLMemObjBinding getMemObjectBinding() {
        return memoryBinding;
    }

    @Override
    protected CLBufferBinding getBufferBinding() {
        return memoryBinding;
    }

    @Override
    protected CLImageBinding getImageBinding() {
        return memoryBinding;
    }

    @Override
    protected CLCommandQueueBinding getCommandQueueBinding() {
        return queueBinding;
    }

    @Override
    protected CLSamplerBinding getSamplerBinding() {
        return samplerBinding;
    }

    @Override
    protected CLEventBinding getEventBinding() {
        return eventBinding;
    }

}
