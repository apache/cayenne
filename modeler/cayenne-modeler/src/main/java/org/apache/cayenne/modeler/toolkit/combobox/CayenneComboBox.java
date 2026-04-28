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

package org.apache.cayenne.modeler.toolkit.combobox;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.List;
import java.util.Vector;

/**
 * A modeler-styled JComboBox.
 */
public class CayenneComboBox<T> extends JComboBox<T> {
    
    public CayenneComboBox() {
        setFont(UIManager.getFont("Label.font"));
        setBackground(Color.WHITE);
        setMaximumRowCount(12);
    }

    @SafeVarargs
    public CayenneComboBox(T... model) {
        this();
        setModel(new DefaultComboBoxModel<>(model));
    }

    public CayenneComboBox(List<T> model) {
        this();
        setModel(new DefaultComboBoxModel<>(new Vector<>(model)));
    }
}
