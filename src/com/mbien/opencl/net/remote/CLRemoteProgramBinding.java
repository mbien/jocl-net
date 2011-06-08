/*
 * Created on Monday, June 06 2011 17:49
 */
package com.mbien.opencl.net.remote;

import com.jogamp.common.nio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author mbien
 */
public class CLRemoteProgramBinding extends CLAbstractRemoteProgramBinding {

    private final ByteBuffer bb;
    private final RemoteNode node;

    public CLRemoteProgramBinding(RemoteNode node) {
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