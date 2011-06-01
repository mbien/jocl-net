/*
 * Created on Saturday, May 28 2011 04:12
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
public class CLRemoteContextBinding extends CLAbstractRemoteContextBinding {

    private final ByteBuffer bb;
    private final RemoteNode node;

    public CLRemoteContextBinding(RemoteNode node) {
        this.node = node;
        this.bb = Buffers.newDirectByteBuffer(40000);
    }

    @Override
    public ByteBuffer getBuffer() {
        bb.clear();
        return bb;
    }

    @Override
    public ByteChannel getChannel() {
        try {
            return node.connect();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
