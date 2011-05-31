/*
 * Created on Saturday, May 28 2011 04:12
 */
package com.mbien.opencl.net.remote;

import com.jogamp.common.nio.Buffers;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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

    @Override
    public void putBytes(ByteBuffer target, ByteBuffer source) {

        if(source == null) {
            target.putInt(0);
        }else{
            target.putInt(source.remaining());

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                target.put(source.get(sourcepos + i));
            }
        }

    }

    @Override
    public void putInts(ByteBuffer target, IntBuffer source) {

        if(source == null) {
            target.putInt(0);
        }else{
            target.putInt(source.remaining() * 4);

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                target.putInt(source.get(sourcepos + i));
            }
        }
    }

    @Override
    public void readBuffer(ByteChannel channel, IntBuffer target, ByteBuffer buffer) throws IOException {
        buffer.rewind();
        buffer.limit(target.remaining()*4);
        channel.read(buffer);
        for(int i = 0; i < target.remaining(); i++) {
            target.put(i, buffer.getInt(i*4));
        }
    }

}
