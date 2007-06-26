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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;

/**
 * A synchronized list that serves as a container of DataObjects. It is returned when a
 * paged query is performed by DataContext. On creation, only the first "page" is fully
 * resolved, for the rest of the objects only their ObjectIds are read. Pages following
 * the first page are resolved on demand only. On access to an element, the list would
 * ensure that this element as well as all its siblings on the same page are fully
 * resolved.
 * <p>
 * The list can hold DataRows or DataObjects. Attempts to add any other object types will
 * result in an exception.
 * </p>
 * <p>
 * Performance note: certain operations like <code>toArray</code> would trigger full
 * list fetch.
 * </p>
 * 
 * @author Andrus Adamchik
 */
public class IncrementalFaultList implements List {

    protected int pageSize;
    protected List elements;
    protected DataContext dataContext;
    protected ObjEntity rootEntity;
    protected SelectQuery internalQuery;
    protected int unfetchedObjects;

    /**
     * Stores a hint allowing to distinguish data rows from unfetched ids when the query
     * fetches data rows.
     */
    protected int rowWidth;

    private IncrementalListHelper helper;

    /**
     * Defines the upper limit on the size of fetches. This is needed to avoid where
     * clause size limitations.
     */
    protected int maxFetchSize = 10000;

    // Don't confuse this with the JDBC ResultSet fetch size setting - this controls
    // the where clause generation that is necessary to fetch specific records a page
    // at a time. Some JDBC Drivers/Databases may have limits on statement length
    // or complexity of the where clause - e.g., PostgreSQL having a default limit of
    // 10,000 nested expressions.

    /**
     * Creates a new list copying settings from another list. Elements WILL NOT be copied
     * or fetched.
     */
    public IncrementalFaultList(IncrementalFaultList list) {
        this.pageSize = list.pageSize;
        this.internalQuery = list.internalQuery;
        this.dataContext = list.dataContext;
        this.rootEntity = list.rootEntity;
        this.maxFetchSize = list.maxFetchSize;
        this.rowWidth = list.rowWidth;
        this.helper = list.helper;
        elements = Collections.synchronizedList(new ArrayList());
    }

    /**
     * Creates a new IncrementalFaultList using a given DataContext and query.
     * 
     * @param dataContext DataContext used by IncrementalFaultList to fill itself with
     *            objects.
     * @param query Main query used to retrieve data. Must have "pageSize" property set to
     *            a value greater than zero.
     */
    public IncrementalFaultList(DataContext dataContext, Query query) {
        QueryMetadata metadata = query.getMetaData(dataContext.getEntityResolver());
        if (metadata.getPageSize() <= 0) {
            throw new CayenneRuntimeException(
                    "IncrementalFaultList does not support unpaged queries. Query page size is "
                            + metadata.getPageSize());
        }

        this.dataContext = dataContext;
        this.pageSize = metadata.getPageSize();
        this.rootEntity = metadata.getObjEntity();

        // create an internal query, it is a partial replica of
        // the original query and will serve as a value holder for
        // various parameters
        this.internalQuery = new SelectQuery(rootEntity);
        this.internalQuery.setFetchingDataRows(metadata.isFetchingDataRows());
        this.internalQuery.setResolvingInherited(metadata.isResolvingInherited());

        if (metadata.isFetchingDataRows()) {
            helper = new DataRowListHelper();
        }
        else {
            helper = new PersistentListHelper();
        }

        boolean resolvesFirstPage = true;

        if (!metadata.isFetchingDataRows() && (query instanceof SelectQuery)) {
            SelectQuery select = (SelectQuery) query;

            this.internalQuery.setPrefetchTree(select.getPrefetchTree());

            // optimize SelectQuery:
            // * just select ID columns - this gives a 5-10x speedup
            // * strip prefetches as they blow the iterated result, and are actually not
            // needed

            SelectQuery clone = select.queryWithParameters(Collections.EMPTY_MAP, true);
            clone.clearPrefetches();

            // I guess this check is redundant, as custom attributes warrant data rows
            if (!select.isFetchingCustomAttributes()) {
                Iterator pk = rootEntity.getDbEntity().getPrimaryKey().iterator();
                while (pk.hasNext()) {
                    DbAttribute attribute = (DbAttribute) pk.next();
                    clone.addCustomDbAttribute(attribute.getName());
                }
            }

            query = clone;
            resolvesFirstPage = false;
        }

        List elementsUnsynced = new ArrayList();
        fillIn(query, elementsUnsynced, resolvesFirstPage);
        this.elements = Collections.synchronizedList(elementsUnsynced);
    }

