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

package org.apache.cayenne.gen;

import org.apache.cayenne.project.validation.NameValidationHelper;
import org.apache.cayenne.util.NameConverter;
import org.apache.cayenne.util.Util;

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
        if (aString == null || aString.length() == 0)
            return aString;

        int lastDot = aString.lastIndexOf('.');

        if (-1 == lastDot)
            return "";

        return aString.substring(0, lastDot);
    }

    /**
     * Capitalizes the first letter of the property name.
     * 
     * @since 1.1
     */
    public String capitalized(String name) {
        if (name == null || name.length() == 0)
            return name;

        char c = Character.toUpperCase(name.charAt(0));
        return (name.length() == 1) ? Character.toString(c) : c + name.substring(1);
    }

    /**
     * Returns string with lowercased first letter
     * 
     * @since 1.2
     */
    public static String uncapitalized(String aString) {
        if (aString == null || aString.length() == 0)
            return aString;

        char c = Character.toLowerCase(aString.charAt(0));
        return (aString.length() == 1) ? Character.toString(c) : c + aString.substring(1);
    }

    /**
     * Converts property name to Java constants naming convention.
     * 
     * @since 1.1
     */
    public String capitalizedAsConstant(String name) {
        if (name == null || name.length() == 0)
            return name;

        return NameConverter.javaToUnderscored(name);
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
}
