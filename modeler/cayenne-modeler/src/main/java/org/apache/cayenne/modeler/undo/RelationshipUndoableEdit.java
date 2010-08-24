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
package org.apache.cayenne.modeler.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.modeler.util.ProjectUtil;

public class RelationshipUndoableEdit extends CompoundEdit {

    private DbRelationship relationship;
	
    @Override
	public void redo() throws CannotRedoException {
		super.redo();
		
		ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(
				relationship, false);

		dialog.setVisible(true);
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		
		ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(
				relationship, false);

		dialog.setVisible(true);
	}

	@Override
	public String getRedoPresentationName() {
		return "Redo Edit relationship";
	}

	@Override
	public String getUndoPresentationName() {
		return "Undo Edit relationship";
	}

	public RelationshipUndoableEdit(DbRelationship relationship) {
		this.relationship = relationship;
	}

	public void addDbJoinAddUndo(final DbJoin join) {
		this.addEdit(new AbstractUndoableEdit() {
			

			@Override
			public void redo() throws CannotRedoException {
				relationship.addJoin(join);
			}

			@Override
			public void undo() throws CannotUndoException {
				relationship.removeJoin(join);
			}
		});
	}

	public void addDbJoinRemoveUndo(final DbJoin join) {
		this.addEdit(new AbstractUndoableEdit() {

			

			@Override
			public void redo() throws CannotRedoException {
				relationship.removeJoin(join);
			}

			@Override
			public void undo() throws CannotUndoException {
				relationship.addJoin(join);
			}

		});
	}

	public void addNameUndo(final DbRelationship relationship,
			final String oldName, final String newName) {
		this.addEdit(new AbstractUndoableEdit() {

			

			@Override
			public void redo() throws CannotRedoException {
				ProjectUtil.setRelationshipName(relationship.getSourceEntity(),
						relationship, newName);
			}

			@Override
			public void undo() throws CannotUndoException {
				ProjectUtil.setRelationshipName(relationship.getSourceEntity(),
						relationship, oldName);
			}

		});
	}
}
