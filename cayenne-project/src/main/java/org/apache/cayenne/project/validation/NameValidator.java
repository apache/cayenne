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

package org.apache.cayenne.project.validation;

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Defines a set of rules for validating java and db mapping identifiers.
 *
 * @since 5.0
 */
public class NameValidator {

    private static final Collection<String> RESERVED_JAVA_KEYWORDS = List.of(
            "abstract",
            "assert",
            "default",
            "enum",
            "if",
            "private",
            "this",
            "boolean",
            "do",
            "implements",
            "protected",
            "throw",
            "break",
            "double",
            "import",
            "public",
            "throws",
            "byte",
            "else",
            "instanceof",
            "return",
            "transient",
            "case",
            "extends",
            "int",
            "short",
            "try",
            "catch",
            "final",
            "interface",
            "static",
            "void",
            "char",
            "finally",
            "long",
            "strictfp",
            "volatile",
            "class",
            "float",
            "native",
            "super",
            "while",
            "const",
            "for",
            "new",
            "switch",
            "continue",
            "goto",
            "package",
            "synchronized");

    // property getter or setter would conflict with Object or Persistent
    private static final Collection<String> PERSISTENT_BASE_PROPERTIES = List.of(
            "class",
            "objectId",
            "objectContext",
            "persistenceState",
            "snapshotVersion");

    private NameValidator() {
    }

    public static boolean isReservedJavaKeyword(String word) {
        return RESERVED_JAVA_KEYWORDS.contains(word);
    }

    /**
     * This is more of a sanity check than a real validation. As different DBs allow
     * different chars in identifiers, here we simply check for dots.
     */
    public static String invalidCharsInDbPathComponent(String dbPathComponent) {
        return (dbPathComponent.indexOf('.') >= 0) ? "." : null;
    }

    /**
     * Scans a name of ObjAttribute or ObjRelationship for invalid characters.
     */
    public static String invalidCharsInObjPathComponent(String objPathComponent) {
        String invalidChars = validateJavaIdentifier(objPathComponent, "");
        return (!invalidChars.isEmpty()) ? invalidChars : null;
    }

    public static String invalidCharsInJavaClassName(String javaClassName) {
        if (javaClassName == null) {
            return null;
        }

        String invalidChars = "";

        StringTokenizer toks = new StringTokenizer(javaClassName, ".");
        while (toks.hasMoreTokens()) {
            invalidChars = validateJavaIdentifier(toks.nextToken(), invalidChars);
        }

        return !invalidChars.isEmpty() ? invalidChars : null;
    }

    /**
     * Returns whether a "."-separated class or package name contains invalid components: reserved Java
     * keywords or empty segments ("com.default.Foo", "com..Foo", ".Foo", "Foo.").
     */
    public static boolean invalidJavaClassComponents(String classFQN) {
        if (classFQN == null) {
            return true;
        }

        // the -1 limit keeps trailing empty segments that StringTokenizer would silently skip
        for (String component : classFQN.split("\\.", -1)) {
            if (component.isEmpty() || RESERVED_JAVA_KEYWORDS.contains(component)) {
                return true;
            }
        }

        return false;
    }

    private static String validateJavaIdentifier(String id, String invalidChars) {
        // TODO: Java spec seems to allow "$" char in identifiers...
        // Cayenne expressions do not, so we should probably check for this char presence...

        int len = (id != null) ? id.length() : 0;
        if (len == 0) {
            return invalidChars;
        }

        if (!Character.isJavaIdentifierStart(id.charAt(0))) {
            if (invalidChars.indexOf(id.charAt(0)) < 0) {
                invalidChars = invalidChars + id.charAt(0);
            }
        }

        StringBuilder buf = new StringBuilder(invalidChars);
        for (int i = 1; i < len; i++) {

            if (!Character.isJavaIdentifierPart(id.charAt(i))) {
                if (buf.toString().indexOf(id.charAt(i)) < 0) {
                    buf.append(id.charAt(i));
                }
            }
        }
        invalidChars = buf.toString();

        return invalidChars;
    }

    /**
     * Returns whether a given String is a valid Persistent property. A property is
     * considered invalid if there is a getter or a setter for it in java.lang.Object or
     * PersistentObject.
     */
    public static boolean invalidPersistentProperty(String persistentObjectProperty) {
        return persistentObjectProperty == null
                || PERSISTENT_BASE_PROPERTIES.contains(persistentObjectProperty);
    }
}
