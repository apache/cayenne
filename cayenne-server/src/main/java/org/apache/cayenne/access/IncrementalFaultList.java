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

package org.apache.cayenne.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;

/**
 * A synchronized list that serves as a container of DataObjects. It is returned
 * when a paged query is performed by DataContext. On creation, only the first
 * "page" is fully resolved, for the rest of the objects only their ObjectIds
 * are read. Pages following the first page are resolved on demand only. On
 * access to an element, the list would ensure that this element as well as all
 * its siblings on the same page are fully resolved.
 * <p>
 * The list can hold DataRows or DataObjects. Attempts to add any other object
 * types will result in an exception.
 * </p>
 * <p>
 * Performance note: certain operations like <code>toArray</code> would trigger
 * full list fetch.
 * </p>
 */
public class IncrementalFaultList<E> implements List<E>, Serializable {

	protected int pageSize;
	protected List elements;
	protected DataContext dataContext;
	protected ObjEntity rootEntity;
	protected SelectQuery<?> internalQuery;
	protected int unfetchedObjects;

	/**
	 * Stores a hint allowing to distinguish data rows from unfetched ids when
	 * the query fetches data rows.
	 */
	protected int idWidth;

	private IncrementalListHelper helper;

	/**
	 * Defines the upper limit on the size of fetches. This is needed to avoid
	 * where clause size limitations.
	 */
	protected int maxFetchSize;

	// Don't confuse this with the JDBC ResultSet fetch size setting - this
	// controls
	// the where clause generation that is necessary to fetch specific records a
	// page
	// at a time. Some JDBC Drivers/Databases may have limits on statement
	// length
	// or complexity of the where clause - e.g., PostgreSQL having a default
	// limit of
	// 10,000 nested expressions.

	/**
	 * Creates a new IncrementalFaultList using a given DataContext and query.
	 * 
	 * @param dataContext
	 *            DataContext used by IncrementalFaultList to fill itself with
	 *            objects.
	 * @param query
	 *            Main query used to retrieve data. Must have "pageSize"
	 *            property set to a value greater than zero.
	 * @param maxFetchSize
	 *            maximum number of fetches in one query
	 */
	public IncrementalFaultList(DataContext dataContext, Query query, int maxFetchSize) {
		QueryMetadata metadata = query.getMetaData(dataContext.getEntityResolver());
		if (metadata.getPageSize() <= 0) {
			throw new CayenneRuntimeException("Not a paginated query; page size: " + metadata.getPageSize());
		}

		this.dataContext = dataContext;
		this.pageSize = metadata.getPageSize();
		this.rootEntity = metadata.getObjEntity();

		if (rootEntity == null) {
			throw new CayenneRuntimeException("Pagination is not supported for queries not rooted in an ObjEntity");
		}

		// create an internal query, it is a partial replica of
		// the original query and will serve as a value holder for
		// various parameters
		this.internalQuery = new SelectQuery<Object>(rootEntity);
		this.internalQuery.setFetchingDataRows(metadata.isFetchingDataRows());
		this.internalQuery.setPrefetchTree(metadata.getPrefetchTree());

		this.helper = createHelper(metadata);
		this.idWidth = metadata.getDbEntity().getPrimaryKeys().size();

		List<Object> elementsUnsynced = new ArrayList<Object>();
		fillIn(query, elementsUnsynced);
		this.elements = Collections.synchronizedList(elementsUnsynced);

		this.maxFetchSize = maxFetchSize;
	}

	/**
	 * @since 3.0
	 */
	IncrementalListHelper createHelper(QueryMetadata metadata) {
		if (metadata.isFetchingDataRows()) {
			return new DataRowListHelper();
		} else {
			return new PersistentListHelper();
		}
	}

	/**
	 * @since 1.2
	 */
	SelectQuery getInternalQuery() {
		return internalQuery;
	}

