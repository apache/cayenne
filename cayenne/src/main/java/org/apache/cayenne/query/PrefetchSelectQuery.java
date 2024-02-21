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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.Util;

/**
 * A SelectQuery to perform a prefetch based on another query. Used internally by Cayenne
 * and is normally never used directly.
 *
 * @since 4.2 this query extends {@link ObjectSelect}
 */
public class PrefetchSelectQuery<T> extends ObjectSelect<T> {

    /**
     * The relationship path from root objects to the objects being prefetched.
     */
    protected CayennePath prefetchPath;

    /**
     * Stores the last ObjRelationship in the prefetch path.
     */
    protected ObjRelationship lastPrefetchHint;

    protected Collection<ASTPath> resultPaths;

    /**
     * Creates a new disjoint prefetch select query.
     * 
     * @since 3.1
     */
    public PrefetchSelectQuery(CayennePath prefetchPath, ObjRelationship lastPrefetchHint) {
        entityName(lastPrefetchHint.getTargetEntityName());
        this.prefetchPath = prefetchPath;
        this.lastPrefetchHint = lastPrefetchHint;
    }

    @Override
    protected void routePrefetches(QueryRouter router, EntityResolver resolver) {
        // noop - intentional.
    }

    /**
     * Returns the prefetchPath.
     * @since 5.0 returns {@link CayennePath} instead of a plain {@code String}
     */
    public CayennePath getPrefetchPath() {
        return prefetchPath;
    }

    /**
     * Sets the prefetchPath.
     *
     * @param prefetchPath The prefetchPath to set
     * @see #setPrefetchPath(CayennePath)
     */
    public void setPrefetchPath(String prefetchPath) {
        setPrefetchPath(CayennePath.of(prefetchPath));
    }

    /**
     * Sets the prefetchPath.
     * 
     * @param prefetchPath The prefetchPath to set
     * @since 5.0
     */
    public void setPrefetchPath(CayennePath prefetchPath) {
        this.prefetchPath = prefetchPath;
    }

    /**
     * Clean set of the prefetch tree without any merge with existing nodes.
     *
     * @param prefetch prefetch tree
     * @since 4.2
     */
    public void setPrefetchTree(PrefetchTreeNode prefetch) {
        getBaseMetaData().setPrefetchTree(prefetch);
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
     * Configures an "extra" path that will resolve to an extra column (or columns) in the result set.
     * 
     * @param path A valid path expression. E.g. "abc" or "db:ABC" or "abc.xyz".
     * @since 1.2
     */
    public void addResultPath(String path) {
        if(path.startsWith("db:")) {
            addResultPath(ExpressionFactory.dbPathExp(path));
        } else {
            addResultPath(ExpressionFactory.pathExp(path));
        }
    }

    /**
     * Configures an "extra" path that will resolve to an extra column (or columns) in the result set.
     *
     * @param path a path expression
     * @since 5.0
     */
    public void addResultPath(Expression path) {
        if (!(path instanceof ASTPath)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        nonNullResultPaths().add((ASTPath) path);
    }

    /**
     * Removes an extra result path. Note that this method doesn't check for expression
     * invariants, as it doesn't have a proper context to do so. E.g. for the purpose of
     * this method "db:ARTIST_NAME" and "obj:artistName" are not the same, though both
     * will resolve to the same column name.
     *
     * @see #removeResultPath(CayennePath)
     */
    public void removeResultPath(String path) {
        removeResultPath(CayennePath.of(path));
    }

    /**
     * Removes an extra result path. Note that this method doesn't check for expression
     * invariants, as it doesn't have a proper context to do so. E.g. for the purpose of
     * this method "db:ARTIST_NAME" and "obj:artistName" are not the same, though both
     * will resolve to the same column name.
     * @since 5.0
     */
    public void removeResultPath(CayennePath path) {
        if (resultPaths != null) {
            resultPaths.remove(path);
        }
    }

    /**
     * Returns extra result paths.
     * 
     * @since 1.2
     * @since 5.0 returns collection of {@link CayennePath}
     */
    public Collection<ASTPath> getResultPaths() {
        return resultPaths != null
                ? Collections.unmodifiableCollection(resultPaths)
                : Collections.emptySet();
    }

    /**
     * Returns a Collection that internally stores extra result paths, creating it on demand.
     * 
     * @since 1.2
     * @since 5.0 returns collection of {@link CayennePath}
     */
    Collection<ASTPath> nonNullResultPaths() {
        if (resultPaths == null) {
            resultPaths = new HashSet<>();
        }

        return resultPaths;
    }
}