    /**
     * @since 1.2
     */
    SelectQuery getInternalQuery() {
        return internalQuery;
    }

    /**
     * Performs initialization of the internal list of objects. Only the first page is
     * fully resolved. For the rest of the list, only ObjectIds are read.
     * 
     * @deprecated since 3.0 this method is not called and is deprecated in favor of
     *             {@link #fillIn(Query, List)}, as this method performed unneeded
     *             synchronization.
     * @since 1.0.6
     */
    protected void fillIn(Query query) {
        synchronized (elements) {
            fillIn(query, elements, true);
        }
    }

    /**
     * Performs initialization of the list of objects. Only the first page is fully
     * resolved. For the rest of the list, only ObjectIds are read.
     * 
     * @since 3.0
     */
    protected void fillIn(Query query, List elementsList, boolean resolvesFirstPage) {
        QueryMetadata info = query.getMetaData(dataContext.getEntityResolver());
        boolean fetchesDataRows = internalQuery.isFetchingDataRows();

        // start fresh
        elementsList.clear();
        rowWidth = 0;

        try {
            int lastResolved = 0;
            long t1 = System.currentTimeMillis();
            ResultIterator it = dataContext.performIteratedQuery(query);
            try {

                rowWidth = it.getDataRowWidth();

                // resolve first page if we can
                if (resolvesFirstPage) {
                    // read first page completely, the rest as ObjectIds
                    for (int i = 0; i < pageSize && it.hasNextRow(); i++) {
                        elementsList.add(it.nextDataRow());
                        lastResolved++;
                    }

                    // defer DataRows -> Objects conversion till we are completely done.
                }

                // continue reading ids
                DbEntity entity = rootEntity.getDbEntity();
                while (it.hasNextRow()) {
                    elementsList.add(it.nextObjectId(entity));
                }

                QueryLogger.logSelectCount(elementsList.size(), System
                        .currentTimeMillis()
                        - t1);
            }
            finally {
                it.close();
            }

            // fill in the first page AFTER the iterator was closed, otherwise we may
            // cause an (unobvious) deadlock due to connection pool exhaustion
            if (!fetchesDataRows && lastResolved > 0) {
                List objects = dataContext.objectsFromDataRows(rootEntity, elementsList
                        .subList(0, lastResolved), info.isRefreshingObjects(), info
                        .isResolvingInherited());

                for (int i = 0; i < lastResolved; i++) {
                    elementsList.set(i, objects.get(i));
                }
            }
        }
        catch (CayenneException e) {
            throw new CayenneRuntimeException("Error performing query.", Util
                    .unwindException(e));
        }

        unfetchedObjects = (resolvesFirstPage)
                ? elementsList.size() - pageSize
                : elementsList.size();
    }

    /**
     * Will resolve all unread objects.
     */
    public void resolveAll() {
        resolveInterval(0, size());
    }

    /**
     * @param object
     * @return <code>true</code> if the object corresponds to an unresolved state and
     *         doesn require a fetch before being returned to the user.
     */
    private boolean isUnresolved(Object object) {
        if (object instanceof Persistent) {
            return false;
        }

        if (internalQuery.isFetchingDataRows()) {
            // both unresolved and resolved objects are represented
            // as Maps, so no instanceof check is possible.
            Map map = (Map) object;
            int size = map.size();
            return size < rowWidth;
        }

        return true;
    }

    /**
     * Checks that an object is of the same type as the rest of objects (DataObject or
     * DataRows depending on the query type).
     */
    private void validateListObject(Object object) throws IllegalArgumentException {

        // I am not sure if such a check makes sense???

        if (internalQuery.isFetchingDataRows()) {
            if (!(object instanceof Map)) {
                throw new IllegalArgumentException(
                        "Only Map objects can be stored in this list.");
            }
        }
        else {
            if (!(object instanceof Persistent)) {
                throw new IllegalArgumentException(
                        "Only DataObjects can be stored in this list.");
            }
        }
    }

