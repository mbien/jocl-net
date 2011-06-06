/*
 * Created on Saturday, May 28 2011 14:57
 */
package com.mbien.opencl.net;

import com.jogamp.common.nio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;


/**
 *
 * @author Michael Bien
 */
public abstract class CLHandler {

    private final ByteBuffer bb;
    

    public CLHandler() {
        this.bb = Buffers.newDirectByteBuffer(40000);
    }

    public ByteBuffer getBuffer() {
        bb.clear();
        return bb;
    }
    
    protected abstract void handle(ByteChannel channel, int methodID) throws IOException;

    
}
