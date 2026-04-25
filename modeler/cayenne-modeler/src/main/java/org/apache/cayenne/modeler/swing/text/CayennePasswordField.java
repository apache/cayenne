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

package org.apache.cayenne.modeler.swing.text;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link JPasswordField} that fires registered commit listeners on Enter (action) or
 * focus loss (input verifier). Empty strings are normalized to {@code null} before listeners
 * are notified.
 *
 * @since 5.0
 */
public class CayennePasswordField extends JPasswordField {

    private final List<Consumer<String>> commitListeners;

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

    public void addCommitListener(Consumer<String> listener) {
        commitListeners.add(listener);
    }

    private void fireCommit() {
        String text = getText();
        String value = (text != null && text.isEmpty()) ? null : text;
        for (Consumer<String> listener : commitListeners) {
            listener.accept(value);
        }
    }
}
