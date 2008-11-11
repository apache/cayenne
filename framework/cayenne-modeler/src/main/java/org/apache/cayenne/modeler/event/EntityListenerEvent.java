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
package org.apache.cayenne.modeler.event;

import org.apache.cayenne.map.event.MapEvent;

/**
 * Event for creating/removing/modifying entity listeners
 *
 * @version 1.0 Oct 25, 2007
 */
public class EntityListenerEvent extends MapEvent{
    /**
     * new entity listener class name
     */
    private String newName;

    /**
     * constructor
     * @param source event source
     * @param oldName old entity listener class name
     * @param newName new entity listener class name
     * @param eventId event type id
     */
    public EntityListenerEvent(Object source, String oldName, String newName, int eventId) {
        super(source, oldName);
        this.newName = newName;
        setId(eventId);
    }

    /**
     * @return new entity listener class name
     */
    public String getNewName() {
        return newName;
    }
}

