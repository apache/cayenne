/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class XMLEncoder {
    protected String indent;
    protected PrintWriter out;

    protected boolean indentLine;
    protected int indentTimes;

    public XMLEncoder(PrintWriter out) {
        this.out = out;
    }

    public XMLEncoder(PrintWriter out, String indent) {
        this.indent = indent;
        this.out = out;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }

    public void indent(int i) {
        indentTimes += i;
        if (indentTimes < 0) {
            indentTimes = 0;
        }
    }

    /**
     * Utility method that prints all map values,
     * assuming they are XMLSerializable objects.
     */
    public void print(Map map) {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ((XMLSerializable) entry.getValue()).encodeAsXML(this);
        }
    }

    /**
     * Utility method that prints all map values,
     * assuming they are XMLSerializable objects.
     */
    public void print(Collection c) {
        Iterator it = c.iterator();
        while (it.hasNext()) {
            ((XMLSerializable) it.next()).encodeAsXML(this);
        }
    }

    /**
     * Prints a common XML element - property with name and value.
     */
    public void printProperty(String name, String value) {
        printIndent();
        out.print("<property name=\"");
        out.print(name);
        out.print("\" value=\"");
        out.print(value);
        out.println("\"/>");
        indentLine = true;
    }

    /**
     * Prints a common XML element - property with name and value.
     */
    public void printProperty(String name, boolean b) {
        printProperty(name, String.valueOf(b));
    }

    /**
     * Prints a common XML element - property with name and value.
     */
    public void printProperty(String name, int i) {
        printProperty(name, String.valueOf(i));
    }

    public void print(String text) {
        printIndent();
        out.print(text);
    }

    public void print(char c) {
        printIndent();
        out.print(c);
    }

    public void print(boolean b) {
        printIndent();
        out.print(b);
    }

    public void print(int i) {
        printIndent();
        out.print(i);
    }

    public void println(String text) {
        printIndent();
        out.println(text);
        indentLine = true;
    }

    public void println(char c) {
        printIndent();
        out.println(c);
        indentLine = true;
    }

    private void printIndent() {
        if (indentLine) {
            indentLine = false;

            if (indentTimes > 0 && indent != null) {
                for (int i = 0; i < indentTimes; i++) {
                    out.print(indent);
                }
            }
        }
    }
}
