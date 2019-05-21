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

    static void parsePath(ASTPath pathExp, Object path) throws ParseException {
        if (path != null && path.toString().contains("#")) {
            String[] pathSegments = path.toString().split("\\.");
            Map<String, String> aliasMap = new HashMap<>();
            for (int i = 0; i < pathSegments.length; i++) {
                if (pathSegments[i].contains("#")) {
                    String[] splitedSegment = pathSegments[i].split("#");
                    splitedSegment[0] += splitedSegment[1].endsWith("+") ? "+" : "";
                    splitedSegment[1] = splitedSegment[1].endsWith("+") ? splitedSegment[1].substring(0, splitedSegment[1].length() - 1) : splitedSegment[1];
                    if (aliasMap.putIfAbsent(splitedSegment[1], splitedSegment[0]) != null && !aliasMap.get(splitedSegment[1]).equals(splitedSegment[0])) {
                        throw new ParseException("Can't add the same alias to different path segments.");
                    }
                    pathSegments[i] = splitedSegment[1];
                }
            }
            pathExp.setPath(String.join(".", pathSegments));
            pathExp.setPathAliases(aliasMap);
        } else {
            pathExp.setPath(path);
        }
    }

}
