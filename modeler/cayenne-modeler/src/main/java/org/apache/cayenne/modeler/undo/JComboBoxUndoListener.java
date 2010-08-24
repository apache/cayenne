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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.undo.UndoManager;

import org.apache.cayenne.modeler.Application;

public class JComboBoxUndoListener implements ItemListener {

    private Object deselectedItem;

    public void itemStateChanged(ItemEvent e) {
        int stateChange = e.getStateChange();

        switch (stateChange) {
            case ItemEvent.DESELECTED:
                deselectedItem = e.getItem();
                break;
            case ItemEvent.SELECTED:

                UndoManager undoManager = Application.getInstance().getUndoManager();
                undoManager.addEdit(new JComboBoxUndoableEdit(
                        (JComboBox) e.getSource(),
                        deselectedItem,
                        e.getItem(),
                        this));

                break;
        }

    }
}
