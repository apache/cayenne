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

package org.apache.cayenne.exp.property;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;

/**
 * @since 4.2
 */
class PropertyUtils {

    static ASTPath createPathExp(CayennePath path, String alias, Map<String, String> aliasMap) {
        CayennePath aliased = path.parent().dot(alias);
        CayennePathSegment segment = path.last();

        Map<String, String> pathAliases = new HashMap<>(aliasMap);
        pathAliases.put(alias, segment.value());
        return buildExp(aliased, pathAliases);
    }

    static ASTPath buildExp(String path, Map<String, String> pathAliases) {
        ASTPath pathExp = new ASTObjPath(path);
        pathExp.setPathAliases(pathAliases);
        return pathExp;
    }

    static ASTPath buildExp(CayennePath path, Map<String, String> pathAliases) {
        ASTPath pathExp = new ASTObjPath(path);
        pathExp.setPathAliases(pathAliases);
        return pathExp;
    }
}
