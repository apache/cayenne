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
package org.apache.cayenne.map.naming;

import org.apache.cayenne.map.DataMap;

/**
 * @since 4.0
 */
public class UniqueNameGenerator {

    public static final String DEFAULT_PATTERN = "%s%d";

    private final NameChecker nameChecker;

    private final String pattern;

    public UniqueNameGenerator(NameChecker nameChecker, String pattern) {
        this.nameChecker = nameChecker;
        this.pattern = pattern;
    }

    public static String generate(NameChecker checker) {
        return generate(checker, DEFAULT_PATTERN, null, null);
    }

    public static String generate(NameChecker checker, Object context) {
        return generate(checker, DEFAULT_PATTERN, context, null);
    }

    public static String generate(NameChecker checker, Object context, String baseName) {
        return generate(checker, DEFAULT_PATTERN, context, baseName);
    }

    public static String generate(NameChecker checker, String pattern, Object context, String baseName) {
        UniqueNameGenerator generator;
        if (checker == NameCheckers.embeddable) {
            generator = new UniqueNameGenerator(NameCheckers.embeddable, pattern) {
                @Override
                public String generate(Object namingContext, String nameBase) {
                    return ((DataMap) namingContext).getNameWithDefaultPackage(super.generate(namingContext, nameBase));
                }
            };
        } else {
            generator = new UniqueNameGenerator(checker, pattern);
        }

        return generator.generate(context, baseName);
    }

    /**
     * Creates a unique name for the new object and constructs this object.
     */
    String generate(Object namingContext) {
        return generate(namingContext, nameChecker.baseName());
    }

    String generate(Object namingContext, String nameBase) {
        return generate(pattern, namingContext, nameBase != null ? nameBase : nameChecker.baseName());
    }

    /**
     * @since 1.0.5
     */
    private String generate(String pattern, Object namingContext, String nameBase) {
        int c = 1;
        String name = nameBase;
        while (nameChecker.isNameInUse(namingContext, name)) {
            name = String.format(pattern, nameBase, c++);
        }

        return name;
    }

}
