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
package org.apache.cayenne.modeler.util;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Overrides CellEditor to allow multiple selection in table
 * without bothering the editor.
 * 
 */
public class CayenneCellEditor extends DefaultCellEditor {
    public CayenneCellEditor(final JTextField textField) {
        super(textField);
    }
    
    public CayenneCellEditor(final JCheckBox checkBox) {
        super(checkBox);
    }
    
    public CayenneCellEditor(final JComboBox comboBox) {
        super(comboBox);
    }
    
    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            //allow multiple selection without 
            
            MouseEvent me = (MouseEvent) e;
            if (me.isControlDown() || me.isShiftDown()) {
                return false;
            }
        }
        
        return super.isCellEditable(e);
    }
}
