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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;

import java.util.List;

/**
 * A common interface for grouping together different kinds of queries
 * that return results. 
 */
public interface Select<T> extends Query {

    /**
     * Selects objects using provided context.
     * <p>
     * Essentially the inversion of "ObjectContext.select(Select)".
     * @since 4.0
     */
    <T> List<T> select(ObjectContext context);

    /**
     * Selects a single object using provided context. The query is expected to
     * match zero or one object. It returns null if no objects were matched. If
     * query matched more than one object, {@link org.apache.cayenne.CayenneRuntimeException} is
     * thrown.
     * <p>
     * Essentially the inversion of "ObjectContext.selectOne(Select)".
     */
    <T> T selectOne(ObjectContext context);

    /**
     * Creates a ResultIterator based on the provided context and passes it to a
     * callback for processing. The caller does not need to worry about closing
     * the iterator. This method takes care of it.
     * <p>
     * Essentially the inversion of "ObjectContext.iterate(Select, ResultIteratorCallback)".
     * @since 4.0
     */
    <T> void iterate(ObjectContext context, ResultIteratorCallback<T> callback);

    /**
     * Creates a ResultIterator based on the provided context. It is usually
     * backed by an open result set and is useful for processing of large data
     * sets, preserving a constant memory footprint. The caller must wrap
     * iteration in try/finally (or try-with-resources for Java 1.7 and higher) and
     * close the ResultIterator explicitly.
     * Or use {@link #iterate(ObjectContext, ResultIteratorCallback)} as an alternative.
     * <p>
     * Essentially the inversion of "ObjectContext.iterator(Select)".
     * @since 4.0
     */
    <T> ResultIterator<T> iterator(ObjectContext context);
}
