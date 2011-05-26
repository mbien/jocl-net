/*
 * Created on Wednesday, May 25 2011 04:08
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CL;
import com.jogamp.opencl.spi.CLAccessorFactory;
import com.jogamp.opencl.spi.CLInfoAccessor;
import com.jogamp.opencl.spi.CLPlatformInfoAccessor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import static com.jogamp.common.nio.Buffers.*;
import static java.util.logging.Level.*;

/**
 *
 * @author Michael Bien
 */
public class CLRemoteAccessorFactory implements CLAccessorFactory {

    private final RemoteNode node;

    public CLRemoteAccessorFactory(RemoteNode node) {
        this.node = node;
    }

    @Override
    public CLInfoAccessor createDeviceInfoAccessor(CL cl, long id) {
        return new CLRemoteInfoAccessor(node, RemoteNode.DEVICE_AID, id);
    }

    @Override
    public CLPlatformInfoAccessor createPlatformInfoAccessor(CL cl, long id) {
        return new CLRemotePlatformInfoAccessor(node, id);
    }


    public static class CLRemotePlatformInfoAccessor extends CLRemoteInfoAccessor implements CLPlatformInfoAccessor {

        private static final Logger LOGGER = Logger.getLogger(CLRemotePlatformInfoAccessor.class.getName());
        public final static int MID_DEVICES = 4;

        public CLRemotePlatformInfoAccessor(RemoteNode node, long clID) {
            super(node, (byte)RemoteNode.PLATFORM_AID, clID);
        }

        @Override
        public long[] getDeviceIDs(long type) {
            SocketChannel channel = null;

            try {
                ByteBuffer buffer = newDirectByteBuffer(SIZEOF_BYTE+SIZEOF_INT+SIZEOF_LONG+SIZEOF_LONG);
                buffer.put(AID).putInt(MID_DEVICES).putLong(clID).putLong(type).rewind();
                channel = writeHeader(buffer);

                buffer.limit(SIZEOF_INT);
                channel.read(buffer);
                buffer.flip();

                int count = buffer.getInt();
                ByteBuffer idBuffer = newDirectByteBuffer(count*SIZEOF_LONG);
                channel.read(idBuffer);
                idBuffer.rewind();

                long[] ids = new long[count];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = idBuffer.getLong();
                }

                return ids;

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

    }

}
