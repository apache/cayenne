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

package org.apache.cayenne.map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Superclass of DataMap objects. Provides a default implementation of CayenneMapEntry
 * needed to implement a doubly linked maps.
 * 
 * @author Andrei Adamchik
 * @deprecated Since 1.2 this class is unused.
 */
public abstract class MapObject implements CayenneMapEntry, XMLSerializable {

    protected String name;
    protected Object parent;

    /**
     * Creates an unnamed MapObject.
     */
    public MapObject() {
    }

    public MapObject(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public Object getParent() {
        return parent;
    }

    public void setParent(Object parent) {
        this.parent = parent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }
}
