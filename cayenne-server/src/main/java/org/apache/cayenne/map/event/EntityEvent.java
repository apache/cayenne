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

package org.apache.cayenne.map.event;

import org.apache.cayenne.map.Entity;

/** 
 * Represents events resulted from Entity changes 
 * in CayenneModeler. This event is used for both ObjEntities
 * and DbEntities.
 */
public class EntityEvent extends MapEvent {
	protected Entity<?,?,?> entity;

	/** Creates a Entity change event. */
	public EntityEvent(Object src, Entity<?,?,?> entity) {
		super(src);
		setEntity(entity);
	}

	/** Creates a Entity event of a specified type. */
	public EntityEvent(Object src, Entity<?,?,?> entity, int id) {
		this(src, entity);
		setId(id);
	}

	/** Creates a Entity name change event.*/
	public EntityEvent(Object src, Entity<?,?,?> entity, String oldName) {
		this(src, entity);
		setOldName(oldName);
	}

	/** Returns entity object associated with this event. */
	public Entity<?,?,?> getEntity() {
		return entity;
	}
	
	/**
	 * Sets the entity.
	 * 
	 * @param entity The entity to set
	 */
	public void setEntity(Entity<?,?,?> entity) {
		this.entity = entity;
	}
	
	@Override
    public String getNewName() {
		return (entity != null) ? entity.getName() : null;
	}
}