	/**
	 * Performs initialization of the list of objects. Only the first page is
	 * fully resolved. For the rest of the list, only ObjectIds are read.
	 * 
	 * @since 3.0
	 */
	protected void fillIn(final Query query, List elementsList) {

		elementsList.clear();

		try (ResultIterator it = dataContext.performIteratedQuery(query);) {

			while (it.hasNextRow()) {
				elementsList.add(it.nextRow());
			}
		}

		unfetchedObjects = elementsList.size();
	}

	/**
	 * Will resolve all unread objects.
	 */
	public void resolveAll() {
		resolveInterval(0, size());
	}

	/**
	 * Checks that an object is of the same type as the rest of objects
	 * (DataObject or DataRows depending on the query type).
	 */
	private void validateListObject(Object object) throws IllegalArgumentException {

		// I am not sure if such a check makes sense???

		if (internalQuery.isFetchingDataRows()) {
			if (!(object instanceof Map)) {
				throw new IllegalArgumentException("Only Map objects can be stored in this list.");
			}
		} else {
			if (!(object instanceof Persistent)) {
				throw new IllegalArgumentException("Only DataObjects can be stored in this list.");
			}
		}
	}

	/**
	 * Resolves a sublist of objects starting at <code>fromIndex</code> up to
	 * but not including <code>toIndex</code>. Internally performs bound
	 * checking and trims indexes accordingly.
	 */
	protected void resolveInterval(int fromIndex, int toIndex) {
		if (fromIndex >= toIndex) {
			return;
		}

		synchronized (elements) {
			if (elements.size() == 0) {
				return;
			}

			// perform bound checking
			if (fromIndex < 0) {
				fromIndex = 0;
			}

			if (toIndex > elements.size()) {
				toIndex = elements.size();
			}

			List<Expression> quals = new ArrayList<Expression>(pageSize);
			List<Object> ids = new ArrayList<Object>(pageSize);
			for (int i = fromIndex; i < toIndex; i++) {
				Object object = elements.get(i);
				if (helper.unresolvedSuspect(object)) {
					quals.add(buildIdQualifier(object));
					ids.add(object);
				}
			}

			int qualsSize = quals.size();
			if (qualsSize == 0) {
				return;
			}

			// fetch the range of objects in fetchSize chunks
			boolean fetchesDataRows = internalQuery.isFetchingDataRows();
			List<Object> objects = new ArrayList<Object>(qualsSize);

			int fetchSize = maxFetchSize > 0 ? maxFetchSize : Integer.MAX_VALUE;

			int fetchEnd = Math.min(qualsSize, fetchSize);
			int fetchBegin = 0;
			while (fetchBegin < qualsSize) {
				SelectQuery<Object> query = new SelectQuery<Object>(rootEntity, ExpressionFactory.joinExp(
						Expression.OR, quals.subList(fetchBegin, fetchEnd)));

				query.setFetchingDataRows(fetchesDataRows);

				if (!query.isFetchingDataRows()) {
					query.setPrefetchTree(internalQuery.getPrefetchTree());
				}

				objects.addAll(dataContext.performQuery(query));
				fetchBegin = fetchEnd;
				fetchEnd += Math.min(fetchSize, qualsSize - fetchEnd);
			}

			// sanity check - database data may have changed
			checkPageResultConsistency(objects, ids);

			// replace ids in the list with objects
			Iterator it = objects.iterator();
			while (it.hasNext()) {
				helper.updateWithResolvedObjectInRange(it.next(), fromIndex, toIndex);
			}

			unfetchedObjects -= objects.size();
		}
	}

	/**
	 * Returns a qualifier expression for an unresolved id object.
	 * 
	 * @since 3.0
	 */
	Expression buildIdQualifier(Object id) {

		Map<String, ?> map = (Map<String, ?>) id;
		if (map.isEmpty()) {
			throw new CayenneRuntimeException("Empty id map");
		}

		return ExpressionFactory.matchAllDbExp(map, Expression.EQUAL_TO);
	}

