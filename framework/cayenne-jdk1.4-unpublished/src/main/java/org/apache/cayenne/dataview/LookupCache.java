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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataObject;

/**
 * @since 1.1
 * @author Andriy Shapochka
 */
public class LookupCache {

    private Map fieldCache = new HashMap();
    private static final Object[] EMPTY_ARRAY = new Object[] {};

    public LookupCache() {
    }

    public void cache(ObjEntityViewField field, List dataObjects) {
        Lookup lookup = getLookup(field);
        if (lookup == null) {
            lookup = new Lookup();
            fieldCache.put(field, lookup);
        }
        lookup.cache(field, dataObjects);
    }

    public void clear() {
        fieldCache.clear();
    }

    public boolean removeFromCache(ObjEntityViewField field) {
        return fieldCache.remove(field) != null;
    }

    public Object[] getCachedValues(ObjEntityViewField field) {
        Lookup lookup = getLookup(field);
        Object[] values = (lookup != null ? lookup.values : EMPTY_ARRAY);
        if (values.length == 0)
            return values;
        else {
            Object[] valuesCopy = new Object[values.length];
            System.arraycopy(values, 0, valuesCopy, 0, values.length);
            return valuesCopy;
        }
    }

    public DataObject getDataObject(ObjEntityViewField field, Object value) {
        Lookup lookup = getLookup(field);
        if (lookup == null)
            return null;
        return lookup.getDataObject(value);
    }

    private Lookup getLookup(ObjEntityViewField field) {
        if (field == null)
            return null;
        return (Lookup) fieldCache.get(field);
    }

    private class Lookup {

        ObjEntityViewField field;
        Object[] values = EMPTY_ARRAY;
        Map valueDataObjectMap;

        void cache(ObjEntityViewField field, List dataObjects) {
            this.field = field;
            if (values.length != dataObjects.size())
                values = new Object[dataObjects.size()];
            valueDataObjectMap = new HashMap(values.length + 1);
            int index = 0;
            for (Iterator i = dataObjects.iterator(); i.hasNext();) {
                DataObject item = (DataObject) i.next();
                values[index] = field.getValue(item);
                valueDataObjectMap.put(values[index], item);
                index++;
            }
        }

        DataObject getDataObject(Object value) {
            return (DataObject) valueDataObjectMap.get(value);
        }
    }
}
