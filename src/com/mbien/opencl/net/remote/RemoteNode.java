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

    public RemoteNode(String group, String name, InetAddress address) {
        super(group, name, address);
        this.factory = new CLRemoteAccessorFactory(this);
    }

    SocketChannel connect() throws IOException {
        InetSocketAddress addr = new InetSocketAddress(address, 9000);
        return SocketChannel.open(addr);
    }

    public CLPlatform[] listPlatforms() {

        CLPlatform[] platforms = null;

        ByteBuffer buffer = newDirectByteBuffer(SIZEOF_BYTE+SIZEOF_INT);
        buffer.put(SPECIAL_AID).putInt(PLATFORM_IDS).rewind();

        SocketChannel channel = null;
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
        } catch (IOException ex) {
            LOGGER.log(WARNING, "can not list remote platforms.", ex);
        }finally{
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException ex) {
                    LOGGER.log(WARNING, "exception on close", ex);
                }
            }
        }

        return platforms;
    }


}
