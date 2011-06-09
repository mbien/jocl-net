/*
 * Created on Friday, May 27 2011 21:31
 */
package com.mbien.opencl.net.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A parameter which has to be transfered into the method and may also be modified
 * within the method.
 * @author Michael Bien
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface InOut {

}
