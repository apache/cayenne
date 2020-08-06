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

package org.apache.cayenne.modeler.util;

import java.util.Collection;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * Utility class to create standard Swing widgets following default look-and-feel of
 * CayenneModeler.
 */
public interface WidgetFactory {

    /**
     * Creates a new JComboBox with a collection of model objects.
     */
    JComboBox<String> createComboBox(Collection<String> model, boolean sort);

    /**
     * Creates a new JComboBox with an array of model objects.
     */
    <E> JComboBox<E> createComboBox(E[] model, boolean sort);

    /**
     * Creates a new JComboBox.
     */
    <E> JComboBox<E> createComboBox();

    /**
     * Creates undoable JComboBox.
     */
    <E> JComboBox<E> createUndoableComboBox();

    /**
     * Creates cell editor for text field
     */
    DefaultCellEditor createCellEditor(JTextField textField);

    /**
     * Creates cell editor for a table with combo as editor component. Type of this editor
     * depends on auto-completion behavior of JComboBox
     * 
     * @param combo JComboBox to be used as editor component
     */
    TableCellEditor createCellEditor(JComboBox<?> combo);

}
