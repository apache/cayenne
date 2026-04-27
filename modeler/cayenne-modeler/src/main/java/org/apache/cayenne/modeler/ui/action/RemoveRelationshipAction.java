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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.project.DataMapOps;
import org.apache.cayenne.modeler.ui.confirmremove.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.undo.RemoveRelationshipUndoableEdit;

import java.awt.event.ActionEvent;

/**
 * Removes currently selected relationship from either the DbEntity or ObjEntity.
 */
public class RemoveRelationshipAction extends RemoveAction implements MultipleObjectsAction {

	private final static String ACTION_NAME = "Remove Relationship";
	private final static String ACTION_NAME_MULTIPLE = "Remove Relationships";

	public RemoveRelationshipAction(Application application) {
		super(ACTION_NAME, application);
	}

	@Override
	public String getActionName(boolean multiple) {
		return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
	}

	/**
	 * Returns <code>true</code> if last object in the path contains a removable relationship.
	 */
	@Override
	public boolean enableForPath(ConfigurationNode object) {
		if (object == null) {
			return false;
		}

		return object instanceof Relationship;
	}

	@Override
	public void performAction(ActionEvent e, boolean allowAsking) {
		ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
		ProjectController mediator = getProjectController();

		ObjRelationship[] rels = getProjectController()
				.getSelectedObjRelationships();
		if (rels != null && rels.length > 0) {
			if ((rels.length == 1 && dialog.shouldDelete("ObjRelationship",
					rels[0].getName()))
					|| (rels.length > 1 && dialog
							.shouldDelete("selected ObjRelationships"))) {
				ObjEntity entity = mediator.getSelectedObjEntity();
				removeObjRelationships(entity, rels);
				application.getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(entity, rels));
			}
		} else {
			DbRelationship[] dbRels = getProjectController()
					.getSelectedDbRelationships();
			if (dbRels != null && dbRels.length > 0) {
				if ((dbRels.length == 1 && dialog.shouldDelete(
						"DbRelationship", dbRels[0].getName()))
						|| (dbRels.length > 1 && dialog
								.shouldDelete("selected DbRelationships"))) {
					DbEntity entity = mediator.getSelectedDbEntity();
					removeDbRelationships(entity, dbRels);
					application.getUndoManager().addEdit(new RemoveRelationshipUndoableEdit(entity, dbRels));
				}
			}
		}
	}

	public void removeObjRelationships(ObjEntity entity, ObjRelationship[] rels) {
		ProjectController controller = getProjectController();

		for (ObjRelationship rel : rels) {
			entity.removeRelationship(rel.getName());
			ObjRelationshipEvent e = ObjRelationshipEvent.ofRemove(application.getFrameController().getView(),
					rel, entity);
			controller.fireObjRelationshipEvent(e);
		}
	}

	public void removeDbRelationships(DbEntity entity, DbRelationship[] rels) {
		ProjectController controller = getProjectController();

		for(int i = 0; i < rels.length; i++) {
			rels[i] = entity.getRelationship(rels[i].getName());
			entity.removeRelationship(rels[i].getName());
			DbRelationshipEvent e = DbRelationshipEvent.ofRemove(application.getFrameController().getView(),
					rels[i], entity);
			controller.fireDbRelationshipEvent(e);
		}

		DataMapOps.removeBrokenObjToDbMappings(controller.getSelectedDataMap());
	}
}
