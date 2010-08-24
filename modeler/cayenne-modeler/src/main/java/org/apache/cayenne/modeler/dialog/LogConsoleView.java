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
     * Button which performs clearing the console output 
     */
    JButton clearButton;
    
    /**
     * Button which performs copying the console output 
     */
    JButton copyButton;
    
    /**
     * Button which performs docking the window, i.e. sticking it to parent 
     */
    JButton dockButton;
    
    /**
     * Scrollpane containing the text area
     */
    JScrollPane scroller;
    
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
        
        JPanel buttonsPane = new JPanel();
        
        copyButton = new JButton("Copy");
        clearButton = new JButton("Clear");
        dockButton = new JButton("Dock");
        
        buttonsPane.add(clearButton);
        buttonsPane.add(copyButton);
        buttonsPane.add(dockButton);
        add(buttonsPane, BorderLayout.SOUTH);
        
        //no need to center log window
        setLocation(100, 100);
    }
    
    /**
     * @return area to be filled with log messages
     */
    protected JTextComponent getLogView() {
        return logView;
    }
    
    protected AbstractButton getCopyButton() {
        return copyButton;
    }
    
    protected AbstractButton getClearButton() {
        return clearButton;
    }
    
    protected AbstractButton getDockButton() {
        return dockButton;
    }
    
    protected JScrollPane getScroller() {
        return scroller;
    }
}
