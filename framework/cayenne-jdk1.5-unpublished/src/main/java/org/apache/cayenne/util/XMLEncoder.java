/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.util;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.1
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
