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
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class ValidationResultBrowserView extends JDialog {

    protected JTextArea messageLabel;
    protected JTextArea errorsDisplay;
    protected JButton closeButton;

    public ValidationResultBrowserView() {
        this.closeButton = new JButton("Close");

        this.messageLabel = new JTextArea();
        messageLabel.setEditable(false);
        messageLabel.setLineWrap(true);
        messageLabel.setWrapStyleWord(true);

        this.errorsDisplay = new JTextArea();
        errorsDisplay.setEditable(false);
        errorsDisplay.setLineWrap(true);
        errorsDisplay.setWrapStyleWord(true);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "fill:20dlu, 9dlu, p, 3dlu, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.add(new JScrollPane(
                messageLabel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 1));
        builder.addSeparator("Details", cc.xy(1, 3));
        builder.add(new JScrollPane(
                errorsDisplay,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 5));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);

        JComponent container = (JComponent) getContentPane();
        container.setLayout(new BorderLayout());
        container.add(builder.getPanel(), BorderLayout.CENTER);
        container.add(buttons, BorderLayout.SOUTH);

        // update top label bg
        messageLabel.setBackground(container.getBackground());

        // we need the right preferred size so that dialog "pack()" produces decent
        // default size...
        container.setPreferredSize(new Dimension(450, 270));
    }

    public JButton getCloseButton() {
        return closeButton;
    }

    public JTextArea getErrorsDisplay() {
        return errorsDisplay;
    }

    public JTextArea getMessageLabel() {
        return messageLabel;
    }
}
