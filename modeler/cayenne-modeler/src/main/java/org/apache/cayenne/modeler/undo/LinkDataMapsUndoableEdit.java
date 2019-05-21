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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.Collection;

public class LinkDataMapsUndoableEdit extends CayenneUndoableEdit {

    DataNodeDescriptor dataNodeDescriptor;
    Collection<String> linkedDataMaps;
    ProjectController mediator;

    @Override
    public String getPresentationName() {
        return "Link unlinked DataMaps";
    }

    public LinkDataMapsUndoableEdit(DataNodeDescriptor dataNodeDescriptor, Collection<String> linkedDataMaps, ProjectController mediator) {
        this.dataNodeDescriptor = dataNodeDescriptor;
        this.linkedDataMaps = linkedDataMaps;
        this.mediator = mediator;
    }

    @Override
    public void redo() throws CannotRedoException {
        for (DataMap dataMap : ((DataChannelDescriptor) mediator.getProject().getRootNode()).getDataMaps()) {
            if (!linkedDataMaps.contains(dataMap.getName())) {
                dataNodeDescriptor.getDataMapNames().add(dataMap.getName());
                mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNodeDescriptor));
            }
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        dataNodeDescriptor.getDataMapNames().retainAll(linkedDataMaps);
        mediator.fireDataNodeEvent(new DataNodeEvent(this, dataNodeDescriptor));
    }

}
