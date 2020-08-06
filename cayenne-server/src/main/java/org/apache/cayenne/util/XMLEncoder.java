/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;

/**
 * <p>
 * A helper class to encode objects to XML.
 * </p>
 * Usage: <pre>{@code
 *      XMLEncoder encoder = new XMLEncoder(writer);
 *      encoder
 *          .start("tag").attribute("name", "tag_name_attribute")
 *          .start("nested_tag").attribute("name", "nested_tag_name).cdata("tag text element").end()
 *          .end();
 * }</pre>
 *
 * @since 1.1
 * @since 4.1 API is greatly reworked to be more usable
 */
public class XMLEncoder {

    protected String projectVersion;
    protected String indent;
    protected PrintWriter out;

    protected boolean indentLine;
    protected int indentTimes;

    protected boolean tagOpened;
    protected boolean cdata;
    protected int currentTagLevel;
    protected int lastTagLevel;
    protected Deque<String> openTags = new LinkedList<>();

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

    public XMLEncoder indent(int i) {
        indentTimes += i;
        if (indentTimes < 0) {
            indentTimes = 0;
        }
        return this;
    }

    public XMLEncoder print(String text) {
        printIndent();
        out.print(text);
        return this;
    }

    public XMLEncoder println(String text) {
        printIndent();
        out.println(text);
        indentLine = true;
        return this;
    }

    /**
     * @since 3.1
     */
    public XMLEncoder println() {
        out.println();
        indentLine = true;
        return this;
    }

    private XMLEncoder printIndent() {
        if (indentLine) {
            indentLine = false;

            if (indentTimes > 0 && indent != null) {
                for (int i = 0; i < indentTimes; i++) {
                    out.print(indent);
                }
            }
        }
        return this;
    }

    /**
     * @since 4.1
     * @param tag to start
     * @return this
     */
    public XMLEncoder start(String tag) {
        if(tagOpened) {
            println(">").indent(1);
        }
        printIndent().print("<").print(tag);

        lastTagLevel = ++currentTagLevel;
        tagOpened = true;
        openTags.push(tag);
        return this;
    }

    /**
     * This method will track presence of nested tags and print closure accordingly
     *
     * @since 4.1
     * @return this
     */
    public XMLEncoder end() {
        tagOpened = false;
        if(lastTagLevel == currentTagLevel-- && !cdata) {
            openTags.pop();
            println("/>");
        } else {
            if(!cdata) {
                indent(-1).printIndent();
            }
            cdata = false;
            print("</").print(openTags.pop()).println(">");
        }
        return this;
    }

    /**
     * @since 4.1
     * @param name of the attribute
     * @param value of the attribute
     * @return this
     */
    public XMLEncoder attribute(String name, String value) {
        return attribute(name, value, false);
    }

    /**
     * @since 4.1
     * @param name of the attribute
     * @param value of the attribute
     * @param newLine should this attribute be printed on new line
     * @return this
     */
    public XMLEncoder attribute(String name, String value, boolean newLine) {
        if(Util.isEmptyString(value)) {
            return this;
        }

        if(newLine) {
            indent(1).println().printIndent();
        }
        print(" ").print(name).print("=\"").print(Util.encodeXmlAttribute(value)).print("\"");
        if(newLine) {
            indent(-1);
        }
        return this;
    }

    /**
     * @since 4.1
     * @param name of the attribute
     * @param value of the attribute
     * @return this
     */
    public XMLEncoder attribute(String name, boolean value) {
        if(!value) {
            return this;
        }
        return attribute(name, "true");
    }

    /**
     * @since 4.1
     * @param name of the attribute
     * @param value of the attribute
     * @return this
     */
    public XMLEncoder attribute(String name, int value) {
        if(value == 0) {
            return this;
        }
        return attribute(name, String.valueOf(value));
    }

    /**
     * @since 4.1
     * @param data char data
     * @return this
     */
    public XMLEncoder cdata(String data) {
        return cdata(data, false);
    }

    /**
     * @since 4.1
     * @param data char data
     * @param escape does this data need to be enclosed into &lt;![CDATA[ ... ]]&gt;
     * @return this
     */
    public XMLEncoder cdata(String data, boolean escape) {
        if(tagOpened) {
            print(">");
        }
        cdata = true;
        if(escape) {
            print("<![CDATA[");
        }
        print(data);
        if(escape) {
            print("]]>");
        }
        return this;
    }

    /**
     * @since 4.1
     * @param object nested object to serialize
     * @param delegate visitor
     * @return this
     */
    public XMLEncoder nested(XMLSerializable object, ConfigurationNodeVisitor delegate) {
        if(object == null) {
            return this;
        }
        object.encodeAsXML(this, delegate);
        return this;
    }

    /**
     * @since 4.1
     * @param collection of nested objects
     * @param delegate visitor
     * @return this
     */
    public XMLEncoder nested(Collection<? extends XMLSerializable> collection, ConfigurationNodeVisitor delegate) {
        if(collection == null) {
            return this;
        }
        for (XMLSerializable value : collection) {
            value.encodeAsXML(this, delegate);
        }
        return this;
    }

    /**
     * @since 4.1
     * @param map of nested objects
     * @param delegate visitor
     * @return this
     */
    public XMLEncoder nested(Map<?, ? extends XMLSerializable> map, ConfigurationNodeVisitor delegate) {
        if(map == null) {
            return this;
        }
        for (XMLSerializable value : map.values()) {
            value.encodeAsXML(this, delegate);
        }
        return this;
    }

    /**
     * Prints a common XML element - property with name and value.
     * @since 4.1
     */
    public XMLEncoder property(String name, String value) {
        if(Util.isEmptyString(value)) {
            return this;
        }
        start("property").attribute("name", name).attribute("value", value).end();
        indentLine = true;
        return this;
    }

    /**
     * Prints a common XML element - property with name and value.
     * @since 4.1
     */
    public XMLEncoder property(String name, boolean b) {
        if(!b) {
            return this;
        }
        return property(name, "true");
    }

    /**
     * Prints a common XML element - property with name and value.
     * @since 4.1
     */
    public XMLEncoder property(String name, int i) {
        if(i == 0) {
            return this;
        }
        return property(name, String.valueOf(i));
    }

    /**
     * Prints common XML element - tag with name and text value (&lt;tag>value&lt;/tag>)
     * If value is empty, nothing will be printed.
     * @since 4.1
     */
    public XMLEncoder simpleTag(String tag, String value) {
        if (!Util.isEmptyString(value)) {
            start(tag).cdata(value).end();
        }
        return this;
    }

    /**
     * Inserts an optional project version attribute in the output. If the project version
     * is not initialized for encoder, will do nothing.
     *
     * @since 4.1
     */
    public XMLEncoder projectVersion() {
        return attribute("project-version", projectVersion, true);
    }
}
