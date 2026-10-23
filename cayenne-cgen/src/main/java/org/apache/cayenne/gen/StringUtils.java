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

package org.apache.cayenne.gen;

import org.apache.cayenne.projecttools.validation.NameValidator;
import org.apache.cayenne.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Methods for mangling strings.
 */
public class StringUtils {

    // a simple or a fully qualified class name within a parameterized type name, like "java.util.List<String>"
    static final Pattern CLASS_NAME = Pattern.compile("[\\p{javaJavaIdentifierPart}.]+");

    private static StringUtils sharedInstance;

    public static StringUtils getInstance() {
        if (null == sharedInstance) {
            sharedInstance = new StringUtils();
        }

        return sharedInstance;
    }

    /**
     * Prepends underscore to variable name if necessary to remove conflict with reserved
     * keywords.
     */
    public String formatVariableName(String variableName) {
        if (NameValidator.isReservedJavaKeyword(variableName)) {
            return "_" + variableName;
        }
        else {
            return variableName;
        }
    }

    /**
     * Removes package name, leaving base name. For a parameterized type, like "java.util.List&lt;java.lang.String&gt;",
     * removes the package names of all the classes in it.
     * 
     * @since 1.2
     */
    public String stripPackageName(String fullyQualifiedClassName) {
        if (fullyQualifiedClassName == null || fullyQualifiedClassName.indexOf('<') < 0) {
            return Util.stripPackageName(fullyQualifiedClassName);
        }

        return CLASS_NAME.matcher(fullyQualifiedClassName)
                .replaceAll(m -> Matcher.quoteReplacement(Util.stripPackageName(m.group())));
    }

    /**
     * Removes base name, leaving package name.
     * 
     * @since 1.2
     */
    public String stripClass(String aString) {
        if (aString == null || aString.isEmpty()) {
            return aString;
        }

        int lastDot = aString.lastIndexOf('.');

        if (-1 == lastDot) {
            return "";
        }

        return aString.substring(0, lastDot);
    }

    /**
     * Capitalizes the first letter of the property name.
     *
     * @since 1.1
     */
    public String capitalized(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char c = Character.toUpperCase(name.charAt(0));
        return (name.length() == 1) ? Character.toString(c) : c + name.substring(1);
    }

    /**
     * Returns string with lowercased first letter
     *
     * @since 1.2
     */
    public String uncapitalized(String aString) {
        if (aString == null || aString.isEmpty()) {
            return aString;
        }

        char c = Character.toLowerCase(aString.charAt(0));
        return (aString.length() == 1) ? Character.toString(c) : c + aString.substring(1);
    }

    /**
     * Converts property name to Java constants naming convention.
     * 
     * @since 1.1
     */
    public String capitalizedAsConstant(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // clear of non-java chars. While the method name implies that a passed identifier
        // is pure Java, it is used to build pk columns names and such, so extra safety
        // check is a good idea
        name = Util.specialCharsToJava(name);

        char[] charArray = name.toCharArray();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < charArray.length; i++) {
            if ((Character.isUpperCase(charArray[i])) && (i != 0)) {

                char prevChar = charArray[i - 1];
                if ((Character.isLowerCase(prevChar))) {
                    buffer.append("_");
                }
            }

            buffer.append(Character.toUpperCase(charArray[i]));
        }

        return buffer.toString();
    }

    /**
     * Converts entity or property name to a plural form. For example:
     * <ul>
     * <li>pluralize("Word") == "Words"</li>
     * <li>pluralize("Status") == "Statuses"</li>
     * <li>pluralize("Index") == "Indexes"</li>
     * <li>pluralize("Factory") == "Factories"</li>
     * </ul>
     * <p>
     * As of 3.1 this method is not used in bundled templates, and is present here for
     * user templates convenience.
     * 
     * @since 3.1
     */
    public String pluralize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        else if (str.endsWith("s") || str.endsWith("x")) {
            return str + "es";
        }
        else if (str.endsWith("y")) {
            return str.substring(0, str.length() - 1) + "ies";
        }
        else {
            return str + "s";
        }
    }

    /**
     * <p>
     * Strip generic definition from string
     * </p>
     * <p>For example: List&gt;Integer&lt; == List</p>
     * @since 4.0
     */
    public String stripGeneric(String str) {
        if(str == null) {
            return null;
        }
        int start = str.indexOf('<');
        if(start == -1) {
            return str;
        }
        int end = str.lastIndexOf('>');
        if(end == -1) {
            return str;
        } else if(end == str.length() - 1) {
            return str.substring(0, start);
        }
        return str.substring(0, start) + str.substring(end+1);
    }

    public String replaceWildcardInStringWithString(String wildcard, String pattern, String replacement) {
        if (pattern == null || wildcard == null) {
            return pattern;
        }

        StringBuilder buffer = new StringBuilder();
        int lastPos = 0;
        int wildCardPos = pattern.indexOf(wildcard);
        while (wildCardPos != -1) {
            if (lastPos != wildCardPos) {
                buffer.append(pattern.substring(lastPos, wildCardPos));
            }
            buffer.append(replacement);
            lastPos += wildCardPos + wildcard.length();
            wildCardPos = pattern.indexOf(wildcard, lastPos);
        }

        if (lastPos < pattern.length()) {
            buffer.append(pattern.substring(lastPos));
        }

        return buffer.toString();
    }
}
