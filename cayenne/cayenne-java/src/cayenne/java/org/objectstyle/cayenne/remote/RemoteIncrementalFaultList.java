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
package org.objectstyle.cayenne.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.util.IDUtil;
import org.objectstyle.cayenne.util.IncrementalListResponse;
import org.objectstyle.cayenne.util.Util;

/**
 * A list that serves as a container of Persistent objects. It is usually returned by an
 * ObjectContext when a paginated query is performed. Initially only the first "page" of
 * objects is fully resolved. Pages following the first page are resolved on demand. When
 * a list element is accessed, the list would ensure that this element as well as all its
 * siblings on the same page are fully resolved.
 * <p>
 * The list can hold DataRows or Persistent objects. Attempts to add any other object
 * types will result in an exception.
 * </p>
 * <p>
 * Certain operations like <code>toArray</code> would trigger full list fetch.
 * </p>
 * <p>
 * Synchronization Note: this list is not synchronized. All access to it should follow
 * synchronization rules applicable for ArrayList.
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class RemoteIncrementalFaultList implements List {

    static final Object PLACEHOLDER = new Object();

    protected List elements;

    protected String cacheKey;
    protected int pageSize;
    protected int unfetchedObjects;
    protected QueryMetadata metadata;

    protected transient ObjectContext context;

    /**
     * Stores a hint allowing to distinguish data rows from unfetched ids when the query
     * fetches data rows.
     */
    protected int rowWidth;

    private ListHelper helper;

    public RemoteIncrementalFaultList(ObjectContext context, Query paginatedQuery) {

        this.metadata = paginatedQuery.getMetaData(context.getEntityResolver());

        if (metadata.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be positive: "
                    + metadata.getPageSize());
        }

        this.pageSize = metadata.getPageSize();
        this.helper = (metadata.isFetchingDataRows())
                ? (ListHelper) new DataRowListHelper()
                : new PersistentListHelper();
        this.context = context;

        // use provided cache key if possible; this would allow clients to
        // address the same server-side list from multiple queries.
        this.cacheKey = metadata.getCacheKey();
        if(cacheKey == null) {
            cacheKey = generateCacheKey();
        }

        IncrementalQuery query = new IncrementalQuery(paginatedQuery, cacheKey);

        // select directly from the channel, bypassing the context. Otherwise our query
        // wrapper can be intercepted incorrectly
        QueryResponse response = context.getChannel().onQuery(context, query);

        List firstPage = response.firstList();

        // sanity check
        if (firstPage.size() > pageSize) {
            throw new IllegalArgumentException("Returned page size ("
                    + firstPage.size()
                    + ") exceeds requested page size ("
                    + pageSize
                    + ")");
        }
        // result is smaller than a page
        else if (firstPage.size() < pageSize) {
            this.elements = new ArrayList(firstPage);
            unfetchedObjects = 0;
        }
        else {

            if (response instanceof IncrementalListResponse) {
                int fullListSize = ((IncrementalListResponse) response).getFullSize();

                this.unfetchedObjects = fullListSize - firstPage.size();
                this.elements = new ArrayList(fullListSize);
                elements.addAll(firstPage);

                // fill the rest with placeholder...
                for (int i = pageSize; i < fullListSize; i++) {
                    elements.add(PLACEHOLDER);
                }
            }
            // this happens when full size equals page size
            else {
                this.elements = new ArrayList(firstPage);
                unfetchedObjects = 0;
            }
        }
    }

    private String generateCacheKey() {
        byte[] bytes = IDUtil.pseudoUniqueByteSequence8();
        StringBuffer buffer = new StringBuffer(17);
        buffer.append("I");
        for (int i = 0; i < bytes.length; i++) {
            IDUtil.appendFormattedByte(buffer, bytes[i]);
        }

        return buffer.toString();
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
     *         does require a fetch before being returned to the user.
     */
    private boolean isUnresolved(Object object) {
        return object == PLACEHOLDER;
    }

    /**
     * Resolves a sublist of objects starting at <code>fromIndex</code> up to but not
     * including <code>toIndex</code>. Internally performs bound checking and trims
     * indexes accordingly.
     */
    protected void resolveInterval(int fromIndex, int toIndex) {
        if (fromIndex >= toIndex || elements.isEmpty()) {
            return;
        }

        if (context == null) {
            throw new CayenneRuntimeException(
                    "No ObjectContext set, can't resolve objects.");
        }

        // bounds checking

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        if (toIndex > elements.size()) {
            toIndex = elements.size();
        }

        // find disjoint ranges and resolve them individually...

        int fromPage = pageIndex(fromIndex);
        int toPage = pageIndex(toIndex - 1);

        int rangeStartIndex = -1;
        for (int i = fromPage; i <= toPage; i++) {

            int pageStartIndex = i * pageSize;
            Object firstPageObject = elements.get(pageStartIndex);
            if (isUnresolved(firstPageObject)) {

                // start range
                if (rangeStartIndex < 0) {
                    rangeStartIndex = pageStartIndex;
                }
            }
            else {

                // finish range...
                if (rangeStartIndex >= 0) {
                    forceResolveInterval(rangeStartIndex, pageStartIndex);
                    rangeStartIndex = -1;
                }
            }
        }

        // load last page
        if (rangeStartIndex >= 0) {
            forceResolveInterval(rangeStartIndex, toIndex);
        }
    }

    void forceResolveInterval(int fromIndex, int toIndex) {

        int pastEnd = toIndex - size();
        if (pastEnd > 0) {
            toIndex = size();
        }

        int fetchLimit = toIndex - fromIndex;

        RangeQuery query = new RangeQuery(cacheKey, fromIndex, fetchLimit, metadata);

        List sublist = context.performQuery(query);

        // sanity check
        if (sublist.size() != fetchLimit) {
            throw new CayenneRuntimeException("Resolved range size '"
                    + sublist.size()
                    + "' is not the same as expected: "
                    + fetchLimit);
        }

        for (int i = 0; i < fetchLimit; i++) {
            elements.set(fromIndex + i, sublist.get(i));
        }

        unfetchedObjects -= sublist.size();
    }

    /**
     * Returns zero-based index of the virtual "page" for a given array element index.
     */
    int pageIndex(int elementIndex) {
        if (elementIndex < 0 || elementIndex > size()) {
            throw new IndexOutOfBoundsException("Index: " + elementIndex);
        }

        if (pageSize <= 0 || elementIndex < 0) {
            return -1;
        }

        return elementIndex / pageSize;
    }

    /**
     * Returns ObjectContext associated with this list.
     */
    public ObjectContext getContext() {
        return context;
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
        return new ListIteratorHelper(0);
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

        return new ListIteratorHelper(index);
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
        helper.validateListObject(element);
        elements.add(index, element);

    }

    /**
     * @see java.util.Collection#add(Object)
     */
    public boolean add(Object o) {
        helper.validateListObject(o);
        return elements.add(o);
    }

    /**
     * @see java.util.Collection#addAll(Collection)
     */
    public boolean addAll(Collection c) {

        return elements.addAll(c);

    }

    /**
     * @see java.util.List#addAll(int, Collection)
     */
    public boolean addAll(int index, Collection c) {

        return elements.addAll(index, c);

    }

    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        elements.clear();
    }

    /**
     * @see java.util.Collection#contains(Object)
     */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * @see java.util.Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection c) {
        Iterator it = c.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index) {

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

        return elements.isEmpty();

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

        return elements.remove(index);

    }

    /**
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object o) {

        return elements.remove(o);

    }

    /**
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c) {

        return elements.removeAll(c);

    }

    /**
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c) {

        return elements.retainAll(c);

    }

    /**
     * @see java.util.List#set(int, Object)
     */
    public Object set(int index, Object element) {
        helper.validateListObject(element);

        return elements.set(index, element);

    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        return elements.size();
    }

    public List subList(int fromIndex, int toIndex) {
        resolveInterval(fromIndex, toIndex);
        return elements.subList(fromIndex, toIndex);
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

    abstract class ListHelper {

        int indexOfObject(Object object) {
            if (incorrectObjectType(object)) {
                return -1;
            }

            for (int i = 0; i < elements.size(); i++) {

                if (Util.nullSafeEquals(object, get(i))) {
                    return i;
                }
            }

            return -1;
        }

        int lastIndexOfObject(Object object) {
            if (incorrectObjectType(object)) {
                return -1;
            }

            for (int i = elements.size() - 1; i >= 0; i--) {
                if (Util.nullSafeEquals(object, get(i))) {
                    return i;
                }
            }

            return -1;
        }

        abstract boolean incorrectObjectType(Object object);

        void validateListObject(Object object) throws IllegalArgumentException {
            if (incorrectObjectType(object)) {
                throw new IllegalArgumentException("Can't store this object: " + object);
            }
        }
    }

    class PersistentListHelper extends ListHelper {

        boolean incorrectObjectType(Object object) {
            if (!(object instanceof Persistent)) {
                return true;
            }

            Persistent persistent = (Persistent) object;
            if (persistent.getObjectContext() != context) {
                return true;
            }

            return false;
        }

    }

    class DataRowListHelper extends ListHelper {

        boolean incorrectObjectType(Object object) {
            if (!(object instanceof Map)) {
                return true;
            }

            Map map = (Map) object;
            return map.size() != rowWidth;
        }
    }

    class ListIteratorHelper implements ListIterator {

        // by virtue of get(index)'s implementation, resolution of ids into
        // objects will occur on pageSize boundaries as necessary.

        int listIndex;

        public ListIteratorHelper(int startIndex) {
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
