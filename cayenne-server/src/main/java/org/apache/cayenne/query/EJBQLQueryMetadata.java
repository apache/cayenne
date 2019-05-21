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
package org.apache.cayenne.query;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A metadata object for the {@link EJBQLQuery}.
 * 
 * @since 3.0
 */
class EJBQLQueryMetadata extends BaseQueryMetadata {

    boolean resolve(EntityResolver resolver, EJBQLQuery query) {
        EJBQLCompiledExpression expression = query.getExpression(resolver);
        setPrefetchTree(expression.getPrefetchTree());
        resultSetMapping = expression.getResult() != null
                ? expression.getResult().getResolvedComponents(resolver)
                : null;

        ObjEntity root = expression.getRootDescriptor().getEntity();

        if (!super.resolve(root, resolver)) {
            return false;
        }

        if (QueryCacheStrategy.NO_CACHE == getCacheStrategy()) {
            return true;
        }

        // create a unique key based on entity, EJBQL, and parameters
        StringBuilder key = new StringBuilder();

        if (query.getEjbqlStatement() != null) {
            key.append('/').append(query.getEjbqlStatement());
        }

        if (query.getFetchLimit() > 0) {
            key.append('/').append(query.getFetchLimit());
        }

        Map<String, Object> namedParameters = query.getNamedParameters();
        if (!namedParameters.isEmpty()) {
            List<String> keys = new ArrayList<>(namedParameters.keySet());
            Collections.sort(keys);
            for (String parameterKey : keys) {
                key.append('/').append(parameterKey).append('=').append(
                        namedParameters.get(parameterKey));
            }
        }

        Map<Integer, Object> positionalParameters = query.getPositionalParameters();
        if (!positionalParameters.isEmpty()) {
            List<Integer> keys = new ArrayList<>(positionalParameters.keySet());
            Collections.sort(keys);
            for (Integer parameterKey : keys) {
                key.append('/').append(parameterKey).append('=').append(
                        positionalParameters.get(parameterKey));
            }
        }

        if (query.getFetchOffset() > 0 || query.getFetchLimit() > 0) {
            key.append('/');
            if (query.getFetchOffset() > 0) {
                key.append('o').append(query.getFetchOffset());
            }
            if (query.getFetchLimit() > 0) {
                key.append('l').append(query.getFetchLimit());
            }
        }

        this.cacheKey = key.toString();

        return true;
    }
}
