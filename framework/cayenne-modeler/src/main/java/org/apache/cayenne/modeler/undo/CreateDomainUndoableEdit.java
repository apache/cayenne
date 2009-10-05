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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.RemoveAction;

public class CreateDomainUndoableEdit extends CayenneUndoableEdit {

    @Override
    public String getPresentationName() {
        return "Create DataDomain";
    }

    

    private DataDomain domain;

    public CreateDomainUndoableEdit(DataDomain domain) {
        this.domain = domain;
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateDomainAction action = (CreateDomainAction) actionManager
                .getAction(CreateDomainAction.getActionName());
        action.createDomain(domain);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAction action = (RemoveAction) actionManager.getAction(RemoveAction
                .getActionName());
        action.removeDomain(domain);
    }
}
