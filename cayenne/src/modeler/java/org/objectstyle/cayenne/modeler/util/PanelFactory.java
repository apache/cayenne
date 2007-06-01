/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;


import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/** 
 * Implements a set of utility methods for laying out components on the panels.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
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

        for (int i = 0; i < buttons.length; i++) {
            panel.add(buttons[i]);
        }

        return panel;
    }

    public static JPanel createForm(
        String title,
        String[] labels,
        Component[] components) {
        Component[] jlabels = new Component[labels.length];
        for (int i = 0; i < labels.length; i++) {
            jlabels[i] = CayenneWidgetFactory.createLabel(labels[i]);
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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

        for (int i = 0; i < components.length; i++) {
            JPanel temp = new JPanel(new BorderLayout());
            temp.add(temp_panel, BorderLayout.CENTER);
            temp.add(components[i], BorderLayout.SOUTH);
            temp_panel = temp;
        }

        panel.add(temp_panel, BorderLayout.CENTER);

        if (buttons != null) {
            panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
        }
        return panel;
    }

}
