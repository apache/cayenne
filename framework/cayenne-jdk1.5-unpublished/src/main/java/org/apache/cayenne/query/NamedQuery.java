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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.Util;

/**
 * A query that is a reference to a named parameterized query stored in the mapping. The
 * actual query is resolved during execution.
 * 
 * @since 1.2
 */
public class NamedQuery extends IndirectQuery {

    protected Map<String, Object> parameters;

    protected boolean forceNoCache;

    protected BaseQueryMetadata overrideMetadata;

    // metadata fields...
    transient int hashCode;
    
    //to enable serialization
    @SuppressWarnings("unused")
    private NamedQuery() {
    }

    public NamedQuery(String name) {
        this(name, null);
    }

    public NamedQuery(String name, Map<String, ?> parameters) {
        this.name = name;

        // copy parameters map (among other things to make hessian serialization work).
        if (parameters != null && !parameters.isEmpty()) {
            this.parameters = new HashMap<String, Object>(parameters);
        }
    }

    /**
     * Creates NamedQuery with parameters passed as two matching arrays of keys and
     * values.
     */
    public NamedQuery(String name, String[] keys, Object[] values) {
        this.name = name;
        this.parameters = Util.toMap(keys, values);
    }

    @Override
    public QueryMetadata getMetaData(EntityResolver resolver) {

        QueryMetadata base = overrideMetadata != null ? overrideMetadata : super
                .getMetaData(resolver);

        QueryMetadataWrapper wrapper = new QueryMetadataWrapper(base);

        // override cache policy, forcing refresh if needed
        if (forceNoCache) {
            QueryCacheStrategy strategy = base.getCacheStrategy();

            if (QueryCacheStrategy.LOCAL_CACHE == strategy) {
                wrapper.override(
                        QueryMetadata.CACHE_STRATEGY_PROPERTY,
                        QueryCacheStrategy.LOCAL_CACHE_REFRESH);
            }
            else if (QueryCacheStrategy.SHARED_CACHE == strategy) {
                wrapper.override(
                        QueryMetadata.CACHE_STRATEGY_PROPERTY,
                        QueryCacheStrategy.SHARED_CACHE_REFRESH);
            }
        }

        // override cache key to include parameters
        if (parameters != null
                && !parameters.isEmpty()
                && replacementQuery instanceof NamedQuery
                && base.getCacheKey() != null) {

            // TODO: andrus, 3/29/2006 this is taken from SelectQuery...probably need a
            // central place for converting parameters to a cache key

            StringBuilder buffer = new StringBuilder(name);

            if (parameters != null && !parameters.isEmpty()) {
                buffer.append(parameters.hashCode());
            }

            wrapper.override(QueryMetadataWrapper.CACHE_KEY_PROPERTY, buffer.toString());
        }

        return wrapper;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        Query query = resolveQuery(resolver);

        if (query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(normalizedParameters());
        } else if (query instanceof EJBQLQuery) {
            
            Iterator it = normalizedParameters().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                ((EJBQLQuery)query).setParameter((String) pairs.getKey(), pairs.getValue());
            }
        }

        return query;
    }

    /**
     * Returns a non-null parameter map, substituting all persistent objects in the
     * initial map with ObjectIds. This is needed so that a query could work uniformly on
     * the server and client sides.
     */
    Map<String, ?> normalizedParameters() {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        Map<String, Object> substitutes = new HashMap<String, Object>(parameters);

        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Persistent) {
                value = ((Persistent) value).getObjectId();
            }

            substitutes.put(entry.getKey(), value);
        }

        return substitutes;
    }

    /**
     * Returns a query for name, throwing an exception if such query is not mapped in the
     * EntityResolver.
     */
    protected Query resolveQuery(EntityResolver resolver) {
        Query query = resolver.lookupQuery(getName());

        if (query == null) {
            throw new CayenneRuntimeException("Can't find named query for name '"
                    + getName()
                    + "'");
        }

        if (query == this) {
            throw new CayenneRuntimeException("Named query resolves to self: '"
                    + getName()
                    + "'");
        }

        return query;
    }

    /**
     * Overrides toString() outputting a short string with query class and name.
     */
    @Override
    public String toString() {
        String className = getClass().getName();
        return Util.stripPackageName(className) + ":" + getName();
    }

    /**
     * Initializes metadata overrides. Needed to store the metadata for the remote query
     * proxies that have no access to the actual query.
     */
    public void initMetadata(QueryMetadata metadata) {
        if (metadata != null) {
            this.overrideMetadata = new BaseQueryMetadata();
            this.overrideMetadata.copyFromInfo(metadata);
        }
        else {
            this.overrideMetadata = null;
        }
    }

    /**
     * An object is considered equal to this NamedQuery if it is a NamedQuery with the
     * same queryName and same parameters.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof NamedQuery)) {
            return false;
        }

        NamedQuery query = (NamedQuery) object;

        if (!Util.nullSafeEquals(name, query.getName())) {
            return false;
        }

        if (query.parameters == null && parameters == null) {
            return true;
        }

        if (query.parameters == null || parameters == null) {
            return false;
        }

        if (query.parameters.size() != parameters.size()) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();

        for (Map.Entry<String, ?> entry : parameters.entrySet()) {
            String entryKey = entry.getKey();
            Object entryValue = entry.getValue();

            if (entryValue == null) {
                if (query.parameters.get(entryKey) != null
                        || !query.parameters.containsKey(entryKey)) {
                    return false;
                }
            }
            else {
                // takes care of comparing primitive arrays, such as byte[]
                builder.append(entryValue, query.parameters.get(entryKey));
                if (!builder.isEquals()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Implements a standard hashCode contract considering custom 'equals' implementation.
     */
    @Override
    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(13, 17);

            if (name != null) {
                builder.append(name.hashCode());
            }

            if (parameters != null) {
                Object[] keys = parameters.keySet().toArray();
                Arrays.sort(keys);

                for (Object key : keys) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]
                    builder.append(key).append(parameters.get(key));
                }

            }

            this.hashCode = builder.toHashCode();
            assert hashCode != 0 : "Generated zero hashCode";
        }

        return hashCode;
    }

    public boolean isForceNoCache() {
        return forceNoCache;
    }

    public void setForceNoCache(boolean forcingNoCache) {
        this.forceNoCache = forcingNoCache;
    }
}