	/**
	 * @since 3.0
	 */
	void checkPageResultConsistency(List<?> objects, List<?> ids) {

		if (objects.size() < ids.size()) {
			// find missing ids
			StringBuilder buffer = new StringBuilder();
			buffer.append("Some ObjectIds are missing from the database. ");
			buffer.append("Expected ").append(ids.size()).append(", fetched ").append(objects.size());

			boolean first = true;
			for (Object id : ids) {
				boolean found = false;

				for (Object object : objects) {

					if (helper.replacesObject(object, id)) {
						found = true;
						break;
					}
				}

				if (!found) {
					if (first) {
						first = false;
					} else {
						buffer.append(", ");
					}

					buffer.append(id.toString());
				}
			}

			throw new CayenneRuntimeException(buffer.toString());
		} else if (objects.size() > ids.size()) {
			throw new CayenneRuntimeException("Expected " + ids.size() + " objects, retrieved " + objects.size());
		}
	}

	/**
	 * Returns zero-based index of the virtual "page" for a given array element
	 * index.
	 */
	public int pageIndex(int elementIndex) {
		if (elementIndex < 0 || elementIndex > size()) {
			throw new IndexOutOfBoundsException("Index: " + elementIndex);
		}

		if (pageSize <= 0 || elementIndex < 0) {
			return -1;
		}

		return elementIndex / pageSize;
	}

	/**
	 * Get the upper bound on the number of records to resolve in one round trip
	 * to the database. This setting governs the size/complexity of the where
	 * clause generated to retrieve the next page of records. If the fetch size
	 * is less than the page size, then multiple fetches will be made to resolve
	 * a page.
	 */
	public int getMaxFetchSize() {
		return maxFetchSize;
	}

	public void setMaxFetchSize(int fetchSize) {
		this.maxFetchSize = fetchSize;
	}

	/**
	 * Returns the dataContext.
	 * 
	 * @return DataContext
	 */
	public DataContext getDataContext() {
		return dataContext;
	}

	/**
	 * Returns the pageSize.
	 * 
	 * @return int
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * Returns a list iterator for this list. DataObjects are resolved a page
	 * (according to getPageSize()) at a time as necessary - when retrieved with
	 * next() or previous().
	 */
	public ListIterator<E> listIterator() {
		return new IncrementalListIterator(0);
	}

