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
package org.apache.cayenne.access.jdbc;

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLException;

/**
 * A holder of multi-column match condition operand.
 * 
 * @since 3.0
 */
public abstract class EJBQLMultiColumnOperand {

    static EJBQLMultiColumnOperand getPathOperand(
            EJBQLTranslationContext context,
            Map pathMap) {
        return new PathMultiColumnOperand(context, pathMap);
    }

    static EJBQLMultiColumnOperand getObjectOperand(
            EJBQLTranslationContext context,
            Map object) {
        return new ObjectMultiColumnOperand(context, object);
    }

    EJBQLTranslationContext context;
    Map map;

    Collection getKeys() {
        return map.keySet();
    }

    abstract void appendValue(Object key);

    private static class PathMultiColumnOperand extends EJBQLMultiColumnOperand {

        PathMultiColumnOperand(EJBQLTranslationContext context, Map pathMap) {
            this.context = context;
            this.map = pathMap;
        }

        @Override
        void appendValue(Object key) {

            Object column = map.get(key);
            if (column == null) {
                throw new EJBQLException("Invalid match path, no match for ID column "
                        + key);
            }

            context.append(' ').append(column.toString());
        }
    }

    private static class ObjectMultiColumnOperand extends EJBQLMultiColumnOperand {

        ObjectMultiColumnOperand(EJBQLTranslationContext context, Map pathMap) {
            this.context = context;
            this.map = pathMap;
        }

        @Override
        void appendValue(Object key) {
            Object value = map.get(key);
            if (value == null) {
                throw new EJBQLException("Invalid object, no match for ID column " + key);
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(')');
        }
    }
}
