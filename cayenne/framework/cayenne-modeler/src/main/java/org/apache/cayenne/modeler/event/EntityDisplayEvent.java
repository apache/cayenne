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
 * 
 * @author Misha Shengaout
 * @author Andrus Adamchik
 */
public class EntityDisplayEvent extends DataMapDisplayEvent {
	protected Entity entity;

    /** True if the event is generated when an entity is beeing searched for */
    protected boolean searched;

	/** True if different from current entity */
	protected boolean entityChanged = true;
	protected boolean unselectAttributes;

	public EntityDisplayEvent(Object src, Entity entity) {
		this(src, entity, null, null, null);
	}

	public EntityDisplayEvent(
		Object src,
		Entity entity,
		DataMap map,
		DataDomain domain) {

		this(src, entity, map, null, domain);
	}

	public EntityDisplayEvent(
		Object src,
		Entity entity,
		DataMap map,
		DataNode node,
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

	/**
	 * Returns the unselectAttributes.
	 * @return boolean
	 */
	public boolean isUnselectAttributes() {
		return unselectAttributes;
	}

	/**
	 * Sets the unselectAttributes.
	 * @param unselectAttributes The unselectAttributes to set
	 */
	public void setUnselectAttributes(boolean unselectAttributes) {
		this.unselectAttributes = unselectAttributes;
	}

	/**
	 * Sets the entity.
	 * @param entity The entity to set
	 */
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

    /**
     * Sets the searched.
     * @param searched
     */
    public void setSearched(boolean searched) {
        this.searched = searched;
    }
    public boolean isSearched() {
        return searched;
    }
}
