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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.modeler.action.CreateDataMapEntityListenerAction;
import org.apache.cayenne.modeler.action.RemoveEntityListenerForDataMapAction;


public class CreateDataMapEntityListenerUndoableEdit extends CayenneUndoableEdit {

    private DataMap dataMap;
    private EntityListener listener;

    public CreateDataMapEntityListenerUndoableEdit(DataMap dataMap, EntityListener listener) {
        this.dataMap = dataMap;
        this.listener = listener;
    }

    @Override
    public String getPresentationName() {
        return "Create Entity Listener";
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateDataMapEntityListenerAction action = (CreateDataMapEntityListenerAction) actionManager
                .getAction(CreateDataMapEntityListenerAction.getActionName());
        action.createMapListener(dataMap, listener);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveEntityListenerForDataMapAction action = (RemoveEntityListenerForDataMapAction) actionManager
                .getAction(RemoveEntityListenerForDataMapAction.getActionName());

        action.removeEntityListener(dataMap, listener.getClassName());
    }
}
