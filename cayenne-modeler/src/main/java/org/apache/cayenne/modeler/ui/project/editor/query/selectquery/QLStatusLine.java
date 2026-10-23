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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.FluentSelect;

/**
 * A line under the query editor that shows what the query returns and its parameters, or the first problem of the
 * query.
 */
class QLStatusLine extends JLabel {

    QLStatusLine() {
        setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
    }

    /**
     * Shows the state of the query after it was checked.
     *
     * @param problems the problems found in the query, empty if there are none
     */
    void update(SelectQueryDescriptor query, List<String> problems) {
        if (query == null) {
            setText(" ");
            setToolTipText(null);
            return;
        }

        if (!problems.isEmpty()) {
            String more = problems.size() > 1 ? "  (and " + (problems.size() - 1) + " more)" : "";
            setText("✗ " + problems.getFirst() + more);
            setToolTipText(problems.size() > 1 ? "<html>" + String.join("<br>", problems) + "</html>" : null);
            setForeground(color("Actions.Red", Color.RED));
            return;
        }

        Set<String> parameters = query.getParameterNames();
        String parametersText = parameters.isEmpty()
                ? "none"
                : parameters.stream().map(p -> "$" + p).collect(Collectors.joining(", "));

        setText("✓ Returns: " + resultType(query) + "    Parameters: " + parametersText);
        setToolTipText(null);
        setForeground(UIManager.getColor("Label.foreground"));
    }

    /**
     * Returns a description of the objects that a valid query returns.
     */
    static String resultType(SelectQueryDescriptor query) {
        FluentSelect<?, ?> parsed = query.getParsedQuery();
        if (query.isColumnQuery()) {
            int columns = parsed.getColumns().size();
            return columns == 1 ? "Object (1 column)" : "Object[] (" + columns + " columns)";
        }

        return switch (query.getRoot()) {
            case DbEntity entity -> "DataRow (" + entity.getName() + ")";
            case ObjEntity entity when query.isFetchingDataRows() -> "DataRow (" + entity.getName() + ")";
            case ObjEntity entity -> entity.getName();
            case null, default -> String.valueOf(query.getRoot());
        };
    }

    private static Color color(String key, Color defaultColor) {
        Color color = UIManager.getColor(key);
        return color != null ? color : defaultColor;
    }
}
