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

package org.apache.cayenne.swing.components;

import org.apache.cayenne.modeler.undo.JCheckBoxUndoListener;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;

public class JCayenneCheckBox extends JCheckBox {

    private ActionListener actionListener;
    private boolean modelUpdateDisabled;

    public JCayenneCheckBox() {
        this.actionListener = new JCheckBoxUndoListener();
        this.addActionListener(this.actionListener);
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