    /**
     * Resolves a sublist of objects starting at <code>fromIndex</code> up to but not
     * including <code>toIndex</code>. Internally performs bound checking and trims
     * indexes accordingly.
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

            List quals = new ArrayList(pageSize);
            List ids = new ArrayList(pageSize);
            for (int i = fromIndex; i < toIndex; i++) {
                Object obj = elements.get(i);
                if (isUnresolved(obj)) {
                    ids.add(obj);

                    Map map = (Map) obj;
                    if (map.isEmpty()) {
                        throw new CayenneRuntimeException("Empty id map at index " + i);
                    }

                    quals.add(ExpressionFactory.matchAllDbExp(map, Expression.EQUAL_TO));
                }
            }

            int qualsSize = quals.size();
            if (qualsSize == 0) {
                return;
            }

            // fetch the range of objects in fetchSize chunks
            boolean fetchesDataRows = internalQuery.isFetchingDataRows();
            List objects = new ArrayList(qualsSize);
            int fetchEnd = Math.min(qualsSize, maxFetchSize);
            int fetchBegin = 0;
            while (fetchBegin < qualsSize) {
                SelectQuery query = new SelectQuery(rootEntity, ExpressionFactory
                        .joinExp(Expression.OR, quals.subList(fetchBegin, fetchEnd)));

                query.setFetchingDataRows(fetchesDataRows);

                if (!query.isFetchingDataRows()) {
                    query.setPrefetchTree(internalQuery.getPrefetchTree());
                }

                objects.addAll(dataContext.performQuery(query));
                fetchBegin = fetchEnd;
                fetchEnd += Math.min(maxFetchSize, qualsSize - fetchEnd);
            }

            // sanity check - database data may have changed
            if (objects.size() < ids.size()) {
                // find missing ids
                StringBuffer buf = new StringBuffer();
                buf.append("Some ObjectIds are missing from the database. ");
                buf.append("Expected ").append(ids.size()).append(", fetched ").append(
                        objects.size());

                Iterator idsIt = ids.iterator();
                boolean first = true;
                while (idsIt.hasNext()) {
                    boolean found = false;
                    Object id = idsIt.next();
                    Iterator oIt = objects.iterator();
                    while (oIt.hasNext()) {
                        if (((Persistent) oIt.next())
                                .getObjectId()
                                .getIdSnapshot()
                                .equals(id)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        if (first) {
                            first = false;
                        }
                        else {
                            buf.append(", ");
                        }

                        buf.append(id.toString());
                    }
                }

                throw new CayenneRuntimeException(buf.toString());
            }
            else if (objects.size() > ids.size()) {
                throw new CayenneRuntimeException("Expected "
                        + ids.size()
                        + " objects, retrieved "
                        + objects.size());
            }

            // replace ids in the list with objects
            Iterator it = objects.iterator();
            while (it.hasNext()) {
                helper.updateWithResolvedObjectInRange(it.next(), fromIndex, toIndex);
            }

            unfetchedObjects -= objects.size();
        }
    }

    /**
     * Returns zero-based index of the virtual "page" for a given array element index.
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
     * Get the upper bound on the number of records to resolve in one round trip to the
     * database. This setting governs the size/complexity of the where clause generated to
     * retrieve the next page of records. If the fetch size is less than the page size,
     * then multiple fetches will be made to resolve a page.
     * 
     * @return int
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
     * Returns a list iterator for this list. DataObjects are resolved a page (according
     * to getPageSize()) at a time as necessary - when retrieved with next() or
     * previous().
     */
    public ListIterator listIterator() {
        return new IncrementalListIterator(0);
    }

    /**
     * Returns a list iterator of the elements in this list (in proper sequence), starting
     * at the specified position in this list. The specified index indicates the first
     * element that would be returned by an initial call to the next method. An initial
     * call to the previous method would return the element with the specified index minus
     * one. DataObjects are resolved a page at a time (according to getPageSize()) as
     * necessary - when retrieved with next() or previous().
     */
    public ListIterator listIterator(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        return new IncrementalListIterator(index);
    }

