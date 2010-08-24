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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Implements a set of utility methods for laying out components on the panels.
 * 
 */

// TODO: get rid of PanelFactory in favor of JGoodies Forms
public class PanelFactory {

    /** 
     * Creates and returns a panel with right-centered buttons.
     */
    public static JPanel createButtonPanel(JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 7));
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        for (JButton button : buttons) {
            panel.add(button);
        }

        return panel;
    }

    public static JPanel createForm(
        String title,
        String[] labels,
        Component[] components) {
        Component[] jlabels = new Component[labels.length];
        for (int i = 0; i < labels.length; i++) {
            jlabels[i] = new JLabel(labels[i]);
        }
        return createForm(title, jlabels, components);
    }

    public static JPanel createForm(
        Component[] leftComponents,
        Component[] rightComponents) {
        return createForm(null, leftComponents, rightComponents);
    }

    /** 
     * Create panel with aligned labels on the right and fields on the left.
     */
    public static JPanel createForm(
        String title,
        Component[] leftComponents,
        Component[] rightComponents) {

        if (leftComponents.length != rightComponents.length) {
            throw new IllegalArgumentException(
                "Arrays must be the same size, instead got "
                    + leftComponents.length
                    + "and "
                    + rightComponents.length);
        }

        int numRows = leftComponents.length;
        if (numRows == 0) {
            throw new IllegalArgumentException("Zero components.");
        }

        FormLayout layout = new FormLayout("right:100, 3dlu, left:300", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        if (title != null) {
            builder.appendSeparator(title);
        }

        for (int i = 0; i < numRows; i++) {
            builder.append(leftComponents[i], rightComponents[i]);
            builder.nextLine();
        }

        return builder.getPanel();
    }

    /** 
     * Creates panel with table within scroll panel and buttons in the bottom.
     * Also sets the resizing and selection policies of the table to
     * AUTO_RESIZE_OFF and SINGLE_SELECTION respectively.
     */
    public static JPanel createTablePanel(JTable table, JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));

        // Create table with two columns and no rows.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Panel to add space between table and EAST/WEST borders
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add Add and Remove buttons
        if (buttons != null) {
            panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
        }
        return panel;
    }

    /** Creates panel with table within scroll panel and buttons in the bottom.
      * Also sets the resizing and selection policies of the table to
      * AUTO_RESIZE_OFF and SINGLE_SELECTION respectively.*/
    public static JPanel createTablePanel(
        JTable table,
        JComponent[] components,
        JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));

        JPanel temp_panel = new JPanel(new BorderLayout());

        // Create table with two columns and no rows.
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scroll_pane = new JScrollPane(table);
        temp_panel.add(scroll_pane, BorderLayout.CENTER);

        for (JComponent component : components) {
            JPanel temp = new JPanel(new BorderLayout());
            temp.add(temp_panel, BorderLayout.CENTER);
            temp.add(component, BorderLayout.SOUTH);
            temp_panel = temp;
        }

        panel.add(temp_panel, BorderLayout.CENTER);

        if (buttons != null) {
            panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
        }
        return panel;
    }

}
