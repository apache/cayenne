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

package org.apache.cayenne.dataview;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.DataObject;

/**
 * A decorator list for a collection of DataObjects that fires
 * {@link DataObjectChangeEvent}events on modification, and supports registering
 * {@link DataObjectChangeListener DataObjectChangeListeners}to recieve these events.
 * Designed to be used as an active model in Swing applications.
 * 
 * @since 1.1
 * @author Andriy Shapochka
 */
public class DataObjectList extends AbstractList {

    protected List dataObjects;
    protected EventDispatcher changeDispatcher;

    public DataObjectList() {
        dataObjects = new ArrayList();
    }

    public DataObjectList(int capacity) {
        dataObjects = new ArrayList(capacity);
    }

    public DataObjectList(Collection dataObjects, boolean typeCheck) {
        if (typeCheck) {
            for (Iterator i = dataObjects.iterator(); i.hasNext();) {
                if (!(i.next() instanceof DataObject)) {
                    this.dataObjects = new ArrayList(1);
                    return;
                }
            }
        }
        this.dataObjects = new ArrayList(dataObjects);
    }

    public DataObjectList(Collection dataObjects) {
        this(dataObjects, true);
    }

    public void addDataObjectChangeListener(DataObjectChangeListener listener) {
        changeDispatcher = EventDispatcher.add(changeDispatcher, listener);
    }

    public void removeDataObjectChangeListener(DataObjectChangeListener listener) {
        changeDispatcher = EventDispatcher.remove(changeDispatcher, listener);
    }

    public void clearDataObjectChangeListeners() {
        if (changeDispatcher != null) {
            changeDispatcher.clear();
            changeDispatcher = null;
        }
    }

    public int indexOf(Object o) {
        if (!(o instanceof DataObject))
            return -1;
        return dataObjects.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        if (!(o instanceof DataObject))
            return -1;
        return dataObjects.lastIndexOf(o);
    }

    public boolean contains(Object o) {
        return (indexOf(o) >= 0);
    }

    public boolean isEmpty() {
        return dataObjects.isEmpty();
    }

    public int size() {
        return dataObjects.size();
    }

    public boolean add(DataObject dataObject) {
        boolean success = dataObjects.add(dataObject);
        int index = dataObjects.size() - 1;
        if (success && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_ADDED,
                    index));
        return success;
    }

    public void add(int index, DataObject dataObject) {
        dataObjects.add(index, dataObject);
        if (changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_ADDED,
                    index));
    }

    public boolean add(Object o) {
        return add((DataObject) o);
    }

    public void add(int index, Object element) {
        add(index, (DataObject) element);
    }

    public Object remove(int index) {
        Object dataObject = dataObjects.remove(index);
        if (dataObject != null && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_REMOVED,
                    index));
        return dataObject;
    }

    public boolean remove(Object o) {
        int index = indexOf(o);
        return remove(index) != null;
    }

    public DataObject set(int index, DataObject dataObject) {
        DataObject oldObject = (DataObject) dataObjects.set(index, dataObject);
        if (changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_CHANGED,
                    index));
        return oldObject;
    }

    public Object set(int index, Object element) {
        return set(index, (DataObject) element);
    }

    public Object get(int index) {
        return dataObjects.get(index);
    }

    public DataObject getDataObject(int index) {
        return (DataObject) get(index);
    }

    public Object[] toArray(Object[] array) {
        return dataObjects.toArray(array);
    }

    public Object[] toArray() {
        return dataObjects.toArray();
    }

    public void clear() {
        boolean empty = isEmpty();
        dataObjects.clear();
        if (!empty && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_REMOVED));
    }

    public boolean removeAll(Collection dataObjects) {
        boolean success = this.dataObjects.removeAll(dataObjects);
        if (success && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_REMOVED));
        return success;
    }

    public boolean containsAll(Collection dataObjects) {
        return this.dataObjects.containsAll(dataObjects);
    }

    public boolean addAll(Collection dataObjects, boolean typeCheck) {
        if (typeCheck) {
            for (Iterator i = dataObjects.iterator(); i.hasNext();) {
                if (!(i.next() instanceof DataObject))
                    return false;
            }
        }
        boolean success = this.dataObjects.addAll(dataObjects);
        if (success && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_ADDED));
        return success;
    }

    public boolean addAll(int index, Collection dataObjects, boolean typeCheck) {
        if (typeCheck) {
            for (Iterator i = dataObjects.iterator(); i.hasNext();) {
                if (!(i.next() instanceof DataObject))
                    return false;
            }
        }
        boolean success = this.dataObjects.addAll(index, dataObjects);
        if (success && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_ADDED));
        return success;
    }

    public boolean addAll(Collection dataObjects) {
        return addAll(dataObjects, true);
    }

    public boolean addAll(int index, Collection c) {
        return addAll(index, dataObjects, true);
    }

    public boolean retainAll(Collection dataObjects) {
        boolean success = this.dataObjects.retainAll(dataObjects);
        if (success && changeDispatcher != null)
            changeDispatcher.dispatch(new DataObjectChangeEvent(
                    this,
                    DataObjectChangeEvent.DATAOBJECT_REMOVED));
        return success;
    }

}
