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

import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.editor.ObjCallbackMethod;

public class RemoveCallbackMethodUndoableEdit extends CayenneUndoableEdit {

    private CallbackType callbackType;
    private ObjCallbackMethod[] methods;

    public RemoveCallbackMethodUndoableEdit(CallbackType callbackType,
    		ObjCallbackMethod[] methods) {
        this.callbackType = callbackType;
        this.methods = methods;
    }

    @Override
    public String getPresentationName() {
    	return "Remove Obj Callback Methods";
    }

    @Override
    public void redo() throws CannotRedoException {
    	RemoveCallbackMethodAction action = actionManager
                .getAction(RemoveCallbackMethodAction.class);
        for (ObjCallbackMethod method : methods) {
            action.removeCallbackMethod(callbackType, method.getName());
        }
    }

    @Override
    public void undo() throws CannotUndoException {
    	CreateCallbackMethodAction action = actionManager
                .getAction(CreateCallbackMethodAction.class);
        for (ObjCallbackMethod method : methods) {
            action.createCallbackMethod(callbackType, method.getName());
        }
    }
}
