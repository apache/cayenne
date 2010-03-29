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

package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.undo.RemoveRelationshipUndoableEdit;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ProjectPath;

/**
 * Removes currently selected relationship from either the DbEntity or
 * ObjEntity.
 * 
 */
public class RemoveRelationshipAction extends RemoveAction implements
		MultipleObjectsAction {

	

	private final static String ACTION_NAME = "Remove Relationship";

	/**
	 * Name of action if multiple rels are selected
	 */
	private final static String ACTION_NAME_MULTIPLE = "Remove Relationships";

	public static String getActionName() {
		return ACTION_NAME;
	}

	public String getActionName(boolean multiple) {
		return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
	}

	public RemoveRelationshipAction(Application application) {
		super(ACTION_NAME, application);
	}

	/**
	 * Returns <code>true</code> if last object in the path contains a removable
	 * relationship.
	 */
	@Override
	public boolean enableForPath(ProjectPath path) {
		if (path == null) {
			return false;
		}

		return path.getObject() instanceof Relationship;
	}

	@Override
	public void performAction(ActionEvent e, boolean allowAsking) {
		ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
		ProjectController mediator = getProjectController();

		ObjRelationship[] rels = getProjectController()
				.getCurrentObjRelationships();
		if (rels != null && rels.length > 0) {
			if ((rels.length == 1 && dialog.shouldDelete("ObjRelationship",
					rels[0].getName()))
					|| (rels.length > 1 && dialog
							.shouldDelete("selected ObjRelationships"))) {
				ObjEntity entity = mediator.getCurrentObjEntity();
				removeObjRelationships(entity, rels);
				Application.getInstance().getUndoManager().addEdit(
						new RemoveRelationshipUndoableEdit(entity, rels));
			}
		} else {
			DbRelationship[] dbRels = getProjectController()
					.getCurrentDbRelationships();
			if (dbRels != null && dbRels.length > 0) {
				if ((dbRels.length == 1 && dialog.shouldDelete(
						"DbRelationship", dbRels[0].getName()))
						|| (dbRels.length > 1 && dialog
								.shouldDelete("selected DbRelationships"))) {
					DbEntity entity = mediator.getCurrentDbEntity();
					removeDbRelationships(entity, dbRels);
					Application.getInstance().getUndoManager().addEdit(
							new RemoveRelationshipUndoableEdit(entity, dbRels));
				}
			}
		}
	}

	public void removeObjRelationships(ObjEntity entity, ObjRelationship[] rels) {
		ProjectController mediator = getProjectController();

		for (ObjRelationship rel : rels) {
			entity.removeRelationship(rel.getName());
			RelationshipEvent e = new RelationshipEvent(Application.getFrame(),
					rel, entity, MapEvent.REMOVE);
			mediator.fireObjRelationshipEvent(e);
		}
	}

	public void removeDbRelationships(DbEntity entity, DbRelationship[] rels) {
		ProjectController mediator = getProjectController();

		for (DbRelationship rel : rels) {
			entity.removeRelationship(rel.getName());

			RelationshipEvent e = new RelationshipEvent(Application.getFrame(),
					rel, entity, MapEvent.REMOVE);
			mediator.fireDbRelationshipEvent(e);
		}

		ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());
	}
}
