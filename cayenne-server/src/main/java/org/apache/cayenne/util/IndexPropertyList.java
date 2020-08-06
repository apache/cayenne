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

package org.apache.cayenne.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.PropertyUtils;

/**
 * A List implementation that would maintain its internal ordering based on some object
 * numeric "index" property. When objects are added to the list at a certain index, an
 * "index" property is modified to reflect list order, when objects are removed, their
 * index property is set to the negative number.
 * <p>
 * For performance reasons this implementation does not guarantee that there is no gaps in
 * the integer ordering sequence (i.e. generally
 * <code>object.getIndexProperty() != list.indexOf(object)</code>). However it
 * guarantees the right ordering based on index property.
 * 
 * @since 1.2
 */
public class IndexPropertyList extends AbstractList implements ValueHolder {

    /**
     * A default gap maintained between elements index property values. Gaps bigger than 1
     * ensure faster and less intrusive additions and removals.
     */
    static final int DEFAULT_GAP = 3;

    /**
     * A list used for the actual objects storage.
     */
    protected List list;
    protected String indexProperty;

    boolean dirty;
    Comparator comparator;

    /**
     * Creates an empty NumericPropertyOrderedList.
     */
    public IndexPropertyList(String indexProperty) {
        this(indexProperty, new ArrayList(), false);
    }

    /**
     * Creates a NumericPropertyOrderedList that decorates another list. If the list is
     * not known to be properly sorted, caller must set <em>sortNeeded</em> to true.
     * This will result in sorting the original list on first access attempt.
     */
    public IndexPropertyList(String indexProperty, List objects, boolean sortNeeded) {

        if (indexProperty == null) {
            throw new IllegalArgumentException("Null sortProperty");
        }

        if (objects == null) {
            throw new IllegalArgumentException("Null objects list.");
        }

        this.indexProperty = indexProperty;
        this.list = objects;

        // be lazy - don't sort here, as (a) it may never be needed and (b) a list can be
        // a Cayenne fault, so resolving it too early is undesirable
        this.dirty = sortNeeded;
    }

    ValueHolder getWrappedValueHolder() {
        return (list instanceof ValueHolder) ? (ValueHolder) list : null;
    }

    public boolean isFault() {
        ValueHolder h = getWrappedValueHolder();
        return (h != null) ? h.isFault() : false;
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {
        ValueHolder h = getWrappedValueHolder();
        return h != null ? h.setValueDirectly(value) : null;
    }

    public Object setValue(Object value) throws CayenneRuntimeException {
        ValueHolder h = getWrappedValueHolder();
        return h != null ? h.setValue(value) : null;
    }

    public Object getValue() throws CayenneRuntimeException {
        ValueHolder h = getWrappedValueHolder();
        return h != null ? h.getValue() : null;
    }
    
    public Object getValueDirectly() throws CayenneRuntimeException {
        ValueHolder h = getWrappedValueHolder();
        return h != null ? h.getValueDirectly() : null;
    }

    public void invalidate() {
        ValueHolder h = getWrappedValueHolder();
        if (h != null) {
            h.invalidate();
        }
    }

    /**
     * Changes list state to "dirty" forcing reordering on next access.
     */
    public void touch() {
        this.dirty = true;
    }

    @Override
    public Object get(int index) {
        if (dirty) {
            sort();
        }

        return list.get(index);
    }

    @Override
    public int size() {
        if (dirty) {
            sort();
        }

        return list.size();
    }

    @Override
    public Object set(int index, Object element) {
        if (dirty) {
            sort();
        }

        Object removed = list.set(index, element);

        if (element != null) {
            int indexValue = (removed != null)
                    ? getIndexValue(removed)
                    : calculateIndexValue(index);

            setIndexValue(element, indexValue);
            shift(index + 1, indexValue);
        }

        if (removed != null && removed != element) {
            setIndexValue(removed, -1);
        }

        return removed;
    }

    @Override
    public void add(int index, Object element) {
        if (dirty) {
            sort();
        }

        list.add(index, element);

        if (element != null) {
            int indexValue = calculateIndexValue(index);

            setIndexValue(element, indexValue);
            shift(index + 1, indexValue);
        }
    }

    @Override
    public Object remove(int index) {
        if (dirty) {
            sort();
        }

        Object removed = list.remove(index);

        if (removed != null) {
            setIndexValue(removed, -1);
        }

        return removed;
    }

    // ============================================
    // ***** Methods to maintain ordering ******
    // ============================================

    /**
     * Calculates an index value at the specified list index. Note that using this value
     * may require a shift of the objects following this index.
     */
    protected int calculateIndexValue(int listIndex) {
        if (list.isEmpty()) {
            throw new ArrayIndexOutOfBoundsException(listIndex);
        }

        if (list.size() == 1 && listIndex == 0) {
            return 0;
        }

        // handle lists with teo or more elements...

        // last element
        if (listIndex == list.size() - 1) {
            return getIndexValue(get(listIndex - 1)) + DEFAULT_GAP;
        }

        int from = (listIndex == 0) ? -1 : getIndexValue(get(listIndex - 1));
        int to = getIndexValue(get(listIndex + 1));
        return (to - from > 1) ? (to - from) / 2 + from : from + DEFAULT_GAP;
    }

    protected int getIndexValue(Object object) {
        Number n = (Number) PropertyUtils.getProperty(object, indexProperty);
        if (n == null) {
            throw new CayenneRuntimeException("Null index property '%s' for object %s"
                    , indexProperty, object);
        }

        return n.intValue();
    }

    protected void setIndexValue(Object object, int index) {
        PropertyUtils.setProperty(object, indexProperty, Integer.valueOf(index));
    }

    protected void shift(int startIndex, int afterIndexValue) {
        int size = size();
        for (int i = startIndex; i < size; i++) {
            Object object = get(i);

            int indexValue = getIndexValue(object);
            if (indexValue > afterIndexValue) {
                break;
            }

            int newValue = calculateIndexValue(i);
            setIndexValue(object, newValue);
            afterIndexValue = newValue;
        }
    }

    /**
     * Sorts internal list.
     */
    protected void sort() {
        if (!dirty) {
            return;
        }

        // do not directly sort Cayenne lists, sort the underlying list instead to avoid a
        // bunch of additions/removals
        Collections.sort(unwrapList(), getComparator());
        dirty = false;
    }

    List unwrapList() {
        if (list instanceof PersistentObjectList) {
            return ((PersistentObjectList) list).resolvedObjectList();
        }
        else {
            return list;
        }
    }

    /**
     * Returns a property Comaparator, creating it on demand.
     */
    Comparator getComparator() {
        if (comparator == null) {
            comparator = new Comparator() {

                public int compare(Object o1, Object o2) {
                    if ((o1 == null && o2 == null) || o1 == o2) {
                        return 0;
                    }
                    else if (o1 == null) {
                        return -1;
                    }
                    else if (o2 == null) {
                        return 1;
                    }

                    Comparable p1 = (Comparable) PropertyUtils.getProperty(
                            o1,
                            indexProperty);
                    Comparable p2 = (Comparable) PropertyUtils.getProperty(
                            o2,
                            indexProperty);
                    return (p1 == null) ? -1 : p1.compareTo(p2);
                }
            };
        }
        return comparator;
    }
}
