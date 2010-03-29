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

package org.apache.cayenne.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.collections.FastTreeMap;

/**
 * A <code>CayenneMap</code> is a specialized double-linked sorted map class. Attempts
 * to add objects using an already existing keys will result in IllegalArgumentExceptions.
 * Any added entries that implement CayenneMapEntry interface will have their parent set
 * to the parent of this map.
 * <p>
 * CayenneMap is not subclassed directly, but is rather used as an instance variable
 * within another class. Enclosing instance would set itself as a parent of this map.
 * </p>
 * 
 * @deprecated since 3.0 this map is not used by Cayenne internally.
 */
// WARNING: CayenneMap is not serializable via Hessian serialization mechanism used by
// CayenneConnector implementation.
// TODO: deprecate this ugly map. it is only used in Configuration
public class CayenneMap extends FastTreeMap {

    protected Object parent;

    /**
     * Constructor for CayenneMap.
     */
    public CayenneMap(Object parent) {
        this.parent = parent;
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param c
     */
    public CayenneMap(Object parent, Comparator c) {
        super(c);
        this.parent = parent;
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param m
     */
    public CayenneMap(Object parent, Map m) {
        // !IMPORTANT - set parent before populating the map
        this.parent = parent;
        putAll(m);
    }

    /**
     * Constructor for CayenneMap.
     * 
     * @param m
     */
    public CayenneMap(Object parent, SortedMap m) {
        // !IMPORTANT - set parent before populating the map
        this.parent = parent;
        putAll(m);
    }

    /**
     * Maps specified key-value pair. If value is a CayenneMapEntry, sets its parent to
     * this map.
     * 
     * @see java.util.Map#put(Object, Object)
     */
    @Override
    public Object put(Object key, Object value) {

        if (containsKey(key) && get(key) != value) {
            // build descriptive failure message
            StringBuilder message = new StringBuilder();
            message.append("Attempt to insert duplicate key. [key '");
            message.append(key);
            message.append("'");

            if (parent instanceof CayenneMapEntry) {
                message
                        .append(", parent '")
                        .append(((CayenneMapEntry) parent).getName())
                        .append("'");
            }

            if (value instanceof CayenneMapEntry) {
                message
                        .append(", child '")
                        .append(((CayenneMapEntry) value).getName())
                        .append("'");
            }
            message.append("]");

            throw new IllegalArgumentException(message.toString());
        }

        if (value instanceof CayenneMapEntry) {
            ((CayenneMapEntry) value).setParent(parent);
        }

        super.put(key, value);
        return null;
    }

    /**
     * @see java.util.Map#putAll(Map)
     */
    @Override
    public void putAll(Map t) {
        Iterator it = t.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the parent.
     * 
     * @return Object
     */
    public Object getParent() {
        return parent;
    }

    public void setParent(Object mapParent) {
        this.parent = mapParent;
    }
}
