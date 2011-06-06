/*
 * Created on Thursday, June 02 2011 19:01
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLProgramBinding;
import com.jogamp.opencl.llb.impl.BuildProgramCallback;
import com.mbien.opencl.net.annotation.Out;
import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface RemoteProgramBinding extends CLProgramBinding {

    @Override
    public long clCreateProgramWithBinary(long context, int num_devices, NativeSizeBuffer device_list, NativeSizeBuffer lengths, NativeSizeBuffer binaries, @Out IntBuffer binary_status, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateProgramWithSource(long context, int count, String[] strings, NativeSizeBuffer lengths, @Out IntBuffer errcode_ret);

    @Override
    public int clGetProgramBuildInfo(long program, long device, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clGetProgramInfo(long program, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clReleaseProgram(long program);

    @Override
    public int clRetainProgram(long program);

    @Override
    public int clBuildProgram(long program, int deviceCount, NativeSizeBuffer devices, String options, BuildProgramCallback cb);

}
