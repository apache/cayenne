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

package org.apache.cayenne.project.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Defines a set of rules for validating java and db mapping identifiers.
 * 
 * @since 1.1
 */
public class NameValidationHelper {

    static final Collection<String> RESERVED_JAVA_KEYWORDS = Arrays.asList(
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

    public boolean isReservedJavaKeyword(String word) {
        return RESERVED_JAVA_KEYWORDS.contains(word);
    }

    // a property is considered invalid if there is a getter or a setter for it in
    // java.lang.Object or CayenneDataObject
    static final Collection<String> INVALID_JAVA_PROPERTIES = Arrays.asList(
            "class",
            "committedSnapshot",
            "currentSnapshot",
            "dataContext",
            "objectId",
            "persistenceState",
            "snapshotVersion");

    static final NameValidationHelper sharedInstance = new NameValidationHelper();

    /**
     * Returns shared instance of the validator.
     */
    public static NameValidationHelper getInstance() {
        return sharedInstance;
    }

    /**
     * This is more of a sanity check than a real validation. As different DBs allow
     * different chars in identifiers, here we simply check for dots.
     */
    public String invalidCharsInDbPathComponent(String dbPathComponent) {
        return (dbPathComponent.indexOf('.') >= 0) ? "." : null;
    }

    /**
     * Scans a name of ObjAttribute or ObjRelationship for invalid characters.
     */
    public String invalidCharsInObjPathComponent(String objPathComponent) {
        String invalidChars = validateJavaIdentifier(objPathComponent, "");
        return (invalidChars.length() > 0) ? invalidChars : null;
    }

    public String invalidCharsInJavaClassName(String javaClassName) {
        if (javaClassName == null) {
            return null;
        }

        String invalidChars = "";

        StringTokenizer toks = new StringTokenizer(javaClassName, ".");
        while (toks.hasMoreTokens()) {
            invalidChars = validateJavaIdentifier(toks.nextToken(), invalidChars);
        }

        return (invalidChars.length() > 0) ? invalidChars : null;
    }

    public boolean invalidDataObjectClass(String dataObjectClassFQN) {
        if (dataObjectClassFQN == null) {
            return true;
        }

        StringTokenizer toks = new StringTokenizer(dataObjectClassFQN, ".");
        while (toks.hasMoreTokens()) {
            if (RESERVED_JAVA_KEYWORDS.contains(toks.nextToken())) {
                return true;
            }
        }

        return false;
    }

    private String validateJavaIdentifier(String id, String invalidChars) {
        // TODO: Java spec seems to allow "$" char in identifiers... Cayenne expressions
        // do
        // not, so we should probably check for this char presence...

        int len = (id != null) ? id.length() : 0;
        if (len == 0) {
            return invalidChars;
        }

        if (!Character.isJavaIdentifierStart(id.charAt(0))) {
            if (invalidChars.indexOf(id.charAt(0)) < 0) {
                invalidChars = invalidChars + id.charAt(0);
            }
        }

        for (int i = 1; i < len; i++) {

            if (!Character.isJavaIdentifierPart(id.charAt(i))) {
                if (invalidChars.indexOf(id.charAt(i)) < 0) {
                    invalidChars = invalidChars + id.charAt(i);
                }
            }
        }

        return invalidChars;
    }

    /**
     * Returns whether a given String is a valid DataObject property. A property is
     * considered invalid if there is a getter or a setter for it in java.lang.Object or
     * CayenneDataObject.
     */
    public boolean invalidDataObjectProperty(String dataObjectProperty) {
        return dataObjectProperty == null
                || INVALID_JAVA_PROPERTIES.contains(dataObjectProperty);
    }
}
