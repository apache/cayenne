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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTPath;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.2
 */
class PropertyUtils {

    static ASTPath createPathExp(String aliasedPath, String segment, String alias, Map<String, String> aliasMap) {
        ASTPath pathExp = new ASTObjPath(aliasedPath);
        Map<String, String> aliases = new HashMap<>(aliasMap);
        aliases.put(alias, segment);
        pathExp.setPathAliases(aliases);

        return pathExp;
    }

    static ASTPath createExpressionWithCopiedAliases(String name, Expression expression) {
        if(expression instanceof ASTPath) {
            ASTPath pathExp = new ASTObjPath(name);
            pathExp.setPathAliases(expression.getPathAliases());
            return pathExp;
        }

        throw new CayenneRuntimeException("Dot is used only with path expressions.");
    }

    static String substringPath(String propertyName){
        for(int i = propertyName.length() - 1; i >= 0; i--) {
            if(propertyName.charAt(i) == '.') {
                return propertyName.substring(0, i + 1);
            }
        }

        return "";
    }

    static void checkAliases(Expression expression) {
        if(!expression.getPathAliases().isEmpty()) {
            throw new CayenneRuntimeException("Can't use aliases with prefetch");
        }
    }

}
