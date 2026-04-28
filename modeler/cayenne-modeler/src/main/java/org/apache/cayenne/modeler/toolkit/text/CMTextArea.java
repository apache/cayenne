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

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JTextArea} that fires registered commit listeners on every text-content change
 * (insert / remove). Empty strings are normalized to {@code null} before listeners are
 * notified.
 * <p>
 * A listener may declare {@link ValidationException} for API symmetry with the other
 * Cayenne text widgets, but unlike them this area fires per keystroke — there is no prior
 * committed value to revert to and surfacing a dialog mid-typing would be hostile, so any
 * thrown {@code ValidationException} is silently dropped here. Validation that reverts and
 * prompts belongs on a commit-on-focus-lost widget such as {@link CMTextField}.
 */
public class CMTextArea extends JTextArea {

    private final List<CMTextCommitListener> commitListeners;

    public CMTextArea() {
        commitListeners = new ArrayList<>(2);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireCommit();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireCommit();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    public void addCommitListener(CMTextCommitListener listener) {
        commitListeners.add(listener);
    }

    private void fireCommit() {
        String text = getText();
        String value = (text != null && text.isEmpty()) ? null : text;
        for (CMTextCommitListener listener : commitListeners) {
            try {
                listener.onCommit(value);
            } catch (ValidationException ignored) {
                // see class javadoc — per-keystroke fire makes revert/dialog unsuitable
            }
        }
    }
}
