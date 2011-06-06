package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.annotation.Out;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface Target {

    public long test1(
        long longVal, int intVal,
        NativeSizeBuffer nativeSize,
        IntBuffer intIn, @Out IntBuffer intOut
    );
    
    public void test2();
    
//    public long test3(int a, String foo, int b);

}
