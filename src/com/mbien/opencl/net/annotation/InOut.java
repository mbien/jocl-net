/*
 * Created on Friday, May 27 2011 21:31
 */
package com.mbien.opencl.net.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Michael Bien
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface InOut {

}
