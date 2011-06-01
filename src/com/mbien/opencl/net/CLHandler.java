/*
 * Created on Saturday, May 28 2011 14:57
 */
package com.mbien.opencl.net;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.llb.CL;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/**
 *
 * @author Michael Bien
 */
public abstract class CLHandler {

    private final ByteBuffer bb;
    
    protected final CL cl;

    public CLHandler(CL cl) {
        this.cl = cl;
        this.bb = Buffers.newDirectByteBuffer(40000);
    }

    public ByteBuffer getBuffer() {
        bb.clear();
        return bb;
    }
    
    protected abstract void handle(SocketChannel channel, int methodID) throws IOException;

    
}
