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

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Utility for wiring Swing text components to model setters using default content update rules.
 * <p>
 * For {@link JTextField}: registers both an {@code ActionListener} (fires on Enter) and an {@code InputVerifier}
 * (fires on focus loss) so every commit path is covered.
 * <p>
 * For {@link JTextArea}: registers a {@code DocumentListener} that fires on every character change.
 * <p>
 * Empty strings are normalized to {@code null} before the consumer is called.
 *
 * @since 5.0
 */
public final class TextBinder {

    private TextBinder() {
    }

    public static void bind(JTextField field, Consumer<String> onCommit) {
        field.addActionListener(e -> onCommit.accept(nullIfEmpty(field.getText())));
        field.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent c) {
                onCommit.accept(nullIfEmpty(field.getText()));
                return true;
            }
        });
    }

    private static String nullIfEmpty(String s) {
        return (s != null && s.isEmpty()) ? null : s;
    }
}
