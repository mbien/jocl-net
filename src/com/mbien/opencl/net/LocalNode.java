/*
 * Created on Thursday, May 19 2011 16:11
 */
package com.mbien.opencl.net;

import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.spi.CLInfoAccessor;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
import com.mbien.opencl.net.handler.CLCommandQueueHandler;

import com.mbien.opencl.net.remote.CLRemoteAccessorFactory.CLRemotePlatformInfoAccessor;
import com.mbien.opencl.net.handler.CLContextHandler;
import com.mbien.opencl.net.handler.CLEventHandler;
import com.mbien.opencl.net.handler.CLKernelHandler;
import com.mbien.opencl.net.handler.CLMemoryHandler;
import com.mbien.opencl.net.handler.CLProgramHandler;
import com.mbien.opencl.net.handler.CLSamplerHandler;
import com.mbien.opencl.net.remote.CLRemoteCommandQueueBinding;
import com.mbien.opencl.net.remote.CLRemoteContextBinding;
import com.mbien.opencl.net.remote.CLRemoteEventBinding;
import com.mbien.opencl.net.remote.CLRemoteInfoAccessor;
import com.mbien.opencl.net.remote.CLRemoteKernelBinding;
import com.mbien.opencl.net.remote.CLRemoteMemoryBinding;
import com.mbien.opencl.net.remote.CLRemoteProgramBinding;
import com.mbien.opencl.net.remote.CLRemoteSamplerBinding;
import com.mbien.opencl.net.remote.RemoteNode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.jogamp.common.nio.Buffers.*;
import static com.mbien.opencl.net.util.NetBuffers.*;
import static java.util.logging.Level.*;

/**
 * A node running on the same JVM as the host application.
 * @author Michael Bien
 */
public class LocalNode extends GridNode {

    private static final Logger LOGGER = Logger.getLogger(LocalNode.class.getName());
    private final InetSocketAddress listeningAddress = new InetSocketAddress(10000);
    private CLHandler[] handlers;

    public LocalNode(String group, String name) {
        super(group, name, null);
        handlers = new CLHandler[10];
    }

