/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.opencl.net.remote;

import com.jogamp.common.nio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Michael Bien
 */
public abstract class CLRemoteBinding {

    private final ByteBuffer bb;
    private final RemoteNode node;

    public CLRemoteBinding(RemoteNode node) {
        this.node = node;
        this.bb = Buffers.newDirectByteBuffer(40000);
    }

    public ByteBuffer getBuffer() {
        bb.clear();
        return bb;
    }

    public ByteChannel getChannel() {
        try {
            return node.connect();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
