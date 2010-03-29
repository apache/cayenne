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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.util.Util;

import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * Implementation for modeler log console functionality
 */
public class LogConsole extends CayenneController {
    /**
     * How much characters are allowed in console
     */
    private static final int TEXT_MAX_LENGTH = 500000;
    
    /**
     * Property to store user preference
     */
    public static final String SHOW_CONSOLE_PROPERTY = "show.log.console";
    
    /**
     * Property to store 'is-docked' preference
     */
    public static final String DOCKED_PROPERTY = "log.console.docked";
    
    /**
     * Message date format 
     */
    private static final DateFormat FORMAT = DateFormat.getDateTimeInstance();
    
    /**
     * Red color style for severe messages
     */
    public static final MutableAttributeSet ERROR_STYLE;
    
    /**
     * Red bold color style for fatal messages
     */
    public static final MutableAttributeSet FATAL_STYLE;
    
    /**
     * Dark red color style for warn messages
     */
    public static final MutableAttributeSet WARN_STYLE;
    
    /**
     * Blue color style for info messages
     */
    public static final MutableAttributeSet INFO_STYLE;
    
    /**
     * Style for debug messages
     */
    public static final MutableAttributeSet DEBUG_STYLE;
    
    static {
        ERROR_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(ERROR_STYLE, Color.RED);
        
        FATAL_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(FATAL_STYLE, Color.RED);
        StyleConstants.setBold(FATAL_STYLE, true);
        
        WARN_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(WARN_STYLE, Color.RED.darker());
        
        INFO_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(INFO_STYLE, Color.BLUE);
        
        DEBUG_STYLE = null;
    }
    
    /**
     * Lone log console instance
     */
    private static LogConsole instance;
    
    /**
     * Swing console window
     */
    private LogConsoleView view;
    
    /**
     * Window, which contains the console. Might be non-visible, if the console is docked
     */
    private LogConsoleWindow logWindow;
        
    /**
     * Whether auto-scrolling should be enabled for the console text area.
     * Currently not included in UI
     */
    private boolean autoScroll;
    
    /**
     * Flag, indicating that no more logging to Swing component is appreciated.
     * This is useful to prevent logging messages when they are no more needed, e.g. on
     * JVM shutdown 
     */
    private boolean loggingStopped;
    
    public LogConsole() {
        super((CayenneController) null);
        
        view = new LogConsoleView();
        autoScroll = true;
        
        initBindings();
    }
    
    protected void initBindings() {
        view.getClearButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        
        view.getCopyButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copy();
            }
        });
        
        view.getDockButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /**
                 * Log console should be visible
                 */
                
                disappear();
                setConsoleProperty(DOCKED_PROPERTY, !getConsoleProperty(DOCKED_PROPERTY));
                appear();
            }
        });
    }
    
    /**
     * Clears the console
     */
    public void clear() {
        view.getLogView().setText("");
    }
        
    /**
     * Shows the console, in separate window or in main frame 
     */
    private void appear() {
        if (!getConsoleProperty(DOCKED_PROPERTY)) {
            view.getDockButton().setText("Dock");
            
            if (logWindow == null) {
                logWindow = new LogConsoleWindow(this);
            
                Domain prefDomain = getDomain();
                ComponentGeometry geometry = ComponentGeometry.getPreference(prefDomain);
                geometry.bind(logWindow, 600, 300, 0);
            }
            
            logWindow.setContentPane(view);
            
            logWindow.validate();
            logWindow.setVisible(true);
        }
        else {
            view.getDockButton().setText("Undock");
            Application.getFrame().setDockComponent(view);
        }
    }
    
    /**
     * Hides the console 
     */
    private void disappear() {
        if (!getConsoleProperty(DOCKED_PROPERTY)) {
            logWindow.dispose();
            logWindow = null;
        }
        else {
            Application.getFrame().setDockComponent(null);
        }
    }
    
    /**
     * Copies selected text from the console to system buffer
     */
    public void copy() {
        String selectedText = view.getLogView().getSelectedText();
        
        /**
         * If nothing is selected, we copy the whole text
         */
        if (Util.isEmptyString(selectedText)) {
            Document doc = view.getLogView().getDocument();
            
            try {
                selectedText = doc.getText(0, doc.getLength());
            }
            catch (BadLocationException e) {
                return;
            }
        }
        
       Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
       StringSelection data = new StringSelection(selectedText); 
       
       sysClip.setContents(data, data);
    }
    
    /**
     * Shows or hides the console window
     */
    public void toggle() {
        boolean needShow = !getConsoleProperty(SHOW_CONSOLE_PROPERTY);
        setConsoleProperty(SHOW_CONSOLE_PROPERTY, needShow);
        
        if (needShow) {
            appear();
        }
        else {
            disappear();
        }
    }
    
    /**
     * Shows the console if the preference 'SHOW_CONSOLE_PROPERTY' is set to true 
     */
    public void showConsoleIfNeeded() {
        if (getConsoleProperty(SHOW_CONSOLE_PROPERTY)) {
            appear();
        }
    }
    
    /**
     * Sets the property, depending on last user's choice
     */
    public void setConsoleProperty(String prop, boolean b) {
        getDomain().getDetail(prop, true).
            setBooleanProperty(prop, b);
    }
    
    /**
     * @return a boolean property
     */
    public boolean getConsoleProperty(String prop) {
        return getDomain().getDetail(prop, true).getBooleanProperty(prop);
    }
    
    /**
     * Appends a message to the console.
     * @param style Message font, color etc.
     */
    public void appendMessage(String level, String message, AttributeSet style) {
        appendMessage(message, null, style);
    }
    
    /**
     * Appends a message and (or) an exception
     * @param style Message font, color etc.
     */
    public void appendMessage(String level, String message, Throwable ex, 
        AttributeSet style) {
        
        if (loggingStopped) {
            return;
        }
        
        Document doc = view.getLogView().getDocument(); 
            
        //truncate if needed
        if (doc.getLength() > TEXT_MAX_LENGTH) {
            clear();
        }
        
        StringBuffer newText = new StringBuffer(FORMAT.format(new Date()))
            .append(System.getProperty("line.separator"))
            .append(level.toUpperCase() + ": ");
        
        if (message != null) {
            /**
             * Append the message
             */
            newText.append(message).append(System.getProperty("line.separator"));
        }
        
        if (ex != null) {
            /**
             * Append the stack trace
             */
            StringWriter out = new StringWriter();
            PrintWriter printer = new PrintWriter(out);
            
            ex.printStackTrace(printer);
            printer.flush();
            
            newText.append(out.toString()).append(System.getProperty("line.separator"));
        }
        
        try {
            doc.insertString(doc.getLength(), newText.toString(), style);

            //view.getLogView().setText(view.getLogView().getText() + newText);
            
            if (autoScroll) {
                view.getLogView().setCaretPosition(
                        doc.getLength() - 1);
            }
        }
        catch (BadLocationException ignored) {
            //should not happen
        }
        
    }

    @Override
    public Component getView() {
        return view;
    }
    
    protected Domain getDomain() {
        return Application.getInstance().getPreferenceDomain().getSubdomain(getClass());
    }
    
    /**
     * Stop logging and don't print any more messages to text area
     */
    public void stopLogging() {
        loggingStopped = true;
    }
    
    /**
     * @return lone log console instance
     */
    public static LogConsole getInstance() {
        if (instance == null) {
            instance = new LogConsole();
        }
        
        return instance;
    }
}
