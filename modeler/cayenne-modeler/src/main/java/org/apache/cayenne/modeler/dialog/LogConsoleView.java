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

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.components.TopBorder;

import java.awt.*;

/**
 * LogConsole is a window with text area filled with log messages of the
 * application. 
 */
public class LogConsoleView extends JPanel {

    static private final Icon DOCK_ICON = ModelerUtil.buildIcon("icon-down.png");
    static private final Icon UNDOCK_ICON = ModelerUtil.buildIcon("icon-up.png");

    /**
     * Area to be filled with log messages
     */
    private JTextComponent logView;
    
    /**
     * Item which performs clearing the console output 
     */
    private JButton clearItem;
    
    /**
     * Item which performs copying the console output 
     */
    private JButton copyItem;
    
    /**
     * Item which performs docking the window, i.e. sticking it to parent 
     */
    private JButton dockItem;

    private JToolBar buttonsBar;
    
    /**
     * Constructs a new log console view component
     */
    public LogConsoleView() {
        //log console window must be non-modal
        super();
        init();
    }
    
    /**
     * Initializes and lays out subcomponents
     */
    protected void init() {
        setLayout(new BorderLayout());

        buttonsBar = new JToolBar();
        buttonsBar.setBorder(BorderFactory.createEmptyBorder());
        buttonsBar.setFloatable(false);

        copyItem = new CayenneAction.CayenneToolbarButton(null, 0);
        copyItem.setIcon(ModelerUtil.buildIcon("icon-copy.png"));
        copyItem.setText("Copy");
        buttonsBar.add(copyItem);

        clearItem = new CayenneAction.CayenneToolbarButton(null, 0);
        clearItem.setIcon(ModelerUtil.buildIcon("icon-trash.png"));
        clearItem.setText("Clear");
        buttonsBar.add(clearItem);

        dockItem = new CayenneAction.CayenneToolbarButton(null, 0);
        setDocked(false);
        buttonsBar.add(dockItem);

        add(buttonsBar, BorderLayout.NORTH);

        logView = new JEditorPane("text/html", "");
        logView.setFont(new JLabel().getFont().deriveFont(12f));
        logView.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logView);
        scrollPane.setBorder(TopBorder.create());
        add(scrollPane, BorderLayout.CENTER);
        
        //no need to center log window
        setLocation(100, 100);
    }
    
    /**
     * @return area to be filled with log messages
     */
    JTextComponent getLogView() {
        return logView;
    }
    
    JButton getCopyItem() {
        return copyItem;
    }
    
    JButton getClearItem() {
        return clearItem;
    }
    
    JButton getDockItem() {
        return dockItem;
    }

    void setDocked(boolean isDocked) {
        if(isDocked) {
            dockItem.setIcon(UNDOCK_ICON);
            dockItem.setText("Undock");
            buttonsBar.setBorder(TopBorder.create());
        } else {
            dockItem.setIcon(DOCK_ICON);
            dockItem.setText("Dock");
            buttonsBar.setBorder(BorderFactory.createEmptyBorder());
        }
    }
}
