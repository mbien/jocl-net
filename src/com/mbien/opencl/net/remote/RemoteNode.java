/*
 * Created on Thursday, May 19 2011 01:00
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.net.GridNode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static com.jogamp.common.nio.Buffers.*;
import static java.util.logging.Level.*;

/**
 * Represents a remotely accessible node in a network.
 * @author Michael Bien
 */
public class RemoteNode extends GridNode {

    private static final Logger LOGGER = Logger.getLogger(RemoteNode.class.getName());

    public static final byte SPECIAL_AID = 0;
    public static final byte PLATFORM_AID = 1;
    public static final byte DEVICE_AID = 2;

    public static final int PLATFORM_IDS = 1;

    private final CLRemoteAccessorFactory factory;
    private final InetSocketAddress connectionAddress;

    private SocketChannel sc;

    public RemoteNode(String group, String name, InetAddress address) {
        super(group, name, address);
        this.connectionAddress = new InetSocketAddress(address, 10000);
        this.factory = new CLRemoteAccessorFactory(this);
    }

    public ByteChannel connect() throws IOException {
        if(sc == null || !sc.isConnected()) {
            sc = SocketChannel.open(connectionAddress);
        }
        return sc;
    }

    public void disconnect() {
        if(sc != null && sc.isConnected()) {
            try {
                sc.close();
            } catch (IOException ex) {
                LOGGER.log(WARNING, null, ex);
            }
        }
    }

    public CLPlatform[] listPlatforms() {

        CLPlatform[] platforms = null;

        ByteBuffer buffer = newDirectByteBuffer(SIZEOF_BYTE+SIZEOF_INT);
        buffer.put(SPECIAL_AID).putInt(PLATFORM_IDS).rewind();

        ByteChannel channel = null;
        try {

            channel = connect();
            channel.write(buffer);
            buffer.rewind();

            buffer.limit(SIZEOF_INT);
            channel.read(buffer);
            buffer.rewind();

            int count = buffer.getInt(0);
            ByteBuffer ids = newDirectByteBuffer(count*SIZEOF_LONG);
            channel.read(ids);

            ids.rewind();

            platforms = new CLPlatform[count];
            for (int i = 0; i < count; i++) {
                long id = ids.getLong();
                platforms[i] = new CLRemotePlatform(id, this, factory);
            }
        } catch (Exception ex) {
            LOGGER.log(WARNING, "can not list remote platforms.", ex);
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException ex2) {
                    LOGGER.log(WARNING, "exception on close", ex2);
                }
            }
        }

        return platforms;
    }


}
