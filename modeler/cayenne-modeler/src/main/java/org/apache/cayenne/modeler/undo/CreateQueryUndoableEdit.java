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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.map.QueryDescriptor;

public class CreateQueryUndoableEdit extends CayenneUndoableEdit {

    private DataChannelDescriptor domain;
    private DataMap map;
    private QueryDescriptor query;

    public CreateQueryUndoableEdit(DataChannelDescriptor domain, DataMap map, QueryDescriptor query) {
        this.domain = domain;
        this.map = map;
        this.query = query;
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateQueryAction action = actionManager.getAction(CreateQueryAction.class);
        action.createQuery(domain, map, query);
    }

    @Override
    public String getPresentationName() {
        return "Create Query";
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAction action = actionManager.getAction(RemoveAction.class);
        action.removeQuery(map, query);
    }

}
