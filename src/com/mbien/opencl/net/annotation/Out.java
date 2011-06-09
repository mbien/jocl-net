/*
 * Created on Friday, May 27 2011 21:31
 */
package com.mbien.opencl.net.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method parameter for being a reference pointing to mutable data
 * which may be modified in the method call.
 * @author Michael Bien
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface Out {

}
