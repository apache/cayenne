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
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.scopemvc.view.awt.AWTUtilities;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A dialog rendering a progress bar. It is normally controlled by a subclass of
 * LongRunningTask.
 * 
 */
public class ProgressDialog extends JDialog {

    protected JProgressBar progressBar;
    protected JLabel statusLabel;
    protected JButton cancelButton;

    public ProgressDialog(JFrame parent, String title, String message) {
        super(parent, title);
        init(message);
    }

    private void init(String message) {
        progressBar = new JProgressBar();
        statusLabel = new JLabel(message, SwingConstants.LEFT);
        JLabel messageLabel = new JLabel(message, SwingConstants.LEFT);
        cancelButton = new JButton("Cancel");

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout("fill:max(250dlu;pref)", "p, 3dlu, p, 3dlu, p");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.add(messageLabel, cc.xy(1, 1));
        builder.add(progressBar, cc.xy(1, 3));
        builder.add(statusLabel, cc.xy(1, 5));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);

        Container root = getContentPane();
        root.setLayout(new BorderLayout(5, 5));

        root.add(builder.getPanel(), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setResizable(false);
        pack();
        AWTUtilities.centreOnWindow(getOwner(), this);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }
}
