/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.generator;

import java.io.PrintWriter;

/**
 *
 * @author Michael Bien
 */
public class IndentingWriter extends PrintWriter {

    private final String TAB;
    private String tabs = "";

    public IndentingWriter(PrintWriter out) {
        this(out, 4);
    }

    public IndentingWriter(PrintWriter out, int tabsize) {
        super(out);
        StringBuilder sb = new StringBuilder(tabsize);
        for (int i = 0; i < tabsize; i++) {
            sb.append(' ');
        }
        TAB = sb.toString();
    }

    @Override
    public void println() {
        super.println();
        super.print(tabs);
    }
    
    public void indent() {
        tabs += TAB;
        super.print(TAB);
    }

    public void unindent() {
        if(tabs.length() >= TAB.length()) {
            tabs = tabs.substring(TAB.length());
        }
    }


}