	/**
	 * Returns a list iterator of the elements in this list (in proper
	 * sequence), starting at the specified position in this list. The specified
	 * index indicates the first element that would be returned by an initial
	 * call to the next method. An initial call to the previous method would
	 * return the element with the specified index minus one. DataObjects are
	 * resolved a page at a time (according to getPageSize()) as necessary -
	 * when retrieved with next() or previous().
	 */
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > size()) {
			throw new IndexOutOfBoundsException("Index: " + index);
		}

		return new IncrementalListIterator(index);
	}

	/**
	 * Return an iterator for this list. DataObjects are resolved a page
	 * (according to getPageSize()) at a time as necessary - when retrieved with
	 * next().
	 */
	public Iterator<E> iterator() {
		// by virtue of get(index)'s implementation, resolution of ids into
		// objects will occur on pageSize boundaries as necessary.
		return new Iterator<E>() {

			int listIndex = 0;

			public boolean hasNext() {
				return (listIndex < elements.size());
			}

			public E next() {
				if (listIndex >= elements.size())
					throw new NoSuchElementException("no more elements");

				return get(listIndex++);
			}

			public void remove() {
				throw new UnsupportedOperationException("remove not supported.");
			}
		};
	}

	/**
	 * @see java.util.List#add(int, Object)
	 */
	public void add(int index, Object element) {
		validateListObject(element);

		synchronized (elements) {
			elements.add(index, element);
		}
	}

	/**
	 * @see java.util.Collection#add(Object)
	 */
	public boolean add(Object o) {
		validateListObject(o);

		synchronized (elements) {
			return elements.add(o);
		}
	}

	/**
	 * @see java.util.Collection#addAll(Collection)
	 */
	public boolean addAll(Collection<? extends E> c) {
		synchronized (elements) {
			return elements.addAll(c);
		}
	}

	/**
	 * @see java.util.List#addAll(int, Collection)
	 */
	public boolean addAll(int index, Collection<? extends E> c) {
		synchronized (elements) {
			return elements.addAll(index, c);
		}
	}

	/**
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		synchronized (elements) {
			elements.clear();
		}
	}

	/**
	 * @see java.util.Collection#contains(Object)
	 */
	public boolean contains(Object o) {
		synchronized (elements) {
			return elements.contains(o);
		}
	}

	/**
	 * @see java.util.Collection#containsAll(Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		synchronized (elements) {
			return elements.containsAll(c);
		}
	}

	public E get(int index) {
		synchronized (elements) {
			Object o = elements.get(index);

			if (helper.unresolvedSuspect(o)) {
				// read this page
				int pageStart = pageIndex(index) * pageSize;
				resolveInterval(pageStart, pageStart + pageSize);

				return (E) elements.get(index);
			} else {
				return (E) o;
			}
		}
	}

	/**
	 * @see java.util.List#indexOf(Object)
	 */
	public int indexOf(Object o) {
		return helper.indexOfObject(o);
	}

	/**
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		synchronized (elements) {
			return elements.isEmpty();
		}
	}

	public int lastIndexOf(Object o) {
		return helper.lastIndexOfObject(o);
	}

	public E remove(int index) {
		synchronized (elements) {
			// have to resolve the page to return correct object
			E object = get(index);
			elements.remove(index);
			return object;
		}
	}

	public boolean remove(Object o) {
		synchronized (elements) {
			return elements.remove(o);
		}
	}

	public boolean removeAll(Collection<?> c) {
		synchronized (elements) {
			return elements.removeAll(c);
		}
	}

	public boolean retainAll(Collection<?> c) {
		synchronized (elements) {
			return elements.retainAll(c);
		}
	}

	/**
	 * @see java.util.List#set(int, Object)
	 */
	public E set(int index, Object element) {
		validateListObject(element);

		synchronized (elements) {
			return (E) elements.set(index, element);
		}
	}

	/**
	 * @see java.util.Collection#size()
	 */
	public int size() {
		synchronized (elements) {
			return elements.size();
		}
	}

	public List<E> subList(int fromIndex, int toIndex) {
		synchronized (elements) {
			resolveInterval(fromIndex, toIndex);
			return elements.subList(fromIndex, toIndex);
		}
	}

	public Object[] toArray() {
		resolveAll();

		return elements.toArray();
	}

	public <T> T[] toArray(T[] a) {
		resolveAll();

		return (T[]) elements.toArray(a);
	}

	/**
	 * Returns a total number of objects that are not resolved yet.
	 */
	public int getUnfetchedObjects() {
		return unfetchedObjects;
	}

	abstract class IncrementalListHelper implements Serializable {

		int indexOfObject(Object object) {
			if (unresolvedSuspect(object)) {
				return -1;
			}

			synchronized (elements) {
				for (int i = 0; i < elements.size(); i++) {
					if (objectsAreEqual(object, elements.get(i))) {
						return i;
					}
				}
			}
			return -1;
		}

		int lastIndexOfObject(Object object) {
			if (unresolvedSuspect(object)) {
				return -1;
			}

			synchronized (elements) {
				for (int i = elements.size() - 1; i >= 0; i--) {
					if (objectsAreEqual(object, elements.get(i))) {
						return i;
					}
				}
			}

			return -1;
		}

		void updateWithResolvedObjectInRange(Object object, int from, int to) {
			boolean found = false;

			synchronized (elements) {

				for (int i = from; i < to; i++) {
					if (replacesObject(object, elements.get(i))) {
						elements.set(i, object);
						found = true;
						break;
					}
				}
			}

			if (!found) {
				throw new CayenneRuntimeException("Can't find id for " + object);
			}
		}

		/**
		 * Returns true if an object is not the type of object expected in the
		 * list. This method is not expected to perform thorough checking of the
		 * object type. What's important is the guarantee that an unresolved
		 * object representation will always return true for this method, and
		 * resolved will return false. Other types of objects that users may
		 * choose to add to the list will not be analyzed in detail.
		 */
		abstract boolean unresolvedSuspect(Object object);

		abstract boolean objectsAreEqual(Object object, Object objectInTheList);

		abstract boolean replacesObject(Object object, Object objectInTheList);
	}

	class PersistentListHelper extends IncrementalListHelper {

		@Override
		boolean unresolvedSuspect(Object object) {
			if (!(object instanceof Persistent)) {
				return true;
			}

			// don't do a full check for object type matching the type of
			// objects in the
			// list... what's important is a quick "false" return if the object
			// is of type
			// representing unresolved objects.. furthermore, if inheritance is
			// involved,
			// we'll need an even more extensive check (see CAY-1142 on
			// inheritance
			// issues).

			return false;
		}

		@Override
		boolean objectsAreEqual(Object object, Object objectInTheList) {

			if (objectInTheList instanceof Persistent) {
				// due to object uniquing this should be sufficient
				return object == objectInTheList;
			} else {
				return ((Persistent) object).getObjectId().getIdSnapshot().equals(objectInTheList);
			}
		}

		@Override
		boolean replacesObject(Object object, Object objectInTheList) {
			if (objectInTheList instanceof Persistent) {
				return false;
			}

			Persistent dataObject = (Persistent) object;
			return dataObject.getObjectId().getIdSnapshot().equals(objectInTheList);
		}
	}

	class DataRowListHelper extends IncrementalListHelper {

		@Override
		boolean unresolvedSuspect(Object object) {
			if (!(object instanceof Map)) {
				return true;
			}

			return false;
		}

		@Override
		boolean objectsAreEqual(Object object, Object objectInTheList) {
			if (object == null && objectInTheList == null) {
				return true;
			}

			if (object != null && objectInTheList != null) {

				Map<?, ?> id = (Map<?, ?>) objectInTheList;
				Map<?, ?> map = (Map<?, ?>) object;

				if (id.size() != map.size()) {
					return false;
				}

				// id must be a subset of this map
				for (Map.Entry<?, ?> entry : id.entrySet()) {
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (!Util.nullSafeEquals(value, map.get(key))) {
						return false;
					}
				}

				return true;
			}

			return false;
		}

		@Override
		boolean replacesObject(Object object, Object objectInTheList) {

			Map<?, ?> id = (Map<?, ?>) objectInTheList;
			if (id.size() > idWidth) {
				return false;
			}

			// id must be a subset of this map
			Map<?, ?> map = (Map<?, ?>) object;
			for (Map.Entry<?, ?> entry : id.entrySet()) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (!Util.nullSafeEquals(value, map.get(key))) {
					return false;
				}
			}

			return true;
		}
	}

	class IncrementalListIterator implements ListIterator<E> {

		// by virtue of get(index)'s implementation, resolution of ids into
		// objects will occur on pageSize boundaries as necessary.

		int listIndex;

		public IncrementalListIterator(int startIndex) {
			this.listIndex = startIndex;
		}

		public void add(Object o) {
			throw new UnsupportedOperationException("add operation not supported");
		}

		public boolean hasNext() {
			return (listIndex < elements.size());
		}

		public boolean hasPrevious() {
			return (listIndex > 0);
		}

		public E next() {
			if (listIndex >= elements.size())
				throw new NoSuchElementException("at the end of the list");

			return get(listIndex++);
		}

		public int nextIndex() {
			return listIndex;
		}

		public E previous() {
			if (listIndex < 1)
				throw new NoSuchElementException("at the beginning of the list");

			return get(--listIndex);
		}

		public int previousIndex() {
			return (listIndex - 1);
		}

		public void remove() {
			throw new UnsupportedOperationException("remove operation not supported");
		}

		public void set(Object o) {
			IncrementalFaultList.this.set(listIndex - 1, o);
		}
	}
}
