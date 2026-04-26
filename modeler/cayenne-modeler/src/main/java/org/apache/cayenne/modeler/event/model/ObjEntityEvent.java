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

import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.map.ObjEntity;

/**
 * Represents events resulted from ObjEntity changes in CayenneModeler.
 */
public class ObjEntityEvent extends ModelEvent {

    private final ObjEntity entity;

    public static ObjEntityEvent ofAdd(Object src, ObjEntity entity) {
        return new ObjEntityEvent(src, entity, Type.ADD, null);
    }

    public static ObjEntityEvent ofChange(Object src, ObjEntity entity) {
        return new ObjEntityEvent(src, entity, Type.CHANGE, null);
    }

    public static ObjEntityEvent ofChange(Object src, ObjEntity entity, String oldName) {
        return new ObjEntityEvent(src, entity, Type.CHANGE, oldName);
    }

    public static ObjEntityEvent ofRemove(Object src, ObjEntity entity) {
        return new ObjEntityEvent(src, entity, Type.REMOVE, null);
    }

    private ObjEntityEvent(Object src, ObjEntity entity, Type type, String oldName) {
        super(src, type, oldName);
        this.entity = entity;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    @Override
    public String getNewName() {
        return (entity != null) ? entity.getName() : null;
    }
}
