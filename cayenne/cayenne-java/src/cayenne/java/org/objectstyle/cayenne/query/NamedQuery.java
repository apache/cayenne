/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.util.Util;

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

            if ((value instanceof Persistent) && !(value instanceof DataObject)) {
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
