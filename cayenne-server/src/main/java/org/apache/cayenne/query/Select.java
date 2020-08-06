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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;

import java.util.List;

/**
 * A common interface for grouping together different kinds of queries that
 * return results.
 */
public interface Select<T> extends Query {

	/**
	 * Selects objects using provided context.
	 * <p>
	 * Essentially the inversion of "ObjectContext.select(Select)".
	 *
	 * @since 4.0
	 */
	List<T> select(ObjectContext context);

	/**
	 * Selects a single object using provided context. The query is expected to
	 * match zero or one object. It returns null if no objects were matched. If
	 * query matched more than one object,
	 * {@link org.apache.cayenne.CayenneRuntimeException} is thrown.
	 * <p>
	 * Essentially the inversion of "ObjectContext.selectOne(Select)".
	 *
	 * @since 4.0
	 */
	T selectOne(ObjectContext context);

	/**
	 * Selects a single object using provided context. The query itself can
	 * match any number of objects, but will return only the first one. It
	 * returns null if no objects were matched.
	 * <p>
	 * If it matched more than one object, the first object from the list is
	 * returned. This makes 'selectFirst' different from
	 * {@link #selectOne(ObjectContext)}, which would throw in this situation.
	 * 'selectFirst' is useful e.g. when the query is ordered and we only want
	 * to see the first object (e.g. "most recent news article"), etc.
	 * <p>
	 * Selecting the first object via "Select.selectFirst(ObjectContext)" is
	 * more comprehensible than selecting via
	 * "ObjectContext.selectFirst(Select)", because implementations of "Select"
	 * set fetch size limit to one.
	 *
	 * @since 4.0
	 */
	T selectFirst(ObjectContext context);

	/**
	 * Creates a ResultIterator based on the provided context and passes it to a
	 * callback for processing. The caller does not need to worry about closing
	 * the iterator. This method takes care of it.
	 * <p>
	 * Essentially the inversion of
	 * "ObjectContext.iterate(Select, ResultIteratorCallback)".
	 *
	 * @since 4.0
	 */
	void iterate(ObjectContext context, ResultIteratorCallback<T> callback);

	/**
	 * Creates a ResultIterator based on the provided context. It is usually
	 * backed by an open result set and is useful for processing of large data
	 * sets, preserving a constant memory footprint. The caller must wrap
	 * iteration in try/finally (or try-with-resources for Java 1.7 and higher)
	 * and close the ResultIterator explicitly. Or use
	 * {@link #iterate(ObjectContext, ResultIteratorCallback)} as an
	 * alternative.
	 * <p>
	 * Essentially the inversion of "ObjectContext.iterator(Select)".
	 *
	 * @since 4.0
	 */
	ResultIterator<T> iterator(ObjectContext context);

	/**
	 * Creates a ResultBatchIterator based on the provided context and batch
	 * size. It is usually backed by an open result set and is useful for
	 * processing of large data sets, preserving a constant memory footprint.
	 * The caller must wrap iteration in try/finally (or try-with-resources for
	 * Java 1.7 and higher) and close the ResultBatchIterator explicitly.
	 *
	 * @since 4.0
	 */
	ResultBatchIterator<T> batchIterator(ObjectContext context, int size);
}
