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
import org.apache.cayenne.modeler.action.UpdateValidationConfigAction;
import org.apache.cayenne.project.validation.ValidationConfig;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @since 5.0
 */
public class UpdateValidationConfigUndoableEdit extends CayenneUndoableEdit {

    private final DataChannelDescriptor dataChannel;
    private final ValidationConfig oldConfig;
    private final ValidationConfig newConfig;

    public UpdateValidationConfigUndoableEdit(DataChannelDescriptor dataChannel,
                                              ValidationConfig oldConfig, ValidationConfig newConfig) {
        this.dataChannel = dataChannel;
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
    }

    @Override
    public String getPresentationName() {
        return "Update ValidationConfig";
    }

    @Override
    public void redo() throws CannotRedoException {
        actionManager.getAction(UpdateValidationConfigAction.class)
                .putDataChannel(dataChannel)
                .putConfig(newConfig)
                .setUndoable(false)
                .performAction(null);
    }

    @Override
    public void undo() throws CannotUndoException {
        actionManager.getAction(UpdateValidationConfigAction.class)
                .putDataChannel(dataChannel)
                .putConfig(oldConfig)
                .setUndoable(false)
                .performAction(null);
    }
}
