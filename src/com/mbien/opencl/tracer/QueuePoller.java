/*
 * Created on Wednesday, June 29 2011 00:22
 */
package com.mbien.opencl.tracer;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import com.mbien.opencl.net.delegate.CLCommandQueueDelegate;
import com.mbien.opencl.net.delegate.CLCommandQueueDelegate.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Michael Bien
 */
public class QueuePoller implements Runnable {

    private final CLCommandQueueDelegate delegate;

    public QueuePoller(CLCommandQueueDelegate delegate) {
        this.delegate = delegate;
    }

    public void start() {

        new Thread(this, "queue-poller").start();

    }

    @Override
    public void run() {

        Queue<Event> queue = delegate.events;
        CL cl = CLPlatform.getLowLevelCLInterface();

        NativeSizeBuffer nsb = NativeSizeBuffer.allocateDirect(1);
        Map<Long, TraceableQueue> queueMap = new HashMap<Long, TraceableQueue>();

        while(!Thread.currentThread().isInterrupted()) {
            try {

                while(!queue.isEmpty()) {

                    Event qe = queue.poll();

                    TraceableQueue tq = queueMap.get(qe.queue);

                    if(tq == null) {

                        int ret = cl.clGetCommandQueueInfo(qe.queue, CL.CL_QUEUE_CONTEXT, NativeSizeBuffer.elementSize(), nsb.getBuffer(), null);
                        CLException.checkForError(ret, "can not retrieve queue context");
                        long contextID = nsb.get(0);

                        ret = cl.clGetCommandQueueInfo(qe.queue, CL.CL_QUEUE_PROPERTIES, NativeSizeBuffer.elementSize(), nsb.getBuffer(), null);
                        CLException.checkForError(ret, "can not retrieve queue properties");
                        long properties = nsb.get(0);

                        ret = cl.clGetCommandQueueInfo(qe.queue, CL.CL_QUEUE_DEVICE, NativeSizeBuffer.elementSize(), nsb.getBuffer(), null);
                        CLException.checkForError(ret, "can not retrieve queue device");
                        long deviceID = nsb.get(0);


                        // TODO wrong platform
                        CLContext context = new CLTraceableContext(CLPlatform.getDefault(), contextID, null);
                        CLDevice device = new CLTraceableDevice(context, deviceID);

                        tq = new TraceableQueue(qe.queue, device, properties);
                        queueMap.put(qe.queue, tq);
                    }

                    CLTraceableEvent event = new CLTraceableEvent(tq.context, qe.event);
                    tq.events.add(event);

                    System.out.println(event);

                }

                Thread.sleep(1000);

            } catch (InterruptedException exit) {
                break;
            }

        }

        for (TraceableQueue tq : queueMap.values()) {
            for (CLEvent event : tq.events) {
                if(!event.isReleased()) {
                    event.release();
                }
            }
            tq.context.release();
        }
        
    }

    private static class CLTraceableContext extends CLContext {

        public CLTraceableContext(CLPlatform platform, long contextID, ErrorDispatcher dispatcher) {
            super(platform, contextID, dispatcher);
            CLPlatform.getLowLevelCLInterface().clRetainContext(contextID);
        }

    }

    private static class CLTraceableEvent extends CLEvent {

        public CLTraceableEvent(CLContext context, long id) {
            super(context, id);
            context.getCL().clRetainEvent(id);
        }

    }

    private static class CLTraceableDevice extends CLDevice {

        public CLTraceableDevice(CLContext context, long id) {
            super(context, id);
        }

    }

    private static class TraceableQueue {

        private long ID;
        private final CLContext context;
        private final boolean profileable;
        private final CLDevice device;
        private final List<CLEvent> events;

        public TraceableQueue(long id, CLDevice device, long properties) {
            this.ID = id;
            this.context = device.getContext();
            this.device = device;
            this.profileable = CLCommandQueue.Mode.valuesOf(properties).contains(CLCommandQueue.Mode.PROFILING_MODE);
            this.events = new ArrayList<CLEvent>(1024);
        }

        public boolean isProfileable() {
            return profileable;
        }

        public long getID() {
            return ID;
        }

        public CLContext getContext() {
            return context;
        }

        public CLDevice getDevice() {
            return device;
        }

    }

}
