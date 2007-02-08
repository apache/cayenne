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
package org.objectstyle.cayenne.dataview;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.DataObject;

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