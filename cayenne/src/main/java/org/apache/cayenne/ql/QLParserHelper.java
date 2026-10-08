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

package org.apache.cayenne.ql;

import org.apache.cayenne.exp.EnumRef;
import org.apache.cayenne.exp.ListExp;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.path.CayennePath;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Node-building helpers for {@link QLParser}.
 *
 * @since 4.2
 */
class QLParserHelper {

    /**
     * Sets the path of a node from its String form, extracting the "#alias" markers into the node's path aliases.
     */
    static void parsePath(PathExp pathExp, String path) throws ParseException {
        if(path == null || !path.contains("#")) {
            pathExp.setOperand(0, path);
            return;
        }

        String[] pathSegments = path.split("\\.");
        Map<String, String> aliasMap = new HashMap<>(pathSegments.length);
        for (int i = 0; i < pathSegments.length; i++) {
            if (pathSegments[i].contains("#")) {
                String[] splitSegment = pathSegments[i].split("#");
                if(splitSegment[1].charAt(splitSegment[1].length() - 1) == CayennePath.OUTER_JOIN_INDICATOR) {
                    splitSegment[0] += CayennePath.OUTER_JOIN_INDICATOR;
                    splitSegment[1] = splitSegment[1].substring(0, splitSegment[1].length() - 1);
                }
                String previousAlias = aliasMap.putIfAbsent(splitSegment[1], splitSegment[0]);
                if (previousAlias != null && !previousAlias.equals(splitSegment[0])) {
                    throw new ParseException("Can't add the same alias to different path segments.");
                }
                pathSegments[i] = splitSegment[1];
            }
        }
        pathExp.setOperand(0, String.join(".", pathSegments));
        pathExp.setPathAliases(aliasMap);
    }

    /**
     * Returns the operand that takes the place of a named parameter in the expression tree: a collection as a list
     * node, anything else as is.
     */
    static Object parameterValue(Object value) {
        return switch (value) {
            case Collection<?> collection -> new ListExp(collection);
            case Object[] array -> new ListExp(array);
            case null, default -> value;
        };
    }

    /**
     * Parses the "class.CONSTANT" path of an "enum:" literal.
     */
    static EnumRef enumRef(String enumPath) throws ParseException {
        int dot = enumPath.lastIndexOf('.');
        if (dot <= 0 || dot == enumPath.length() - 1) {
            throw new ParseException("Invalid enum path: " + enumPath);
        }
        return new EnumRef(enumPath.substring(0, dot), enumPath.substring(dot + 1));
    }

}
