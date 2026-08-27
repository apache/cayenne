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

package org.apache.cayenne.modeler.toolkit.checkbox;

import org.apache.cayenne.modeler.undo.CayenneUndoManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class CMCheckBox extends JCheckBox {

    private boolean modelUpdateDisabled;

    public CMCheckBox(CayenneUndoManager undoManager) {
        ActionListener undoListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undoManager.addEdit(new CMCheckBoxUndoableEdit((JCheckBox) e.getSource(), this));
            }
        };
        this.addActionListener(undoListener);
    }

    @Override
    public void setSelected(boolean b) {
        modelUpdateDisabled = true;
        try {
            super.setSelected(b);
        } finally {
            modelUpdateDisabled = false;
        }
    }

    @Override
    public void addItemListener(ItemListener l) {
        super.addItemListener(e -> {
            if(!modelUpdateDisabled) {
                l.itemStateChanged(e);
            }
        });
    }
}
