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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Contains various unorganized static utility methods used across Cayenne.
 */
public class Util {

    private static DefaultAdhocObjectFactory objectFactory;

    static {
        objectFactory = new DefaultAdhocObjectFactory();
    }

    /**
     * Converts URL to file. Throws {@link IllegalArgumentException} if the URL is not a
     * "file://" URL.
     */
    public static File toFile(URL url) throws IllegalArgumentException {
        // must convert spaces to %20, or URL->URI conversion may fail
        String urlString = url.toExternalForm();

        URI uri;
        try {
            uri = new URI(urlString.replace(" ", "%20"));
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL "
                    + urlString
                    + " can't be converted to URI", e);
        }
        return new File(uri);
    }

    /**
     * Reads file contents, returning it as a String, using System default line separator.
     */
    public static String stringFromFile(File file) throws IOException {
        return stringFromFile(file, System.getProperty("line.separator"));
    }

    /**
     * Reads file contents, returning it as a String, joining lines with provided
     * separator.
     */
    public static String stringFromFile(File file, String joinWith) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(file));

        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                buf.append(line).append(joinWith);
            }
        }
        finally {
            in.close();
        }
        return buf.toString();
    }

    /**
     * @param strings The list of strings to join.
     * @param separator The separator between the strings.
     * @return A single string of all the input strings separated by the separator.
     */
    public static String join(List<String> strings, String separator) {
        if (strings == null || strings.size() == 0)
            return "";

        if (separator == null)
            separator = "";

        StringBuilder builder = new StringBuilder();

        for (String string : strings) {
            if (builder.length() > 0)
                builder.append(separator);
            builder.append(string);
        }

        return builder.toString();
    }

    /**
     * Replaces all backslashes "\" with forward slashes "/". Convenience method to
     * convert path Strings to URI format.
     */
    public static String substBackslashes(String string) {
        return RegexUtil.substBackslashes(string);
    }

    /**
     * Looks up and returns the root cause of an exception. If none is found, returns
     * supplied Throwable object unchanged. If root is found, recursively "unwraps" it,
     * and returns the result to the user.
     */
    public static Throwable unwindException(Throwable th) {
        if (th instanceof SAXException) {
            SAXException sax = (SAXException) th;
            if (sax.getException() != null) {
                return unwindException(sax.getException());
            }
        }
        else if (th instanceof SQLException) {
            SQLException sql = (SQLException) th;
            if (sql.getNextException() != null) {
                return unwindException(sql.getNextException());
            }
        }
        else if (th.getCause() != null) {
            return unwindException(th.getCause());
        }

        return th;
    }

    /**
     * Compares two objects similar to "Object.equals(Object)". Unlike Object.equals(..),
     * this method doesn't throw an exception if any of the two objects is null.
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {

        if (o1 == null) {
            return o2 == null;
        }

        // Arrays must be handled differently since equals() only does
        // an "==" for an array and ignores equivalence. If an array, use
        // the Jakarta Commons Language component EqualsBuilder to determine
        // the types contained in the array and do individual comparisons.
        if (o1.getClass().isArray()) {
            EqualsBuilder builder = new EqualsBuilder();
            builder.append(o1, o2);
            return builder.isEquals();
        }
        else { // It is NOT an array, so use regular equals()
            return o1.equals(o2);
        }
    }

    /**
     * Compares two objects similar to "Comparable.compareTo(Object)". Unlike
     * Comparable.compareTo(..), this method doesn't throw an exception if any of the two
     * objects is null.
     *
     * @since 1.1
     */
    public static <T> int nullSafeCompare(boolean nullsFirst, Comparable<T> o1, T o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        else if (o1 == null) {
            return nullsFirst ? -1 : 1;
        }
        else if (o2 == null) {
            return nullsFirst ? 1 : -1;
        }
        else {
            return o1.compareTo(o2);
        }
    }

    /**
     * Returns true, if the String is null or an empty string.
     */
    public static boolean isEmptyString(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Creates Serializable object copy using serialization/deserialization.
     */
    public static <T extends Serializable> T cloneViaSerialization(T object)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream() {

            @Override
            public byte[] toByteArray() {
                return buf;
            }
        };

        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(object);
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes
                .toByteArray()));
        T copy = (T) in.readObject();

        // no need to close the stream - we created it and now will be throwing away...
        // in.close();

        return copy;
    }

    /**
     * Creates an XMLReader with default feature set. Note that all Cayenne internal XML
     * parsers should probably use XMLReader obtained via this method for consistency
     * sake, and can customize feature sets as needed.
     */
    public static XMLReader createXmlReader() throws SAXException,
            ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        // Create a JAXP SAXParser
        SAXParser saxParser = spf.newSAXParser();

        // Get the encapsulated SAX XMLReader
        XMLReader reader = saxParser.getXMLReader();

        // set default features
        reader.setFeature("http://xml.org/sax/features/namespaces", true);

        return reader;
    }

    /**
     * Returns package name for the Java class as a path separated with forward slash
     * ("/"). Method is used to lookup resources that are located in package
     * subdirectories. For example, a String "a/b/c" will be returned for class name
     * "a.b.c.ClassName".
     */
    public static String getPackagePath(String className) {
        return RegexUtil.getPackagePath(className);
    }

    /**
     * Returns an unqualified class name for the fully qualified name.
     *
     * @since 3.0
     */
    public static String stripPackageName(String className) {
        if (className == null || className.length() == 0)
            return className;

        int lastDot = className.lastIndexOf('.');

        if ((-1 == lastDot) || ((className.length() - 1) == lastDot))
            return className;

        return className.substring(lastDot + 1);
    }

    /**
     * Creates a mutable map out of two arrays with keys and values.
     *
     * @since 1.2
     */
    public static <K, V> Map<K, V> toMap(K[] keys, V[] values) {
        int keysSize = (keys != null) ? keys.length : 0;
        int valuesSize = (values != null) ? values.length : 0;

        if (keysSize == 0 && valuesSize == 0) {
            // return mutable map
            return new HashMap<K, V>();
        }

        if (keysSize != valuesSize) {
            throw new IllegalArgumentException(
                    "The number of keys doesn't match the number of values.");
        }

        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < keysSize; i++) {
            map.put(keys[i], values[i]);
        }

        return map;
    }

    /**
     * Extracts extension from the file name. Dot is not included in the returned string.
     */
    public static String extractFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0 && dotInd < fileName.length()) ? fileName
                .substring(dotInd + 1) : null;
    }

    /**
     * Strips extension from the file name.
     */
    public static String stripFileExtension(String fileName) {
        int dotInd = fileName.lastIndexOf('.');

        // if dot is in the first position,
        // we are dealing with a hidden file rather than an extension
        return (dotInd > 0) ? fileName.substring(0, dotInd) : fileName;
    }

    /**
     * Strips "\n", "\r\n", "\r" from the argument string, replacing them with a provided
     * character.
     *
     * @since 3.1
     */
    public static String stripLineBreaks(String string, char replaceWith) {

        if (string == null) {
            return null;
        }

        int len = string.length();
        char[] buffer = new char[len];
        boolean matched = false;

        int j = 0;
        for (int i = 0; i < len; i++, j++) {
            char c = string.charAt(i);

            // skip \n, \r, \r\n
            if (c == '\n' || c == '\r') {

                matched = true;

                // do lookahead
                if (i + 1 < len && string.charAt(i + 1) == '\n') {
                    i++;
                }

                buffer[j] = replaceWith;
            }
            else {
                buffer[j] = c;
            }
        }

        return matched ? new String(buffer, 0, j) : string;
    }

    /**
     * Encodes a string so that it can be used as an attribute value in an XML document.
     * Will do conversion of the greater/less signs, quotes and ampersands.
     */
    public static String encodeXmlAttribute(String string) {
        if (string == null) {
            return null;
        }

        int len = string.length();
        if (len == 0) {
            return string;
        }

        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (c == '<')
                encoded.append("&lt;");
            else if (c == '\"')
                encoded.append("&quot;");
            else if (c == '>')
                encoded.append("&gt;");
            else if (c == '\'')
                encoded.append("&apos;");
            else if (c == '&')
                encoded.append("&amp;");
            else
                encoded.append(c);
        }

        return encoded.toString();
    }

    /**
     * Trims long strings substituting middle part with "...".
     *
     * @param str String to trim.
     * @param maxLength maximum allowable length. Must be at least 5, or an
     *            IllegalArgumentException is thrown.
     * @return String
     */
    public static String prettyTrim(String str, int maxLength) {
        if (maxLength < 5) {
            throw new IllegalArgumentException(
                    "Algorithm for 'prettyTrim' works only with length >= 5. "
                            + "Supplied length is "
                            + maxLength);
        }

        if (str == null || str.length() <= maxLength) {
            return str;
        }

        // find a section to cut off
        int len = maxLength - 3;
        int startLen = len / 2;
        int endLen = len - startLen;

        return str.substring(0, startLen) + "..." + str.substring(str.length() - endLen);
    }

    /**
     * Returns a sorted iterator from an unsorted one. Use this method as a last resort,
     * since it is much less efficient then just sorting a collection that backs the
     * original iterator.
     */
    public static <T> Iterator<T> sortedIterator(Iterator<T> it, Comparator<T> comparator) {
        List<T> list = new ArrayList<T>();
        while (it.hasNext()) {
            list.add(it.next());
        }

        Collections.sort(list, comparator);
        return list.iterator();
    }

    /**
     * Builds a hashCode of Collection.
     */
    public static int hashCode(Collection<?> c) {
        HashCodeBuilder builder = new HashCodeBuilder();
        for (Object o : c) {
            builder.append(o);
        }
        return builder.toHashCode();
    }

    /**
     * @since 1.2
     */
    public static Pattern sqlPatternToPattern(String pattern, boolean ignoreCase) {
        String preprocessed = RegexUtil.sqlPatternToRegex(pattern);

        int flag = (ignoreCase) ? Pattern.CASE_INSENSITIVE : 0;
        return Pattern.compile(preprocessed, flag);
    }

    /**
     * Returns true if a Member is accessible via reflection under normal Java access
     * controls.
     *
     * @since 1.2
     */
    public static boolean isAccessible(Member member) {
        return Modifier.isPublic(member.getModifiers())
                && Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    /**
     * Creates a Java class, handling regular class names as well as
     * single-dimensional arrays and primitive types.
     * 
     * @since 1.2
     * @deprecated since 3.2 this method based on statically defined class
     *             loading algorithm is not going to work in environments like
     *             OSGi. {@link AdhocObjectFactory} should be used as it can
     *             provide the environment-specific class loading policy.
     */
    @Deprecated
    public static Class<?> getJavaClass(String className) throws ClassNotFoundException {
        return objectFactory.getJavaClass(className);
    }

    static void setReverse(
            final Persistent sourceObject,
            String propertyName,
            final Persistent targetObject) {

        ArcProperty property = (ArcProperty) Cayenne
                .getClassDescriptor(sourceObject)
                .getProperty(propertyName);
        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitToMany(ToManyProperty property) {
                    property.addTargetDirectly(targetObject, sourceObject);
                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    property.setTarget(targetObject, sourceObject, false);
                    return false;
                }

                public boolean visitAttribute(AttributeProperty property) {
                    return false;
                }

            });

            sourceObject.getObjectContext().getGraphManager().arcCreated(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }

    static void unsetReverse(
            final Persistent sourceObject,
            String propertyName,
            final Persistent targetObject) {

        ArcProperty property = (ArcProperty) Cayenne
                .getClassDescriptor(sourceObject)
                .getProperty(propertyName);
        ArcProperty reverseArc = property.getComplimentaryReverseArc();
        if (reverseArc != null) {
            reverseArc.visit(new PropertyVisitor() {

                public boolean visitToMany(ToManyProperty property) {
                    property.removeTargetDirectly(targetObject, sourceObject);
                    return false;
                }

                public boolean visitToOne(ToOneProperty property) {
                    property.setTarget(targetObject, null, false);
                    return false;
                }

                public boolean visitAttribute(AttributeProperty property) {
                    return false;
                }

            });

            sourceObject.getObjectContext().getGraphManager().arcDeleted(
                    targetObject.getObjectId(),
                    sourceObject.getObjectId(),
                    reverseArc.getName());

            markAsDirty(targetObject);
        }
    }

    /**
     * Changes object state to MODIFIED if needed, returning true if the change has
     * occurred, false if not.
     */
    static boolean markAsDirty(Persistent object) {
        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
            object.setPersistenceState(PersistenceState.MODIFIED);
            return true;
        }

        return false;
    }
}