    /**
     * Return an iterator for this list. DataObjects are resolved a page (according to
     * getPageSize()) at a time as necessary - when retrieved with next().
     */
    public Iterator iterator() {
        // by virtue of get(index)'s implementation, resolution of ids into
        // objects will occur on pageSize boundaries as necessary.
        return new Iterator() {

            int listIndex = 0;

            public boolean hasNext() {
                return (listIndex < elements.size());
            }

            public Object next() {
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
    public boolean addAll(Collection c) {
        synchronized (elements) {
            return elements.addAll(c);
        }
    }

    /**
     * @see java.util.List#addAll(int, Collection)
     */
    public boolean addAll(int index, Collection c) {
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
    public boolean containsAll(Collection c) {
        synchronized (elements) {
            return elements.containsAll(c);
        }
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        synchronized (elements) {
            Object o = elements.get(index);

            if (isUnresolved(o)) {
                // read this page
                int pageStart = pageIndex(index) * pageSize;
                resolveInterval(pageStart, pageStart + pageSize);

                return elements.get(index);
            }
            else {
                return o;
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

    /**
     * @see java.util.List#lastIndexOf(Object)
     */
    public int lastIndexOf(Object o) {
        return helper.lastIndexOfObject(o);
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int index) {
        synchronized (elements) {
            return elements.remove(index);
        }
    }

    /**
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object o) {
        synchronized (elements) {
            return elements.remove(o);
        }
    }

    /**
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c) {
        synchronized (elements) {
            return elements.removeAll(c);
        }
    }

    /**
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c) {
        synchronized (elements) {
            return elements.retainAll(c);
        }
    }

    /**
     * @see java.util.List#set(int, Object)
     */
    public Object set(int index, Object element) {
        validateListObject(element);

        synchronized (elements) {
            return elements.set(index, element);
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

    public List subList(int fromIndex, int toIndex) {
        synchronized (elements) {
            resolveInterval(fromIndex, toIndex);
            return elements.subList(fromIndex, toIndex);
        }
    }

    public Object[] toArray() {
        resolveAll();

        return elements.toArray();
    }

    /**
     * @see java.util.Collection#toArray(Object[])
     */
    public Object[] toArray(Object[] a) {
        resolveAll();

        return elements.toArray(a);
    }

    /**
     * Returns a total number of objects that are not resolved yet.
     */
    public int getUnfetchedObjects() {
        return unfetchedObjects;
    }

    abstract class IncrementalListHelper {

        int indexOfObject(Object object) {
            if (incorrectObjectType(object)) {
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
            if (incorrectObjectType(object)) {
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

        abstract boolean incorrectObjectType(Object object);

        abstract boolean objectsAreEqual(Object object, Object objectInTheList);

        abstract boolean replacesObject(Object object, Object objectInTheList);
    }

    class PersistentListHelper extends IncrementalListHelper {

        boolean incorrectObjectType(Object object) {
            if (!(object instanceof Persistent)) {
                return true;
            }

            Persistent dataObj = (Persistent) object;
            if (dataObj.getObjectContext() != dataContext) {
                return true;
            }

            if (!dataObj.getObjectId().getEntityName().equals(rootEntity.getName())) {
                return true;
            }

            return false;
        }

        boolean objectsAreEqual(Object object, Object objectInTheList) {

            if (objectInTheList instanceof Persistent) {
                // due to object uniquing this should be sufficient
                return object == objectInTheList;
            }
            else {
                return ((Persistent) object).getObjectId().getIdSnapshot().equals(
                        objectInTheList);
            }
        }

        boolean replacesObject(Object object, Object objectInTheList) {
            if (objectInTheList instanceof Persistent) {
                return false;
            }

            Persistent dataObject = (Persistent) object;
            return dataObject.getObjectId().getIdSnapshot().equals(objectInTheList);
        }
    }

    class DataRowListHelper extends IncrementalListHelper {

        boolean incorrectObjectType(Object object) {
            if (!(object instanceof Map)) {
                return true;
            }

            Map map = (Map) object;
            return map.size() != rowWidth;
        }

        boolean objectsAreEqual(Object object, Object objectInTheList) {
            if (object == null && objectInTheList == null) {
                return true;
            }

            if (object != null && objectInTheList != null) {

                Map id = (Map) objectInTheList;
                Map map = (Map) object;

                // id must be a subset of this map
                Iterator it = id.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
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

        boolean replacesObject(Object object, Object objectInTheList) {

            Map id = (Map) objectInTheList;
            if (id.size() == rowWidth) {
                return false;
            }

            // id must be a subset of this map
            Map map = (Map) object;
            Iterator it = id.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (!Util.nullSafeEquals(value, map.get(key))) {
                    return false;
                }
            }

            return true;
        }
    }

    class IncrementalListIterator implements ListIterator {

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

        public Object next() {
            if (listIndex >= elements.size())
                throw new NoSuchElementException("at the end of the list");

            return get(listIndex++);
        }

        public int nextIndex() {
            return listIndex;
        }

        public Object previous() {
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
            throw new UnsupportedOperationException("set operation not supported");
        }
    }
}
