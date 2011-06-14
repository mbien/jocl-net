/*
 * Created on Monday, June 13 2011 20:52
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLEventBinding;
import com.jogamp.opencl.llb.impl.CLEventCallback;
import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.annotation.Unsupported;
import java.nio.Buffer;
import java.nio.IntBuffer;


import static com.mbien.opencl.net.annotation.Unsupported.Kind.*;

/**
 *
 * @author Michael Bien
 */
public interface RemoteEventBinding extends CLEventBinding {

    @Override
    public long clCreateUserEvent(long context, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateUserEvent(long context, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public int clGetEventInfo(long event, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clGetEventProfilingInfo(long event, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clReleaseEvent(long event);

    @Override
    public int clRetainEvent(long event);

    @Override
    public int clSetUserEventStatus(long event, int execution_status);

    @Override
    public int clWaitForEvents(int num_events, NativeSizeBuffer event_list);

    @Override
    @Unsupported(UOE)
    public int clSetEventCallback(long event, int type, CLEventCallback cb);
    
}
