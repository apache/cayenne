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

import org.apache.cayenne.project.validation.NameValidationHelper;
import org.apache.cayenne.util.Util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Methods for mangling strings.
 */
public class StringUtils {

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
        if (NameValidationHelper.getInstance().isReservedJavaKeyword(variableName)) {
            return "_" + variableName;
        }
        else {
            return variableName;
        }
    }

    /**
     * Removes package name, leaving base name.
     * 
     * @since 1.2
     */
    public String stripPackageName(String fullyQualifiedClassName) {
        return Util.stripPackageName(fullyQualifiedClassName);
    }

    /**
     * Removes base name, leaving package name.
     * 
     * @since 1.2
     */
    public String stripClass(String aString) {
        if (aString == null || aString.length() == 0) {
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
        return Util.capitalized(name);
    }

    /**
     * Returns string with lowercased first letter
     * 
     * @since 1.2
     */
    public String uncapitalized(String aString) {
        return Util.uncapitalized(aString);
    }

    /**
     * Converts property name to Java constants naming convention.
     * 
     * @since 1.1
     */
    public String capitalizedAsConstant(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }

        // clear of non-java chars. While the method name implies that a passed identifier
        // is pure Java, it is used to build pk columns names and such, so extra safety
        // check is a good idea
        name = Util.specialCharsToJava(name);

        char charArray[] = name.toCharArray();
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
        if (str == null || str.length() == 0) {
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
     * Converts string to camel case string
     * @param aString
     * @param upOrDown
     * @return camel cased version
     */
    public String camelCase(String aString, boolean upOrDown) {
        if (aString == null || aString.length() == 0) {
            return aString;
        }
        return Util.underscoredToJava(aString, upOrDown);
    }

    /**
     * Converts string to lower case
     * @param aString
     * @return
     */
    public String toLowerCase(String aString) {
        if (aString == null || aString.length() == 0) {
            return aString;
        }
        return aString.toLowerCase();
    }


    public String dateAndTimeNow() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd--HH:mm");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(  calendar.getTime());


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
