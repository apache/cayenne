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

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;

/** 
 * Represents events resulted from Relationship changes 
 * in CayenneModeler. This event is used for both ObjRelationships
 * and DbRelationships.
 * 
 * 
 */
public class RelationshipEvent extends EntityEvent {
	protected Relationship relationship;

	/** Creates a Relationship change event. */
	public RelationshipEvent(Object src, Relationship rel, Entity entity) {
		super(src, entity);
		setRelationship(rel);
	}

	/** Creates a Relationship event of a specified type. */
	public RelationshipEvent(
		Object src,
		Relationship rel,
		Entity entity,
		int id) {

		this(src, rel, entity);
		setId(id);
	}

	/** Creates a Relationship name change event. */
	public RelationshipEvent(
		Object src,
		Relationship rel,
		Entity entity,
		String oldName) {
			
		this(src, rel, entity);
        setOldName(oldName);
	}

	/** Returns relationship associated with this event. */
	public Relationship getRelationship() {
		return relationship;
	}

	/**
	 * Sets relationship associated with this event.
	 * 
	 * @param relationship The relationship to set
	 */
	public void setRelationship(Relationship relationship) {
		this.relationship = relationship;
	}
	
	@Override
    public String getNewName() {
		return (relationship != null) ? relationship.getName() : null;
	}
}
