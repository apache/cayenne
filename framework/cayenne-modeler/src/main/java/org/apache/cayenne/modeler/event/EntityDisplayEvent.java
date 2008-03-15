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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;

/**
 * Represents a display event of an Entity.
 */
public class EntityDisplayEvent extends DataMapDisplayEvent {

    protected Entity entity;

    /**
     * If true, the event causes entity editor to switch to the main entity tab.
     */
    protected boolean mainTabFocus;

    /**
     * True if different from current entity.
     */
    protected boolean entityChanged = true;
    protected boolean unselectAttributes;

    public EntityDisplayEvent(Object src, Entity entity) {
        this(src, entity, null, null, null);
    }

    public EntityDisplayEvent(Object src, Entity entity, DataMap map, DataDomain domain) {

        this(src, entity, map, null, domain);
    }

    public EntityDisplayEvent(Object src, Entity entity, DataMap map, DataNode node,
            DataDomain domain) {

        super(src, map, domain, node);
        this.entity = entity;
        setDataMapChanged(false);
    }

    /**
     * Returns entity associated with this event.
     */
    public Entity getEntity() {
        return entity;
    }

    /** True if entity different from current entity. */
    public boolean isEntityChanged() {
        return entityChanged;
    }

    public void setEntityChanged(boolean temp) {
        entityChanged = temp;
    }

    public boolean isUnselectAttributes() {
        return unselectAttributes;
    }

    public void setUnselectAttributes(boolean unselectAttributes) {
        this.unselectAttributes = unselectAttributes;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void setMainTabFocus(boolean searched) {
        this.mainTabFocus = searched;
    }

    public boolean isMainTabFocus() {
        return mainTabFocus;
    }
}
