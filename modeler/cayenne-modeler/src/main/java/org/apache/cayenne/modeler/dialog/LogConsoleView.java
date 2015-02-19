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

import org.apache.cayenne.modeler.util.ModelerUtil;

import java.awt.*;

/**
 * LogConsole is a window with text area filled with log messages of the
 * application. 
 */
public class LogConsoleView extends JPanel {
    
    /**
     * Area to be filled with log messages
     */
    JTextComponent logView;
    
    /**
     * Item which performs clearing the console output 
     */
    JMenuItem clearItem;
    
    /**
     * Item which performs copying the console output 
     */
    JMenuItem copyItem;
    
    /**
     * Item which performs docking the window, i.e. sticking it to parent 
     */
    JMenuItem dockItem;
    
    /**
     * Scrollpane containing the text area
     */
    JScrollPane scroller;
    
    /**
     * PopupMenu to choose item 
     */
    JPopupMenu menu;
    
    /**
     * Button which performs showing PopupMenu
     */
    JButton menuButton;
    
    /**
     * Constructs a new log console view component
     */
    public LogConsoleView() {
        //log console window must be non-modal
        super();
        
        init();
    }
    
    /**
     * Initializes all lays out subcomponents
     */
    protected void init() {
        setLayout(new BorderLayout(5, 5));
        
        logView = new JEditorPane("text/html", "");
        logView.setEditable(false);
        
        scroller = new JScrollPane(logView);
        add(scroller, BorderLayout.CENTER);
        
        JToolBar buttonsBar = new JToolBar();
        buttonsBar.setFloatable(false);
        
        menu = new JPopupMenu();
        copyItem = new JMenuItem("Copy");
        menu.add(copyItem);
        clearItem = new JMenuItem("Clear");
        menu.add(clearItem);
        dockItem = new JMenuItem("Dock");
        menu.add(dockItem);
        
        menu.setInvoker(this);

        Icon icon = ModelerUtil.buildIcon("popupmenu.gif");
        menuButton = new JButton(icon);
        
        buttonsBar.add(menuButton);
        add(buttonsBar, BorderLayout.NORTH);
        
        //no need to center log window
        setLocation(100, 100);
    }
    
    /**
     * @return area to be filled with log messages
     */
    protected JTextComponent getLogView() {
        return logView;
    }
    
    protected JMenuItem getCopyItem() {
        return copyItem;
    }
    
    protected JMenuItem getClearItem() {
        return clearItem;
    }
    
    protected JMenuItem getDockItem() {
        return dockItem;
    }
    
    protected JScrollPane getScroller() {
        return scroller;
    }
    
    protected AbstractButton getMenuButton() {
        return menuButton;
    }
    
    protected JPopupMenu getMenu() {
        return menu;
    }
}
