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

package org.apache.cayenne.modeler.toolkit.text;

import org.apache.cayenne.validation.ValidationException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link JPasswordField} that fires registered commit listeners whenever the field value
 * changes and is committed — either by pressing Enter (action) or by losing focus (input
 * verifier). Empty strings are normalized to {@code null} before listeners are notified.
 * Listeners are not notified when the committed value matches the previous committed value
 * (or the value most recently assigned via {@link #setText}). Listeners may throw
 * {@link ValidationException} to reject the input — the field reverts to the previously
 * committed value and surfaces the message in an error dialog.
 */
public class CayennePasswordField extends JPasswordField {

    private final List<CayenneTextCommitListener> commitListeners;
    private String lastCommittedValue;

    public CayennePasswordField() {
        commitListeners = new ArrayList<>(2);
        addActionListener(e -> fireCommit());
        setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent c) {
                fireCommit();
                return true;
            }
        });
    }

    public void addCommitListener(CayenneTextCommitListener listener) {
        commitListeners.add(listener);
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        this.lastCommittedValue = normalize(t);
    }

    private void fireCommit() {

        String previous = this.lastCommittedValue;
        String normalized = normalize(getText());

        if (Objects.equals(normalized, previous)) {
            return;
        }

        try {
            for (CayenneTextCommitListener listener : commitListeners) {
                listener.onCommit(normalized);
            }
            this.lastCommittedValue = normalized;
        } catch (ValidationException ex) {
            setText(previous == null ? "" : previous);
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    ex.getUnlabeledMessage(),
                    "Invalid value",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String normalize(String text) {
        return (text == null || text.isEmpty()) ? null : text;
    }
}
