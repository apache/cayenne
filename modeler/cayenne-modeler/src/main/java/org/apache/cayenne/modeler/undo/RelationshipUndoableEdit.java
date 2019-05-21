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
package org.apache.cayenne.modeler.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;

public class RelationshipUndoableEdit extends CayenneUndoableEdit {

	private static final long serialVersionUID = -1864303176024098961L;

	private Relationship relationship;
    private Relationship prevRelationship;
    private ProjectController projectController;
    private boolean useDb;

	public RelationshipUndoableEdit(Relationship relationship) {
		this.projectController = Application.getInstance().getFrameController().getProjectController();
		this.relationship = relationship;
		this.useDb = relationship instanceof DbRelationship;
		this.prevRelationship = copyRelationship(relationship);
	}

    @Override
	public void redo() throws CannotRedoException {
		fireRelationshipEvent(relationship, prevRelationship);
	}

	@Override
	public void undo() throws CannotUndoException {
		fireRelationshipEvent(prevRelationship, relationship);
	}

	private void fireRelationshipEvent(Relationship relToFire, Relationship currRel) {
		if(useDb) {
			fireDbRelationshipEvent(relToFire, currRel);
		} else {
			fireObjRelationshipEvent(relToFire, currRel);
		}
	}

	private void fireDbRelationshipEvent(Relationship relToFire, Relationship currRel) {
		DbEntity dbEntity = ((DbRelationship)currRel).getSourceEntity();
		dbEntity.removeRelationship(currRel.getName());
		dbEntity.addRelationship(relToFire);
		projectController
				.fireDbRelationshipEvent(
						new RelationshipEvent(this, relToFire, relToFire.getSourceEntity(), MapEvent.ADD));
	}

	private void fireObjRelationshipEvent(Relationship relToFire, Relationship currRel) {
		ObjEntity objEntity = ((ObjRelationship) currRel).getSourceEntity();
		objEntity.removeRelationship(currRel.getName());
		objEntity.addRelationship(relToFire);
		projectController
				.fireObjRelationshipEvent(
						new RelationshipEvent(this, relToFire, relToFire.getSourceEntity(), MapEvent.ADD));
	}

	@Override
	public String getRedoPresentationName() {
		return "Redo Edit relationship";
	}

	@Override
	public String getUndoPresentationName() {
		return "Undo Edit relationship";
	}

	private Relationship copyRelationship(Relationship relationship) {
		return useDb ? getDbRelationship(relationship) : getObjRelationship(relationship);
	}

	private DbRelationship getDbRelationship(Relationship dbRelationship) {
		DbRelationship rel = new DbRelationship();
		rel.setName(dbRelationship.getName());
		rel.setToDependentPK(((DbRelationship)dbRelationship).isToDependentPK());
		rel.setToMany(dbRelationship.isToMany());
		rel.setTargetEntityName(dbRelationship.getTargetEntityName());
		rel.setSourceEntity(dbRelationship.getSourceEntity());
		rel.setJoins(rel.getJoins());
		return rel;
	}

	private ObjRelationship getObjRelationship(Relationship objRelationship) {
		ObjRelationship rel = new ObjRelationship();
		rel.setName(objRelationship.getName());
		rel.setTargetEntityName(objRelationship.getTargetEntityName());
		rel.setSourceEntity(objRelationship.getSourceEntity());
		rel.setDeleteRule(((ObjRelationship)objRelationship).getDeleteRule());
		rel.setUsedForLocking(((ObjRelationship)objRelationship).isUsedForLocking());
		rel.setDbRelationshipPath(((ObjRelationship)objRelationship).getDbRelationshipPath());
		rel.setCollectionType(((ObjRelationship)objRelationship).getCollectionType());
		rel.setMapKey(((ObjRelationship)objRelationship).getMapKey());
		return rel;
	}
}
