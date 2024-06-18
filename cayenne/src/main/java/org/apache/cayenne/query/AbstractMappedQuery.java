/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.query;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractMappedQuery extends IndirectQuery {

    protected String queryName;
    protected Map<String, Object> params;

    protected AbstractMappedQuery(String queryName) {
        this.queryName = queryName;
    }

    public AbstractMappedQuery name(String queryName) {
        this.queryName = queryName;
        return this;
    }

    protected AbstractMappedQuery params(Map<String, ?> parameters) {
        if (this.params == null) {
            this.params = new HashMap<>(parameters);
        } else {
            this.params.putAll(parameters);
        }

        this.replacementQuery = null;

        return this;
    }

    public AbstractMappedQuery param(String name, Object value) {
        if (this.params == null) {
            this.params = new HashMap<>();
        }

        this.params.put(name, value);

        this.replacementQuery = null;

        return this;
    }

    /**
     * Returns a non-null parameter map, substituting all persistent objects in the
     * initial map with ObjectIds. This is needed so that a query could work uniformly on
     * the server and client sides.
     */
    Map<String, ?> normalizedParameters() {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> substitutes = new HashMap<>(params);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Persistent) {
                value = ((Persistent) value).getObjectId();
            }

            substitutes.put(entry.getKey(), value);
        }

        return substitutes;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        Query query = resolver.getQueryDescriptor(queryName).buildQuery();

        if (query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(normalizedParameters());
        } else if (query instanceof EJBQLQuery) {
            for (Map.Entry<String, ?> entry : normalizedParameters().entrySet()) {
                ((EJBQLQuery) query).setParameter(entry.getKey(), entry.getValue());
            }
        }

        return query;
    }
}
