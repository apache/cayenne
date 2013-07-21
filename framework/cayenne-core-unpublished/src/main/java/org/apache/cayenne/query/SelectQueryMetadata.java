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
package org.apache.cayenne.query;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.0
 */
class SelectQueryMetadata extends BaseQueryMetadata {

    Map<String, String> pathSplitAliases;

    @Override
    void copyFromInfo(QueryMetadata info) {
        super.copyFromInfo(info);
        this.pathSplitAliases = new HashMap<String, String>(info.getPathSplitAliases());
    }

    <T> boolean resolve(Object root, EntityResolver resolver, SelectQuery<T> query) {

        if (super.resolve(root, resolver, null)) {

            // generate unique cache key...
            if (QueryCacheStrategy.NO_CACHE == getCacheStrategy()) {

            } else {
                // create a unique key based on entity, qualifier, ordering and
                // fetch offset and limit

                StringBuilder key = new StringBuilder();

                ObjEntity entity = getObjEntity();
                if (entity != null) {
                    key.append(entity.getName());
                } else if (dbEntity != null) {
                    key.append("db:").append(dbEntity.getName());
                }

                if (query.getQualifier() != null) {
                    key.append('/');
                    try {
                        query.getQualifier().appendAsString(key);
                    } catch (IOException e) {
                        throw new CayenneRuntimeException("Unexpected IO Exception appending to StringBuilder", e);
                    }
                }

                if (!query.getOrderings().isEmpty()) {
                    for (Ordering o : query.getOrderings()) {
                        key.append('/').append(o.getSortSpecString());
                        if (!o.isAscending()) {
                            key.append(":d");
                        }

                        if (o.isCaseInsensitive()) {
                            key.append(":i");
                        }
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
            }

            resolveAutoAliases(query);

            return true;
        }

        return false;
    }

    private <T> void resolveAutoAliases(SelectQuery<T> query) {
        Expression qualifier = query.getQualifier();
        if (qualifier != null) {
            resolveAutoAliases(qualifier);
        }

        // TODO: include aliases in prefetches? flattened attributes?
    }

    private void resolveAutoAliases(Expression expression) {
        Map<String, String> aliases = expression.getPathAliases();
        if (!aliases.isEmpty()) {
            if (pathSplitAliases == null) {
                pathSplitAliases = new HashMap<String, String>();
            }

            pathSplitAliases.putAll(aliases);
        }

        int len = expression.getOperandCount();
        for (int i = 0; i < len; i++) {
            Object operand = expression.getOperand(i);
            if (operand instanceof Expression) {
                resolveAutoAliases((Expression) operand);
            }
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public Map<String, String> getPathSplitAliases() {
        return pathSplitAliases != null ? pathSplitAliases : Collections.<String, String> emptyMap();
    }

    /**
     * @since 3.0
     */
    public void addPathSplitAliases(String path, String... aliases) {
        if (aliases == null) {
            throw new NullPointerException("Null aliases");
        }

        if (aliases.length == 0) {
            throw new IllegalArgumentException("No aliases specified");
        }

        if (pathSplitAliases == null) {
            pathSplitAliases = new HashMap<String, String>();
        }

        for (String alias : aliases) {
            pathSplitAliases.put(alias, path);
        }
    }
}
