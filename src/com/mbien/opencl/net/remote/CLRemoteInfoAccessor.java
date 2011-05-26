/*
 * Created on Friday, May 20 2011 23:01
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.spi.CLInfoAccessor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static com.jogamp.common.nio.Buffers.*;
import static java.util.logging.Level.*;

/**
 *
 * @author Michael Bien
 */
public class CLRemoteInfoAccessor implements CLInfoAccessor {
    
    private static final Logger LOGGER = Logger.getLogger(CLRemoteInfoAccessor.class.getName());

    public final byte AID;
    public final static int MID_STRING = 1;
    public final static int MID_LONG = 2;

    public final long clID;
    private final RemoteNode node;

    public CLRemoteInfoAccessor(RemoteNode node, byte accessorID, long clID) {
        this.AID = accessorID;
        this.clID = clID;
        this.node = node;
    }
    
    protected ByteBuffer prepareHeader(int methodID, int key) {
        ByteBuffer buffer = newDirectByteBuffer(SIZEOF_BYTE+SIZEOF_INT+SIZEOF_LONG+SIZEOF_INT);
        buffer.put(AID).putInt(methodID).putLong(clID).putInt(key).rewind();
        return buffer;
    }

    protected SocketChannel writeHeader(ByteBuffer buffer) throws IOException {
        SocketChannel channel = node.connect();
        channel.write(buffer);
        buffer.rewind();
        return channel;
    }

    @Override
    public int[] getInts(int key, int n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLong(int key) {
        SocketChannel channel = null;

        try {
            ByteBuffer buffer = prepareHeader(MID_LONG, key);
            channel = writeHeader(buffer);

            buffer.limit(SIZEOF_LONG);
            channel.read(buffer);
            buffer.flip();

            return buffer.getLong();

        } catch (IOException ex) {
            throw new RuntimeException("can not retrieve value", ex);
        }finally{
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException ex) {
                    LOGGER.log(SEVERE, "unable to close channel", ex);
                }
            }
        }
    }

    @Override
    public String getString(int key) {

        SocketChannel channel = null;

        try {
            ByteBuffer buffer = prepareHeader(MID_STRING, key);
            channel = writeHeader(buffer);

            buffer.limit(SIZEOF_INT);
            channel.read(buffer);
            buffer.flip();

            int length = buffer.getInt();
            ByteBuffer bytes = ByteBuffer.allocate(length).order(ByteOrder.nativeOrder());
            channel.read(bytes);
            channel.close();
            buffer.rewind();

            return new String(bytes.array());

        } catch (IOException ex) {
            throw new RuntimeException("can not retrieve value", ex);
        }finally{
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException ex) {
                    LOGGER.log(SEVERE, "unable to close channel", ex);
                }
            }
        }

    }

    public byte getAID() {
        return AID;
    }

}
