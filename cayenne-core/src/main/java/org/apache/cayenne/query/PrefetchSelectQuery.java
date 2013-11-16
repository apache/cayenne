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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.Util;

/**
 * A SelectQuery to perform a prefetch based on another query. Used internally by Cayenne
 * and is normally never used directly.
 */
public class PrefetchSelectQuery extends SelectQuery<Object> {

    /**
     * The relationship path from root objects to the objects being prefetched.
     */
    protected String prefetchPath;

    /**
     * Stores the last ObjRelationship in the prefetch path.
     */
    protected ObjRelationship lastPrefetchHint;

    // TODO, Andrus 11/17/2005 - i guess we should deprecate
    // SelectQuery.customDbAttribute, replacing it with "resultPaths" mechanism.
    protected Collection<String> resultPaths;

    /**
     * Creates a new disjoint prefetch select query.
     * 
     * @since 3.1
     */
    public PrefetchSelectQuery(String prefetchPath, ObjRelationship lastPrefetchHint) {
        setRoot(lastPrefetchHint.getTargetEntity());
        this.prefetchPath = prefetchPath;
        this.lastPrefetchHint = lastPrefetchHint;
    }

    /**
     * Overrides super implementation to suppress disjoint prefetch routing, as the parent
     * query should take care of that.
     * 
     * @since 1.2
     */
    @Override
    void routePrefetches(QueryRouter router, EntityResolver resolver) {
        // noop - intentional.
    }

    /**
     * Returns the prefetchPath.
     */
    public String getPrefetchPath() {
        return prefetchPath;
    }

    /**
     * Sets the prefetchPath.
     * 
     * @param prefetchPath The prefetchPath to set
     */
    public void setPrefetchPath(String prefetchPath) {
        this.prefetchPath = prefetchPath;
    }

    /**
     * Returns last incoming ObjRelationship in the prefetch relationship chain.
     * 
     * @since 1.1
     */
    public ObjRelationship getLastPrefetchHint() {
        return lastPrefetchHint;
    }

    /**
     * @since 1.1
     */
    public void setLastPrefetchHint(ObjRelationship relationship) {
        lastPrefetchHint = relationship;
    }

    /**
     * Configures an "extra" path that will resolve to an extra column (or columns) in the
     * result set.
     * 
     * @param path A valid path expression. E.g. "abc" or "db:ABC" or "abc.xyz".
     * @since 1.2
     */
    public void addResultPath(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        nonNullResultPaths().add(path);
    }

    /**
     * Removes an extra result path. Note that this method doesn't check for expression
     * invariants, as it doesn't have a proper context to do so. E.g. for the purspose of
     * this method "db:ARTIST_NAME" and "obj:artistName" are not the same, though both
     * will resolve to the same column name.
     */
    public void removeResultPath(String path) {
        if (resultPaths != null) {
            resultPaths.remove(path);
        }
    }

    /**
     * Returns extra result paths.
     * 
     * @since 1.2
     */
    public Collection<String> getResultPaths() {
        return resultPaths != null
                ? Collections.unmodifiableCollection(resultPaths)
                : Collections.EMPTY_SET;
    }

    /**
     * Returns a Collection that internally stores extra result paths, creating it on
     * demand.
     * 
     * @since 1.2
     */
    Collection<String> nonNullResultPaths() {
        if (resultPaths == null) {
            resultPaths = new HashSet<String>();
        }

        return resultPaths;
    }
}
