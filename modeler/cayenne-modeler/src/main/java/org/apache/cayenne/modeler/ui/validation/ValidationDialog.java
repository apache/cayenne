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

package org.apache.cayenne.modeler.ui.validation;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Collection;

/**
 * Modal "operation completed with issues" reporter — shows a message and the validation
 * failures from a recent action (DB generation, migration, etc.).
 */
public class ValidationDialog extends AppDialog {

    private final JTextArea messageArea;
    private final JTextArea errorsArea;
    private final JButton closeButton;

    public ValidationDialog(Application app, Window owner, String title, String message, ValidationResult result) {
        this(app, owner, title, message);
        errorsArea.setText(buildValidationText(result));
    }

    public ValidationDialog(Application app, Window owner, String title, String message, Collection<ValidationResult> failures) {
        this(app, owner, title, message);
        for (ValidationResult failure : failures) {
            if (failure != null) {
                errorsArea.append(buildValidationText(failure) + " ");
            }
        }
    }

    private ValidationDialog(Application app, Window owner, String title, String message) {
        super(app, owner, title, ModalityType.APPLICATION_MODAL);

        this.messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        this.errorsArea = new JTextArea();
        errorsArea.setEditable(false);
        errorsArea.setLineWrap(true);
        errorsArea.setWrapStyleWord(true);

        this.closeButton = new JButton("Close");

        initLayout();
        initBindings();
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(50dlu;pref):grow",
                "fill:20dlu, $pgap, p, $rgap, fill:40dlu:grow"));
        builder.setDefaultDialogBorder();
        builder.add(messageArea, cc.xy(1, 1));
        builder.addSeparator("Details", cc.xy(1, 3));
        builder.add(new JScrollPane(
                errorsArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), cc.xy(1, 5));

        getRootPane().setDefaultButton(closeButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(closeButton);

        JComponent container = (JComponent) getContentPane();
        container.setLayout(new BorderLayout());
        container.add(builder.getPanel(), BorderLayout.CENTER);
        container.add(buttons, BorderLayout.SOUTH);

        // match the message area background to the surrounding container
        messageArea.setBackground(container.getBackground());

        // pack() needs a sane preferred size for a decent default
        container.setPreferredSize(new Dimension(450, 270));
    }

    private void initBindings() {
        closeButton.addActionListener(e -> dispose());
    }

    private static String buildValidationText(ValidationResult result) {
        StringBuilder buffer = new StringBuilder();
        String separator = System.lineSeparator();

        for (ValidationFailure failure : result.getFailures()) {
            if (buffer.length() > 0) {
                buffer.append(separator);
            }
            if (failure.getSource() != null) {
                buffer.append("[SQL: ").append(failure.getSource()).append("] - ");
            }
            if (failure.getDescription() != null) {
                buffer.append(failure.getDescription());
            }
        }
        return buffer.toString();
    }
}
