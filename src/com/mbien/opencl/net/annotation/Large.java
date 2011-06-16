/*
 * Created on Thursday, June 16 2011 02:31
 */
package com.mbien.opencl.net.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks large buffers used as parameters.
 * @author Michael Bien
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface Large {

}