    /**
     * Starts the server serving the given OpenCL platforms.
     */
    public void startServer(CLPlatform[] platforms) {

        initializeHandlers(platforms);

        ServerSocketChannel server = null;
        try {
            server = ServerSocketChannel.open();
            server.socket().bind(listeningAddress);
        } catch (IOException ex) {
            LOGGER.log(SEVERE, "unable to initialize channel", ex);
            return;
        }

        ByteBuffer buffer = newDirectByteBuffer(SIZEOF_BYTE + SIZEOF_INT);

        SocketChannel channel = null;
        while (true) {
            try {
                if(channel == null || !channel.isOpen()) {
                    channel = server.accept();
                }
                
                int bytesRead = channel.read(buffer);

                if(bytesRead < 0) {
                    LOGGER.warning("client terminated connection.");
                    channel.close();
                }else{
                    buffer.rewind();

                    byte accessorID = buffer.get();
                    int methodID = buffer.getInt();
                    buffer.rewind();

                    if (accessorID >= 0 && accessorID < handlers.length) {
                        CLHandler accessor = handlers[accessorID];
                        accessor.handle(channel, methodID);
                    } else {
                        LOGGER.warning("ignoring command: [" + accessorID + ", " + methodID + "]");
                    }
                }

            } catch (Exception ex) {
                LOGGER.log(SEVERE, "exception in server loop ["+channel+"]", ex);
                buffer.rewind();
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (Exception ex2) {
                        LOGGER.log(WARNING, "can not close channel.", ex2);
                    }
                }
            }
        }
    }

    private void insertHandler(int index, CLHandler handler) {
        if (handlers[index] != null) {
            throw new IllegalStateException("slot not empty");
        }
        handlers[index] = handler;
    }

    private void initializeHandlers(final CLPlatform[] platforms) {

        Map<Long, CLPlatform> platformMap = new HashMap<Long, CLPlatform>(platforms.length);
        Map<Long, CLDevice> deviceMap = new HashMap<Long, CLDevice>(platforms.length);

        for (CLPlatform platform : platforms) {

            platformMap.put(platform.ID, platform);

            CLDevice[] devices = platform.listCLDevices();
            for (CLDevice device : devices) {
                deviceMap.put(device.ID, device);
            }
        }

        CL cl = CLPlatform.getLowLevelCLInterface();

        // accessors
        insertHandler(RemoteNode.SPECIAL_AID, new CLStaticPlatformHandler(platforms));
        insertHandler(RemoteNode.PLATFORM_AID, new CLPlatformHandler(platformMap));
        insertHandler(RemoteNode.DEVICE_AID, new CLDeviceHandler(deviceMap));

        // bindings
        insertHandler(CLRemoteContextBinding.AID, new CLContextHandler(cl));
        insertHandler(CLRemoteProgramBinding.AID, new CLProgramHandler(cl));
        insertHandler(CLRemoteKernelBinding.AID, new CLKernelHandler(cl));
        insertHandler(CLRemoteMemoryBinding.AID, new CLMemoryHandler(cl));
        insertHandler(CLRemoteCommandQueueBinding.AID, new CLCommandQueueHandler(cl));
        insertHandler(CLRemoteEventBinding.AID, new CLEventHandler(cl));
        insertHandler(CLRemoteSamplerBinding.AID, new CLSamplerHandler(cl));
    }

    private static class CLStaticPlatformHandler extends CLHandler {

        final CLPlatform[] platforms;

        public CLStaticPlatformHandler(CLPlatform[] platforms) {
            this.platforms = platforms;
        }

        @Override
        public void handle(ByteChannel channel, int methodID) throws IOException {
            ByteBuffer buffer = null;
            switch (methodID) {
                case RemoteNode.PLATFORM_IDS:
                    buffer = newDirectByteBuffer(SIZEOF_INT + platforms.length * SIZEOF_LONG);
                    buffer.putInt(platforms.length);
                    for (CLPlatform platform : platforms) {
                        buffer.putLong(platform.ID);
                    }
                    break;
                default:
                    throw new RuntimeException("unexpected methodID " + methodID);
            }
            if (buffer != null) {
                buffer.rewind();
                channel.write(buffer);
            }
        }
    }

    private static class CLPlatformHandler extends CLHandler {

        final Map<Long, CLPlatform> platformMap;

        public CLPlatformHandler(Map<Long, CLPlatform> platformMap) {
            this.platformMap = platformMap;
        }

        @Override
        public void handle(ByteChannel channel, int methodID) throws IOException {

            ByteBuffer buffer = newDirectByteBuffer(SIZEOF_LONG);

            long platformID = readLong(channel, buffer);

            CLPlatform platform = platformMap.get(platformID);
            if (platform == null) {
                throw new RuntimeException("unknown platform id: " + platformID);
            }

            switch (methodID) {
                case CLRemoteInfoAccessor.MID_STRING:

                    int infoKey = readInt(channel, buffer);

                    // todo optimize
                    String string = platform.getInfoString(infoKey);
                    byte[] bytes = string.getBytes();

                    buffer = newDirectByteBuffer(SIZEOF_INT + bytes.length);
                    buffer.putInt(bytes.length).put(bytes).rewind();
                    channel.write(buffer);

                    break;

                case CLRemotePlatformInfoAccessor.MID_DEVICES:

                    long typeFlags = readLong(channel, buffer);

                    // todo optimize
                    CLDevice[] devices = platform.listCLDevices(Type.valueOf(typeFlags));
                    buffer = newDirectByteBuffer(SIZEOF_INT + devices.length * SIZEOF_LONG);
                    buffer.putInt(devices.length);
                    for (CLDevice device : devices) {
                        buffer.putLong(device.ID);
                    }
                    buffer.rewind();
                    channel.write(buffer);

                    break;

                default:
                    throw new RuntimeException("unexpected methodID " + methodID);
            }

        }
    }

    private static class CLDeviceHandler extends CLHandler {

        private final Map<Long, CLDevice> deviceMap;

        public CLDeviceHandler(Map<Long, CLDevice> deviceMap) {
            this.deviceMap = deviceMap;
        }

        @Override
        public void handle(ByteChannel channel, int methodID) throws IOException {

            ByteBuffer buffer = newDirectByteBuffer(SIZEOF_LONG);

            long deviceID = readLong(channel, buffer);

            CLDevice device = deviceMap.get(deviceID);
            if (device == null) {
                throw new RuntimeException("unknown device id: " + deviceID);
            }

            int infoKey = readInt(channel, buffer);
            CLInfoAccessor accessor = device.getCLAccessor();

            switch (methodID) {
                case CLRemoteInfoAccessor.MID_STRING:

                    // todo optimize
                    String string = accessor.getString(infoKey);
                    byte[] bytes = string.getBytes();

                    buffer = newDirectByteBuffer(SIZEOF_INT + bytes.length);
                    buffer.putInt(bytes.length).put(bytes).rewind();
                    channel.write(buffer);

                    break;

                case CLRemotePlatformInfoAccessor.MID_LONG:

                    long value = accessor.getLong(infoKey);
                    buffer.limit(SIZEOF_LONG);
                    buffer.putLong(0, value);
                    channel.write(buffer);

                    break;

                default:
                    throw new RuntimeException("unexpected methodID " + methodID);
            }

        }
    }

}