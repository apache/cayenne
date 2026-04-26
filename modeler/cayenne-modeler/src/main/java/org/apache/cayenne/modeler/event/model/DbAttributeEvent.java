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

import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Represents events resulted from DbAttribute changes in CayenneModeler.
 */
public class DbAttributeEvent extends ModelEvent {

    private final DbAttribute attribute;
    private final DbEntity entity;

    public static DbAttributeEvent ofAdd(Object src, DbAttribute attr, DbEntity entity) {
        return new DbAttributeEvent(src, attr, entity, Type.ADD, null);
    }

    public static DbAttributeEvent ofChange(Object src, DbAttribute attr, DbEntity entity) {
        return new DbAttributeEvent(src, attr, entity, Type.CHANGE, null);
    }

    public static DbAttributeEvent ofChange(Object src, DbAttribute attr, DbEntity entity, String oldName) {
        return new DbAttributeEvent(src, attr, entity, Type.CHANGE, oldName);
    }

    public static DbAttributeEvent ofRemove(Object src, DbAttribute attr, DbEntity entity) {
        return new DbAttributeEvent(src, attr, entity, Type.REMOVE, null);
    }

    private DbAttributeEvent(Object src, DbAttribute attr, DbEntity entity, Type type, String oldName) {
        super(src, type, oldName);
        this.attribute = attr;
        this.entity = entity;
    }

    public DbAttribute getAttribute() {
        return attribute;
    }

    public DbEntity getEntity() {
        return entity;
    }

    @Override
    public String getNewName() {
        return (attribute != null) ? attribute.getName() : null;
    }
}
