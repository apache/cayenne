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
package org.apache.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Swing component displaying results produced by search feature.
 */
public class FindDialogView extends JDialog {
    private JButton okButton;
    private java.util.List entityButtons;

    public FindDialogView(java.util.List names) {
        entityButtons = new ArrayList();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Iterator it = names.iterator();
        
        while(it.hasNext()) {
            String name = (String) it.next();
            JButton b = new JButton(name);
            b.setBorder(new EmptyBorder(2, 10, 2, 10));       // top, left, bottom, right
            panel.add(b);

            entityButtons.add(b);
        }
        if(entityButtons.isEmpty())
            panel.add(new JLabel("No matched entities found!"));

        JPanel okPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        okPanel.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(okPanel, BorderLayout.SOUTH);

        setTitle("Found entities");
    }

    public JButton getOkButton() {
        return okButton;
    }

    public java.util.List getEntityButtons() {
        return entityButtons;
    }
}
