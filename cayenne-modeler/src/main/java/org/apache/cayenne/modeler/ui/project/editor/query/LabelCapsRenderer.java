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
package org.apache.cayenne.modeler.ui.project.editor.query;

import java.awt.Component;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.apache.cayenne.query.CapsStrategy;

/**
 * Shows a {@link CapsStrategy} of the result column labels of a raw query by its description.
 */
public class LabelCapsRenderer extends DefaultListCellRenderer {

    /**
     * The strategies to choose from, in the order they are listed.
     */
    public static final CapsStrategy[] STRATEGIES = {CapsStrategy.DEFAULT, CapsStrategy.LOWER, CapsStrategy.UPPER};

    private static final Map<CapsStrategy, String> LABELS = Map.of(
            CapsStrategy.DEFAULT, "Database Default",
            CapsStrategy.LOWER, "Force Lower Case",
            CapsStrategy.UPPER, "Force Upper Case");

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
        Object label = value instanceof CapsStrategy strategy ? LABELS.get(strategy) : value;
        return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
    }
}
