/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mbien.generator;

import java.io.PrintWriter;

/**
 *
 * @author mbien
 */
public class IndentingWriter extends PrintWriter {

    private String tabs = "";

    public IndentingWriter(PrintWriter out) {
        super(out);
    }

    @Override
    public void println() {
        super.println();
        super.print(tabs);
    }
    
    public void indent() {
        tabs += "    ";
    }

    public void unindent() {
        if(tabs.length() >= 4) {
            tabs = tabs.substring(4);
        }
    }


}
