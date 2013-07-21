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
import java.util.Map;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.1
 */
public class XMLEncoder {

    protected String projectVersion;
    protected String indent;
    protected PrintWriter out;

    protected boolean indentLine;
    protected int indentTimes;

    public XMLEncoder(PrintWriter out) {
        this(out, null, null);
    }

    public XMLEncoder(PrintWriter out, String indent) {
        this(out, indent, null);
    }

    /**
     * @since 3.1
     */
    public XMLEncoder(PrintWriter out, String indent, String projectVersion) {
        this.indent = indent;
        this.out = out;
        this.projectVersion = projectVersion;
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
     * Utility method that prints all map values, assuming they are XMLSerializable
     * objects.
     */
    public void print(Map<?, ? extends XMLSerializable> map) {
        for (XMLSerializable value : map.values()) {
            value.encodeAsXML(this);
        }
    }

    /**
     * Utility method that prints all map values, assuming they are XMLSerializable
     * objects.
     */
    public void print(Collection<? extends XMLSerializable> c) {
        for (XMLSerializable value : c) {
            value.encodeAsXML(this);
        }
    }

    /**
     * Inserts an optional project version attribute in the output. If the project version
     * is not initialized for encoder, will do nothing.
     * 
     * @since 3.1
     */
    public void printProjectVersion() {
        printAttribute("project-version", projectVersion);
    }

    /**
     * Prints an XML attribute. The value is trimmed (so leading and following spaces are
     * lost) and then encoded to be a proper XML attribute value. E.g. "&" becomes
     * "&amp;", etc.
     * 
     * @since 3.1
     */
    public void printAttribute(String name, String value) {
        printAttribute(name, value, false);
    }

    /**
     * @since 3.1
     */
    public void printlnAttribute(String name, String value) {
        printAttribute(name, value, true);
    }

    private void printAttribute(String name, String value, boolean lineBreak) {
        if (value == null) {
            return;
        }

        value = value.trim();
        if (value.length() == 0) {
            return;
        }

        value = Util.encodeXmlAttribute(value);

        printIndent();
        out.print(' ');
        out.print(name);
        out.print("=\"");
        out.print(value);
        out.print("\"");
        if (lineBreak) {
            println();
        }
    }

    /**
     * Prints a common XML element - property with name and value.
     */
    public void printProperty(String name, String value) {
        printIndent();
        out.print("<property");
        printAttribute("name", name);
        printAttribute("value", value);
        out.println("/>");
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

    /**
     * @since 3.1
     */
    public void println() {
        out.println();
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
