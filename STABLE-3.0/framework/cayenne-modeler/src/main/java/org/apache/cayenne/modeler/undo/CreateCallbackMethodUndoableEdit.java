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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.editor.CallbackType;

public class CreateCallbackMethodUndoableEdit extends CayenneUndoableEdit {

	

	private CallbackMap map;
	private CallbackType callbackType;
	private String methodName;

	@Override
	public String getPresentationName() {
		return "Create Callback Method";
	}

	@Override
	public void redo() throws CannotRedoException {
		CreateCallbackMethodAction action = (CreateCallbackMethodAction) actionManager
				.getAction(CreateCallbackMethodAction.getActionName());
		action.createCallbackMethod(map, callbackType, methodName);
	}

	@Override
	public void undo() throws CannotUndoException {
		RemoveCallbackMethodAction action = (RemoveCallbackMethodAction) actionManager
				.getAction(RemoveCallbackMethodAction.getActionName());
		action.removeCallbackMethod(map, callbackType, methodName);
	}

	public CreateCallbackMethodUndoableEdit(CallbackMap map,
			CallbackType callbackType, String methodName) {
		this.map = map;
		this.callbackType = callbackType;
		this.methodName = methodName;
	}

}
