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

import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;

import org.apache.cayenne.modeler.ActionManager;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.ProjectTreeView;

public abstract class CayenneUndoableEdit extends AbstractUndoableEdit {
	

	protected ProjectTreeView treeView;
	protected ActionManager actionManager;
	protected ProjectController controller;
	
	private TreePath[] paths;
	

	public CayenneUndoableEdit() {
		this.treeView = ((CayenneModelerFrame) Application.getInstance()
				.getFrameController().getView()).getView().getProjectTreeView();
		this.actionManager = Application.getInstance().getActionManager();
		this.paths = this.treeView.getSelectionPaths();
		this.controller = Application.getInstance().getFrameController().getProjectController();
	}

	protected void restoreSelections() {
		this.treeView.setSelectionPaths(paths);
	}

	@Override
	public boolean canRedo() {
		return true;
	}
}
