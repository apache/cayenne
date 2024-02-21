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

package org.apache.cayenne.exp.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.2
 */
class ExpressionUtils {
    static void parsePath(ASTPath pathExp, String path) throws ParseException {
        if(path == null || !path.contains("#")) {
            pathExp.setPath(path);
            return;
        }

        String[] pathSegments = path.split("\\.");
        Map<String, String> aliasMap = new HashMap<>(pathSegments.length);
        for (int i = 0; i < pathSegments.length; i++) {
            if (pathSegments[i].contains("#")) {
                String[] splitSegment = pathSegments[i].split("#");
                if(splitSegment[1].endsWith("+")) {
                    splitSegment[0] += '+';
                    splitSegment[1] = splitSegment[1].substring(0, splitSegment[1].length() - 1);
                }
                String previousAlias = aliasMap.putIfAbsent(splitSegment[1], splitSegment[0]);
                if (previousAlias != null && !previousAlias.equals(splitSegment[0])) {
                    throw new ParseException("Can't add the same alias to different path segments.");
                }
                pathSegments[i] = splitSegment[1];
            }
        }
        pathExp.setPath(String.join(".", pathSegments));
        pathExp.setPathAliases(aliasMap);
    }

}
