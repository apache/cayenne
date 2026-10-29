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

import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * A line under a query editor that shows the first problem of the query, or a summary of a valid query.
 */
public class QueryStatusLine extends JLabel {

    public QueryStatusLine() {
        setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        clear();
    }

    public void clear() {
        setText(" ");
        setToolTipText(null);
    }

    /**
     * Shows the first error of the query, with all of them in the tooltip.
     */
    public void showErrors(List<String> errors) {
        show("✗ ", errors, color("Actions.Red", Color.RED));
    }

    /**
     * Shows the first warning of a query that has no errors, with all of them in the tooltip.
     */
    public void showWarnings(List<String> warnings) {
        show("⚠ ", warnings, color("Actions.Yellow", Color.ORANGE.darker()));
    }

    /**
     * Shows the summary of a query that has no problems.
     */
    public void showOk(String summary) {
        setText("✓ " + summary);
        setToolTipText(null);
        setForeground(UIManager.getColor("Label.foreground"));
    }

    private void show(String mark, List<String> problems, Color color) {
        String more = problems.size() > 1 ? "  (and " + (problems.size() - 1) + " more)" : "";
        setText(mark + problems.getFirst() + more);
        setToolTipText(problems.size() > 1 ? "<html>" + String.join("<br>", problems) + "</html>" : null);
        setForeground(color);
    }

    private static Color color(String key, Color defaultColor) {
        Color color = UIManager.getColor(key);
        return color != null ? color : defaultColor;
    }
}
