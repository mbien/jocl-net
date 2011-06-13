/*
 * Created on Friday, June 10 2011 02:13
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLCommandQueueBinding;
import com.mbien.opencl.net.annotation.InOut;
import com.mbien.opencl.net.annotation.Out;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface RemoteCommandQueueBinding extends CLCommandQueueBinding {

    @Override
    public long clCreateCommandQueue(long context, long device, long properties, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateCommandQueue(long context, long device, long properties, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public int clEnqueueCopyBuffer(long command_queue, long src_buffer, long dst_buffer, long src_offset, long dst_offset, long cb, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueCopyBufferRect(long command_queue, long src_buffer, long dst_buffer, NativeSizeBuffer src_origin, NativeSizeBuffer dst_origin, NativeSizeBuffer region, long src_row_pitch, long src_slice_pitch, long dst_row_pitch, long dst_slice_pitch, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueCopyBufferToImage(long command_queue, long src_buffer, long dst_image, long src_offset, NativeSizeBuffer arg4, NativeSizeBuffer arg5, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueCopyImage(long command_queue, long src_image, long dst_image, NativeSizeBuffer arg3, NativeSizeBuffer arg4, NativeSizeBuffer arg5, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueCopyImageToBuffer(long command_queue, long src_image, long dst_buffer, NativeSizeBuffer arg3, NativeSizeBuffer arg4, long dst_offset, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueMarker(long command_queue, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueNDRangeKernel(long command_queue, long kernel, int work_dim, NativeSizeBuffer global_work_offset, NativeSizeBuffer global_work_size, NativeSizeBuffer local_work_size, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueReadBuffer(long command_queue, long buffer, int blocking_read, long offset, long cb, @Out Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueReadBufferRect(long command_queue, long buffer, int blocking_read, NativeSizeBuffer buffer_origin, NativeSizeBuffer host_origin, NativeSizeBuffer region, long buffer_row_pitch, long buffer_slice_pitch, long host_row_pitch, long host_slice_pitch, @Out Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueReadImage(long command_queue, long image, int blocking_read, NativeSizeBuffer arg3, NativeSizeBuffer arg4, long row_pitch, long slice_pitch, @Out Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueTask(long command_queue, long kernel, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueUnmapMemObject(long command_queue, long memobj, Buffer mapped_ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueWaitForEvents(long command_queue, int num_events, @InOut NativeSizeBuffer event_list);

    @Override
    public int clEnqueueWriteBuffer(long command_queue, long buffer, int blocking_write, long offset, long cb, Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueWriteBufferRect(long command_queue, long buffer, int blocking_write, NativeSizeBuffer buffer_origin, NativeSizeBuffer host_origin, NativeSizeBuffer region, long buffer_row_pitch, long buffer_slice_pitch, long host_row_pitch, long host_slice_pitch, Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clEnqueueWriteImage(long command_queue, long image, int blocking_write, NativeSizeBuffer arg3, NativeSizeBuffer arg4, long input_row_pitch, long input_slice_pitch, Buffer ptr, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, @InOut NativeSizeBuffer event);

    @Override
    public int clFinish(long command_queue);

    @Override
    public int clFlush(long command_queue);

    @Override
    public int clEnqueueBarrier(long command_queue);

    @Override
    public int clGetCommandQueueInfo(long command_queue, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clReleaseCommandQueue(long command_queue);

    @Override
    public int clRetainCommandQueue(long command_queue);

    @Override
    public ByteBuffer clEnqueueMapBuffer(long command_queue, long buffer, int blocking_map, long map_flags, long offset, long cb, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, NativeSizeBuffer event, IntBuffer errcode_ret);

    @Override
    public ByteBuffer clEnqueueMapImage(long command_queue, long image, int blocking_map, long map_flags, NativeSizeBuffer arg4, NativeSizeBuffer arg5, NativeSizeBuffer image_row_pitch, NativeSizeBuffer image_slice_pitch, int num_events_in_wait_list, NativeSizeBuffer event_wait_list, NativeSizeBuffer event, IntBuffer errcode_ret);
}
