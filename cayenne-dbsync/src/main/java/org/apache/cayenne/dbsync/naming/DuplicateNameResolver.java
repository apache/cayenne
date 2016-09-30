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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DataMap;

/**
 * A generator of unique names for the various model objects.
 *
 * @since 4.0
 */
public class DuplicateNameResolver {

    private static final String DEFAULT_PATTERN = "%s%d";

    public static String resolve(NameChecker checker) {
        return resolve(checker, DEFAULT_PATTERN, null, null);
    }

    public static String resolve(NameChecker checker, Object context) {
        return resolve(checker, DEFAULT_PATTERN, context, null);
    }

    public static String resolve(NameChecker checker, Object context, String baseName) {
        return resolve(checker, DEFAULT_PATTERN, context, baseName);
    }

    public static String resolve(NameChecker nameChecker, String pattern, Object context, String baseName) {

        if (baseName == null) {
            baseName = nameChecker.baseName();
        }

        String resolved = doResolve(nameChecker, pattern, context, baseName);

        // TODO ugly hack with cast... something more OO is in order
        return (nameChecker == NameCheckers.embeddable)
                ? ((DataMap) context).getNameWithDefaultPackage(resolved) : resolved;
    }


    private static String doResolve(NameChecker nameChecker, String pattern, Object namingContext, String baseName) {
        int c = 1;
        String name = baseName;
        while (nameChecker.isNameInUse(namingContext, name)) {
            name = String.format(pattern, baseName, c++);
        }

        return name;
    }

}
