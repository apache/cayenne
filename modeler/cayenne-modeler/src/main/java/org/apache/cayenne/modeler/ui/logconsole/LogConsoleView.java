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
package org.apache.cayenne.modeler.ui.logconsole;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;

import java.awt.*;

/**
 * LogConsole is a panel with a text area filled with log messages of the
 * application. Always docked into the main frame.
 */
public class LogConsoleView extends JPanel {

    private JTextComponent logView;
    private JButton clearItem;
    private JButton copyItem;
    private JButton closeItem;

    public LogConsoleView() {
        super();
        init();
    }

    protected void init() {
        setLayout(new BorderLayout());

        JToolBar buttonsBar = new JToolBar();
        buttonsBar.setBorder(TopBorder.create());
        buttonsBar.setFloatable(false);

        copyItem = new ModelerAbstractAction.CayenneToolbarButton(null, 0);
        copyItem.setIcon(IconFactory.buildIcon("icon-copy.png"));
        copyItem.setText("Copy");
        buttonsBar.add(copyItem);

        clearItem = new ModelerAbstractAction.CayenneToolbarButton(null, 0);
        clearItem.setIcon(IconFactory.buildIcon("icon-trash.png"));
        clearItem.setText("Clear");
        buttonsBar.add(clearItem);

        buttonsBar.add(Box.createHorizontalGlue());

        // Icon/tooltip and the bound action are configured by LogConsoleController, since the
        // close button shares ShowLogConsoleAction with the Tools menu item.
        closeItem = new ModelerAbstractAction.CayenneToolbarButton(null, 0);
        buttonsBar.add(closeItem);

        add(buttonsBar, BorderLayout.NORTH);

        logView = new JEditorPane("text/html", "");
        logView.setFont(new JLabel().getFont().deriveFont(12f));
        logView.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logView);
        scrollPane.setBorder(TopBorder.create());
        add(scrollPane, BorderLayout.CENTER);
    }

    JTextComponent getLogView() {
        return logView;
    }

    JButton getCopyItem() {
        return copyItem;
    }

    JButton getClearItem() {
        return clearItem;
    }

    JButton getCloseItem() {
        return closeItem;
    }
}
