/*
 * Created on Tuesday, June 14 2011
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
@Target(value = {ElementType.METHOD, ElementType.PARAMETER})
public @interface Unsupported {

    public enum Kind {
        
        /**
         * Operation should do nothing.
         */
        NOOP,

        /**
         * Operation should throw a UnsupportedOperationException.
         */
        UOE
    }

    Kind value();

}
