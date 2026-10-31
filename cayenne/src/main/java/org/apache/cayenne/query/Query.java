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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;

import java.util.List;

/**
 * A query that can be executed against an {@link ObjectContext}. Any query can be run via
 * {@link #execute(ObjectContext)}, returning everything it produced: result sets, update counts, OUT parameters. A
 * query that selects rows of a known type also has a set of selecting shortcuts, such as
 * {@link #select(ObjectContext)}, returning the rows of its first result set.
 *
 * @param <T> the type of the rows the query selects. Queries that produce no rows (updates, DDL, batches) are typed
 *            as {@code Query<?>}.
 */
public interface Query<T> {

    /**
     * Returns query runtime parameters. The method is called at various stages of the
     * execution by Cayenne access stack to retrieve query parameters. EntityResolver
     * instance is passed to this method, meaning that the query doesn't need to store
     * direct references to Cayenne mapping objects and can resolve them at runtime.
     * 
     * @since 1.2
     */
    QueryMetadata getMetaData(EntityResolver resolver);

    /**
     * A callback method invoked by Cayenne during the routing phase of the query
     * execution. Mapping of DataNodes is provided by QueryRouter. Query should use a
     * {@link QueryRouter#route(DataNode, Query, Query)} callback method to route
     * itself. Query can create one or more substitute queries or even provide its own
     * DataNode to execute itself.
     * <p>
     * The default implementation relies on the EntityResolver to find the DataMap based on
     * the query metadata. This mechanism is sufficient for most queries that "know" their root.
     * 
     * @since 1.2
     */
    default void route(QueryRouter router, EntityResolver resolver, Query<?> substitutedQuery) {
        DataMap map = getMetaData(resolver).getDataMap();

        if (map == null) {
            throw new CayenneRuntimeException("No DataMap found, can't route query %s", this);
        }

        router.route(router.nodeForDataMap(map), this, substitutedQuery);
    }

    /**
     * A callback method invoked by Cayenne during the final execution phase of the query
     * run. A concrete query implementation is given a chance to decide how it should be
     * handled. Implementors can pick an appropriate method of the SQLActionVisitor to
     * handle itself, create a custom SQLAction of its own, or substitute itself with
     * another query that should be used for SQLAction construction.
     * 
     * @since 1.2
     */
    SQLAction createSQLAction(SQLActionVisitor visitor);

    /**
     * Executes the query, returning all of its result sets, update counts, iterators and OUT parameters in the order
     * they were produced. Essentially the inversion of "ObjectContext.execute(Query)".
     *
     * @since 6.0
     */
    default List<QueryResult> execute(ObjectContext context) {
        return context.execute(this);
    }

    /**
     * Selects objects using provided context, returning the rows of the first result set the query produced, or an
     * empty list if it produced none.
     * <p>
     * Essentially the inversion of "ObjectContext.select(Query)".
     *
     * @since 4.0
     */
    default List<T> select(ObjectContext context) {
        return context.select(this);
    }

    /**
     * Selects a single object using provided context. The query is expected to match zero or one object. It returns
     * null if no objects were matched. If query matched more than one object,
     * {@link CayenneRuntimeException} is thrown.
     * <p>
     * Essentially the inversion of "ObjectContext.selectOne(Query)".
     *
     * @since 4.0
     */
    default T selectOne(ObjectContext context) {
        return context.selectOne(this);
    }

    /**
     * Selects a single object using provided context. The query itself can match any number of objects, but will
     * return only the first one. It returns null if no objects were matched.
     * <p>
     * If it matched more than one object, the first object from the list is returned. This makes 'selectFirst'
     * different from {@link #selectOne(ObjectContext)}, which would throw in this situation. 'selectFirst' is useful
     * e.g. when the query is ordered and we only want to see the first object (e.g. "most recent news article"), etc.
     * <p>
     * Selecting the first object via "Query.selectFirst(ObjectContext)" is preferable to selecting via
     * "ObjectContext.selectFirst(Query)", because queries that support a fetch limit set it to one, optimizing the
     * generated SQL. The default implementation does not touch the limit.
     *
     * @since 4.0
     */
    default T selectFirst(ObjectContext context) {
        return context.selectFirst(this);
    }

    /**
     * Creates a ResultIterator based on the provided context and passes it to a callback for processing. The caller
     * does not need to worry about closing the iterator. This method takes care of it.
     * <p>
     * Essentially the inversion of "ObjectContext.iterate(Query, ResultIteratorCallback)".
     *
     * @since 4.0
     */
    default void iterate(ObjectContext context, ResultIteratorCallback<T> callback) {
        context.iterate(this, callback);
    }

    /**
     * Creates a ResultIterator based on the provided context. It is usually backed by an open result set and is
     * useful for processing of large data sets, preserving a constant memory footprint. The caller must wrap
     * iteration in try-with-resources and close the ResultIterator explicitly. Or use
     * {@link #iterate(ObjectContext, ResultIteratorCallback)} as an alternative.
     * <p>
     * Essentially the inversion of "ObjectContext.iterator(Query)".
     *
     * @since 4.0
     */
    default ResultIterator<T> iterator(ObjectContext context) {
        return context.iterator(this);
    }

    /**
     * Creates a ResultBatchIterator based on the provided context and batch size. It is usually backed by an open
     * result set and is useful for processing of large data sets, preserving a constant memory footprint. The caller
     * must wrap iteration in try-with-resources and close the ResultBatchIterator explicitly.
     *
     * @since 4.0
     */
    default ResultBatchIterator<T> batchIterator(ObjectContext context, int size) {
        return context.batchIterator(this, size);
    }
}
