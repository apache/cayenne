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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;

public class JTextFieldUndoListener implements UndoableEditListener {

	private UndoManager undoManager;
	private CompoundEdit compoundEdit;
	private JTextComponent textComponent;
	private UndoDocumentListener undoDocumentListener;

	private Object currentObj;
	private TreePath[] paths;

	private int lastOffset;
	private int lastLength;

	private JTextFieldUndoListener self = this;

	private boolean inProgress = false;

	private class UndoDocumentListener implements DocumentListener {

		public void changedUpdate(DocumentEvent e) {
		}

		public void insertUpdate(final DocumentEvent e) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					int offset = e.getOffset() + e.getLength();
					offset = Math.min(offset, textComponent.getDocument()
							.getLength());
					textComponent.setCaretPosition(offset);
				}
			});
		}

		public void removeUpdate(DocumentEvent e) {
			textComponent.setCaretPosition(e.getOffset());
		}

	}

	public JTextFieldUndoListener(JTextComponent textComponent) {
		this.textComponent = textComponent;
		this.undoManager = Application.getInstance().getUndoManager();
		this.undoDocumentListener = new UndoDocumentListener();

		this.textComponent.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {
				if (self.currentObj == null) {
					self.currentObj = getProjectController().getCurrentObject();
					self.paths = ((CayenneModelerFrame) Application
							.getInstance().getFrameController().getView())
							.getView().getProjectTreeView().getSelectionPaths();
				}
			}

			public void focusLost(FocusEvent e) {
				self.currentObj = null;

				if (compoundEdit != null) {
					compoundEdit.end();
					compoundEdit = null;
				}
			}

		});

		this.textComponent.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				inProgress = true;
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}

		});

	}

	public void undoableEditHappened(UndoableEditEvent e) {
		if (inProgress) {
			if (compoundEdit == null) {
				compoundEdit = startCompoundEdit(e.getEdit());
			} else {
				AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) e
						.getEdit();

				if (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
					compoundEdit.addEdit(e.getEdit());
					return;
				}

				int offsetChange = textComponent.getCaretPosition()
						- lastOffset;
				int lengthChange = textComponent.getDocument().getLength()
						- lastLength;

				if (offsetChange == lengthChange && Math.abs(offsetChange) == 1) {
					compoundEdit.addEdit(e.getEdit());
					lastOffset = textComponent.getCaretPosition();
					lastLength = textComponent.getDocument().getLength();
					return;
				} else {
					compoundEdit.end();
					compoundEdit = startCompoundEdit(e.getEdit());
				}
			}

			inProgress = !inProgress;
		}
	}

	private CompoundEdit startCompoundEdit(UndoableEdit e) {
		lastOffset = textComponent.getCaretPosition();
		lastLength = textComponent.getDocument().getLength();

		CompoundEdit compoundEdit = new TextCompoundEdit();
		compoundEdit.addEdit(e);

		undoManager.addEdit(compoundEdit);

		return compoundEdit;
	}

	private class TextCompoundEdit extends CompoundEdit {
		

		public boolean isInProgress() {
			return false;
		}

		@Override
		public String getRedoPresentationName() {
			return "Redo Text Change";
		}

		@Override
		public String getUndoPresentationName() {
			return "Undo Text Change";
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		public void undo() throws CannotUndoException {
			textComponent.getDocument().addDocumentListener(
					undoDocumentListener);

			if (compoundEdit != null) {
				compoundEdit.end();
			}

			restoreSelections();

			super.undo();

			compoundEdit = null;

			textComponent.getDocument().removeDocumentListener(
					undoDocumentListener);

			textComponent.requestFocusInWindow();
			textComponent.selectAll();
		}

		public void redo() throws CannotRedoException {
			textComponent.getDocument().addDocumentListener(
					undoDocumentListener);

			super.redo();

			textComponent.getDocument().removeDocumentListener(
					undoDocumentListener);
			textComponent.requestFocusInWindow();
		}

		private void restoreSelections() {
			((CayenneModelerFrame) Application.getInstance()
					.getFrameController().getView()).getView()
					.getProjectTreeView().setSelectionPaths(self.paths);

			if (self.currentObj instanceof DataMap) {

				getProjectController().fireDataMapDisplayEvent(
						new DataMapDisplayEvent(this,
								(DataMap) self.currentObj,
								getProjectController().getCurrentDataDomain(),
								getProjectController().getCurrentDataNode()));

			} else if (self.currentObj instanceof ObjEntity) {

				getProjectController().fireObjEntityDisplayEvent(
						new EntityDisplayEvent(this,
								(ObjEntity) self.currentObj,
								getProjectController().getCurrentDataMap(),
								getProjectController().getCurrentDataNode(),
								getProjectController().getCurrentDataDomain()));

			} else if (self.currentObj instanceof DbEntity) {

				getProjectController().fireDbEntityDisplayEvent(
						new EntityDisplayEvent(this, (DbEntity) self.currentObj,
								getProjectController().getCurrentDataMap(),
								getProjectController().getCurrentDataNode(),
								getProjectController().getCurrentDataDomain()));
				

			}
		}
	}

	private ProjectController getProjectController() {
		return Application.getInstance().getFrameController()
				.getProjectController();
	}
}
