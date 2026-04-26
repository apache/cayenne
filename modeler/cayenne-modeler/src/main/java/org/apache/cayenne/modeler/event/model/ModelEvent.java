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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.util.Util;

import java.util.EventObject;

/**
 * Superclass of project model change events.
 */
public abstract class ModelEvent extends EventObject {

    /**
     * Categorizes a model change. Set once at construction time via the
     * {@code ofAdd / ofChange / ofRemove} factory methods on each subclass.
     */
    public enum Type {
        CHANGE, ADD, REMOVE
    }

    private final Type type;
    private final String oldName;

    protected ModelEvent(Object source, Type type, String oldName) {
        super(source);
        if (type == null) {
            throw new NullPointerException("Null event type");
        }
        this.type = type;
        this.oldName = oldName;
    }

    public Type getType() {
        return type;
    }

    /**
     * @return the previous name when this event records a rename, or {@code null} otherwise.
     */
    public String getOldName() {
        return oldName;
    }

    public boolean isNameChange() {
        return oldName != null && !Util.nullSafeEquals(oldName, getNewName());
    }

    /**
     * Returns the new name of the object that caused this event.
     */
    public abstract String getNewName();
}
