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

package org.apache.cayenne.map.event;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.event.CayenneEvent;
import org.apache.cayenne.util.Util;

/**
 * Superclass of CayenneModeler events.
 * 
 */
public abstract class MapEvent extends CayenneEvent {

    /**
     * A type that describes object modification events. CHANGE is a default type of new
     * MapEvents, unless the type is specified explicitly.
     */
    public static final int CHANGE = 1;

    /**
     * A type that describes object creation events.
     */
    public static final int ADD = 2;

    /**
     * A type that describes object removal events.
     */
    public static final int REMOVE = 3;

    protected int id = CHANGE;
    protected String oldName;
    protected boolean oldNameSet;
    
    /**
     * Domain of event object. Might be null
     */
    protected DataChannelDescriptor domain;

    /**
     * Constructor for MapEvent.
     * 
     * @param source event source
     */
    public MapEvent(Object source) {
        super(source);
    }

    /**
     * Constructor for MapEvent.
     * 
     * @param source event source
     */
    public MapEvent(Object source, String oldName) {
        super(source);
        setOldName(oldName);
    }

    public boolean isNameChange() {
        return oldNameSet && !Util.nullSafeEquals(getOldName(), getNewName());
    }

    /**
     * Returns the id.
     * 
     * @return int
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the newName of the object that caused this event.
     */
    public abstract String getNewName();

    /**
     * Returns the oldName.
     */
    public String getOldName() {
        return oldName;
    }

    /**
     * Sets the id.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Sets the oldName.
     */
    public void setOldName(String oldName) {
        this.oldName = oldName;
        this.oldNameSet = true;
    }
    
    /**
     * Sets domain of event object.
     */
    public void setDomain(DataChannelDescriptor domain) {
        this.domain = domain;
    }
    
    /**
     * @return Domain of event object. Might be null
     */
    public DataChannelDescriptor getDomain() {
        return domain;
    }
}
