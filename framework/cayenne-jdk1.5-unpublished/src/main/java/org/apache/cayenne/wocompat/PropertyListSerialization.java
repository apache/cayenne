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

package org.apache.cayenne.wocompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.wocompat.parser.Parser;

/**
 * A <b>PropertyListSerialization</b> is a utility class that reads and stores files in
 * NeXT/Apple property list format. Unlike corresponding WebObjects class,
 * <code>PropertyListSerialization</code> uses standard Java collections (lists and
 * maps) to store property lists.
 * 
 */
public class PropertyListSerialization {

    /**
     * Reads a property list file. Returns a property list object, that is normally a
     * java.util.List or a java.util.Map, but can also be a String or a Number.
     */
    public static Object propertyListFromFile(File f) throws FileNotFoundException {
        return propertyListFromFile(f, null);
    }

    /**
     * Reads a property list file. Returns a property list object, that is normally a
     * java.util.List or a java.util.Map, but can also be a String or a Number.
     */
    public static Object propertyListFromFile(File f, PlistDataStructureFactory factory)
            throws FileNotFoundException {
        if (!f.isFile()) {
            throw new FileNotFoundException("No such file: " + f);
        }

        return new Parser(f, factory).propertyList();
    }

    /**
     * Reads a property list data from InputStream. Returns a property list o bject, that
     * is normally a java.util.List or a java.util.Map, but can also be a String or a
     * Number.
     */
    public static Object propertyListFromStream(InputStream in) {
        return propertyListFromStream(in, null);
    }

    /**
     * Reads a property list data from InputStream. Returns a property list o bject, that
     * is normally a java.util.List or a java.util.Map, but can also be a String or a
     * Number.
     */
    public static Object propertyListFromStream(
            InputStream in,
            PlistDataStructureFactory factory) {
        return new Parser(in, factory).propertyList();
    }

    /**
     * Saves property list to file.
     */
    public static void propertyListToFile(File f, Object plist) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            try {
                writeObject("", out, plist);
            }
            finally {
                out.close();
            }
        }
        catch (IOException ioex) {
            throw new CayenneRuntimeException("Error saving plist.", ioex);
        }
    }

    /**
     * Saves property list to file.
     */
    public static void propertyListToStream(OutputStream os, Object plist) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
            try {
                writeObject("", out, plist);
            }
            finally {
                out.close();
            }
        }
        catch (IOException ioex) {
            throw new CayenneRuntimeException("Error saving plist.", ioex);
        }
    }

    /**
     * Internal method to recursively write a property list object.
     */
    protected static void writeObject(String offset, Writer out, Object plist)
            throws IOException {
        if (plist == null) {
            return;
        }

        if (plist instanceof Collection) {
            Collection list = (Collection) plist;

            out.write('\n');
            out.write(offset);

            if (list.size() == 0) {
                out.write("()");
                return;
            }

            out.write("(\n");

            String childOffset = offset + "   ";
            Iterator it = list.iterator();
            boolean appended = false;
            while (it.hasNext()) {
                // Java collections can contain nulls, skip them
                Object obj = it.next();
                if (obj != null) {
                    if (appended) {
                        out.write(", \n");
                    }

                    out.write(childOffset);
                    writeObject(childOffset, out, obj);
                    appended = true;
                }
            }

            out.write('\n');
            out.write(offset);
            out.write(')');
        }
        else if (plist instanceof Map) {
            Map map = (Map) plist;
            out.write('\n');
            out.write(offset);

            if (map.size() == 0) {
                out.write("{}");
                return;
            }

            out.write("{");

            String childOffset = offset + "    ";

            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                // Java collections can contain nulls, skip them
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                if (key == null) {
                    continue;
                }
                Object obj = entry.getValue();
                if (obj == null) {
                    continue;
                }
                out.write('\n');
                out.write(childOffset);
                out.write(quoteString(key.toString()));
                out.write(" = ");
                writeObject(childOffset, out, obj);
                out.write(';');
            }

            out.write('\n');
            out.write(offset);
            out.write('}');
        }
        else if (plist instanceof String) {
            out.write(quoteString(plist.toString()));
        }
        else if (plist instanceof Number) {
            out.write(plist.toString());
        }
        else {
            throw new CayenneRuntimeException(
                    "Unsupported class for property list serialization: "
                            + plist.getClass().getName());
        }
    }

    /**
     * Escapes all doublequotes and backslashes.
     */
    protected static String escapeString(String str) {
        char[] chars = str.toCharArray();
        int len = chars.length;
        StringBuilder buf = new StringBuilder(len + 3);

        for (int i = 0; i < len; i++) {
            if (chars[i] == '\"' || chars[i] == '\\') {
                buf.append('\\');
            }
            buf.append(chars[i]);
        }

        return buf.toString();
    }

    /**
     * Returns a quoted String, with all the escapes preprocessed. May return an unquoted
     * String if it contains no special characters. The rule for a non-special character
     * is the following:
     * 
     * <pre>
     *       c &gt;= 'a' &amp;&amp; c &lt;= 'z'
     *       c &gt;= 'A' &amp;&amp; c &lt;= 'Z'
     *       c &gt;= '0' &amp;&amp; c &lt;= '9'
     *       c == '_'
     *       c == '$'
     *       c == ':'
     *       c == '.'
     *       c == '/'
     * </pre>
     */
    protected static String quoteString(String str) {
        boolean shouldQuote = false;

        // scan string for special chars,
        // if we have them, string must be quoted

        String noQuoteExtras = "_$:./";
        char[] chars = str.toCharArray();
        int len = chars.length;
        if (len == 0) {
            shouldQuote = true;
        }
        for (int i = 0; !shouldQuote && i < len; i++) {
            char c = chars[i];

            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || noQuoteExtras.indexOf(c) >= 0) {
                continue;
            }

            shouldQuote = true;
        }

        str = escapeString(str);
        return (shouldQuote) ? '\"' + str + '\"' : str;
    }

    
}
