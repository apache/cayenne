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
import org.apache.cayenne.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * A query that is a reference to a named parameterized query stored in the mapping. The
 * actual query is resolved during execution.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class NamedQuery extends IndirectQuery {

    protected Map parameters;

    protected boolean forceNoCache;

    protected BaseQueryMetadata overrideMetadata;

    // metadata fields...
    transient int hashCode;

    public NamedQuery(String name) {
        this(name, null);
    }

    public NamedQuery(String name, Map parameters) {
        this.name = name;

        // copy parameters map (among other things to make hessian serilaization work).
        if (parameters != null && !parameters.isEmpty()) {
            this.parameters = new HashMap(parameters);
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

    public QueryMetadata getMetaData(EntityResolver resolver) {

        QueryMetadata base = overrideMetadata != null ? overrideMetadata : super
                .getMetaData(resolver);

        QueryMetadataWrapper wrapper = new QueryMetadataWrapper(base);

        // override cache policy, forcing refresh if needed
        if (forceNoCache) {
            String policy = base.getCachePolicy();

            if (QueryMetadata.LOCAL_CACHE.equals(policy)) {
                wrapper.override(
                        QueryMetadata.CACHE_POLICY_PROPERTY,
                        QueryMetadata.LOCAL_CACHE_REFRESH);
            }
            else if (QueryMetadata.SHARED_CACHE.equals(policy)) {
                wrapper.override(
                        QueryMetadata.CACHE_POLICY_PROPERTY,
                        QueryMetadata.SHARED_CACHE_REFRESH);
            }
        }

        // override cache key to include parameters
        if (parameters != null
                && !parameters.isEmpty()
                && replacementQuery instanceof NamedQuery
                && base.getCacheKey() != null) {

            // TODO: andrus, 3/29/2006 this is taken from SelectQuery...probably need a
            // central place for converting parameters to a cache key

            StringBuffer buffer = new StringBuffer(name);

            if (parameters != null && !parameters.isEmpty()) {
                buffer.append(parameters.hashCode());
            }

            wrapper.override(QueryMetadataWrapper.CACHE_KEY_PROPERTY, buffer.toString());
        }

        return wrapper;
    }

    protected Query createReplacementQuery(EntityResolver resolver) {
        Query query = resolveQuery(resolver);

        if (query instanceof ParameterizedQuery) {
            query = ((ParameterizedQuery) query).createQuery(normalizedParameters());
        }

        return query;
    }

    /**
     * Returns a non-null parameter map, substituting all persistent objects in the
     * initial map with ObjectIds. This is needed so that a query could work uniformly on
     * the server and client sides.
     */
    Map normalizedParameters() {
        if (parameters == null || parameters.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        Map substitutes = new HashMap(parameters);

        Iterator it = parameters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

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
    public String toString() {
        return StringUtils.substringAfterLast(getClass().getName(), ".")
                + ":"
                + getName();
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
        Iterator entries = parameters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Object entryKey = entry.getKey();
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
    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(13, 17);

            if (name != null) {
                builder.append(name.hashCode());
            }

            if (parameters != null) {
                Object[] keys = parameters.keySet().toArray();
                Arrays.sort(keys);

                for (int i = 0; i < keys.length; i++) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]
                    builder.append(keys[i]).append(parameters.get(keys[i]));
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
