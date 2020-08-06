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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.merge.context.EntityMergeSupport;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.objentity.EntitySyncController;
import org.apache.cayenne.modeler.undo.DbEntitySyncUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;

/**
 * Action that synchronizes all ObjEntities with the current state of the
 * selected DbEntity.
 */
public class DbEntitySyncAction extends CayenneAction {

	public static String getActionName() {
		return "Sync Dependent ObjEntities with DbEntity";
	}

	public DbEntitySyncAction(final Application application) {
		super(getActionName(), application);
	}

	@Override
	public KeyStroke getAcceleratorKey() {
		return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit
				.getDefaultToolkit()
				.getMenuShortcutKeyMask());
	}

	public String getIconName() {
		return "icon-sync.png";
	}

	/**
	 * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
	 */
	public void performAction(final ActionEvent e) {
		syncDbEntity();
	}

	protected void syncDbEntity() {
		final ProjectController mediator = getProjectController();
		final DbEntity dbEntity = mediator.getCurrentDbEntity();

		if (dbEntity != null) {

			final Collection<ObjEntity> entities = dbEntity.getDataMap().getMappedEntities(dbEntity);
			if (entities.isEmpty()) {
				return;
			}

			final EntityMergeSupport merger = new EntitySyncController(Application.getInstance().getFrameController(), dbEntity)
					.createMerger();

			if (merger == null) {
				return;
			}

			merger.setNameGenerator(new PreserveRelationshipNameGenerator());

			final DbEntitySyncUndoableEdit undoableEdit = new DbEntitySyncUndoableEdit((DataChannelDescriptor) mediator
					.getProject().getRootNode(), mediator.getCurrentDataMap());

			// filter out inherited entities, as we need to add attributes only to the roots
			filterInheritedEntities(entities);

			boolean hasChanges = false;
			for (final ObjEntity entity : entities) {

				final DbEntitySyncUndoableEdit.EntitySyncUndoableListener listener = undoableEdit.new EntitySyncUndoableListener(
						entity);

				merger.addEntityMergeListener(listener);

				final Collection<DbAttribute> meaningfulFKs = merger.getMeaningfulFKs(entity);

				// TODO: addition or removal of model objects should be reflected in listener callbacks...
				// we should not be trying to introspect the merger
				if (merger.isRemovingMeaningfulFKs() && !meaningfulFKs.isEmpty()) {
					undoableEdit.addEdit(undoableEdit.new MeaningfulFKsUndoableEdit(entity, meaningfulFKs));
					hasChanges = true;
				}

				if (merger.synchronizeWithDbEntity(entity)) {
					mediator.fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.CHANGE));
					hasChanges = true;
				}

				merger.removeEntityMergeListener(listener);
			}

			if (hasChanges) {
				application.getUndoManager().addEdit(undoableEdit);
			}
		}
	}

	/**
	 * This method works only for case when all inherited entities bound to same DbEntity
	 * if this will ever change some additional checks should be performed.
	 */
	private void filterInheritedEntities(final Collection<ObjEntity> entities) {
		// entities.removeIf(c -> c.getSuperEntity() != null);
		final Iterator<ObjEntity> it = entities.iterator();
		while (it.hasNext()) {
			if (it.next().getSuperEntity() != null) {
				it.remove();
			}
		}
	}

	static class PreserveRelationshipNameGenerator extends DefaultObjectNameGenerator {

		@Override
		public String relationshipName(final DbRelationship... relationshipChain) {
			if (relationshipChain.length == 0) {
				return super.relationshipName(relationshipChain);
			}
			final DbRelationship last = relationshipChain[relationshipChain.length - 1];
			// must be in sync with DefaultBaseNameVisitor.visitDbRelationship
			if (last.getName().startsWith("untitledRel")) {
				return super.relationshipName(relationshipChain);
			}

			// keep manually set relationship name
			return last.getName();
		}
	}
}
