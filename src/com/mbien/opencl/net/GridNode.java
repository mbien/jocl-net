/*
 * Created on Monday, May 16 2011 17:26
 */
package com.mbien.opencl.net;

import java.net.InetAddress;

/**
 * Superclass for all nodes of a grid.
 * @author Michael Bien
 */
public abstract class GridNode {

    public final String group;
    public final String name;
    
    public final InetAddress address;

    public GridNode(String group, String name, InetAddress address) {
        this.group = group;
        this.name = name;
        this.address = address;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+group+", "+name+", "+address.getHostAddress()+"]";
    }



}
