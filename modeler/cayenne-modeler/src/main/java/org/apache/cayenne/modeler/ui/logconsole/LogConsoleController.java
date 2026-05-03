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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.action.ShowLogConsoleAction;
import org.apache.cayenne.util.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * Implementation for modeler log console functionality
 */
public class LogConsoleController extends RootController {
    /**
     * How much characters are allowed in console
     */
    private static final int TEXT_MAX_LENGTH = 500000;

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
        StyleConstants.setForeground(INFO_STYLE, new Color(32, 65, 150));

        DEBUG_STYLE = new SimpleAttributeSet();
        StyleConstants.setForeground(DEBUG_STYLE, Color.GRAY);
    }

    private final LogConsoleView view;
    private boolean loggingStopped;

    public LogConsoleController(Application application) {
        super(application);

        view = new LogConsoleView();

        initBindings();
    }

    protected void initBindings() {
        view.getClearItem().addActionListener(e -> clear());
        view.getCopyItem().addActionListener(e -> copy());

        // Bind to the same action as the "Show log console" menu item so the menu's
        // checkbox state and the close button stay unified through Action.SELECTED_KEY.
        view.getCloseItem().setAction(application.getActionManager().getAction(ShowLogConsoleAction.class));
        view.getCloseItem().setIcon(IconFactory.buildIcon("icon-remove.png"));
        view.getCloseItem().setText("Close");
    }

    public void clear() {
        view.getLogView().setText("");
    }

    /**
     * Shows the console docked into the main frame.
     */
    private void appear() {
        application.getFrameController().getView().setDockComponent(view);
    }

    /**
     * Hides the console.
     */
    private void disappear() {
        application.getFrameController().getView().setDockComponent(null);
    }

    /**
     * Copies selected text from the console to system buffer
     */
    public void copy() {
        String selectedText = view.getLogView().getSelectedText();

        // If nothing is selected, we copy the whole text
        if (Util.isEmptyString(selectedText)) {
            Document doc = view.getLogView().getDocument();
            try {
                selectedText = doc.getText(0, doc.getLength());
            } catch (BadLocationException e) {
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
        if (LogConsolePrefs.of(application.getPreferencesRepository()).toggleShowConsole()) {
            appear();
        } else {
            disappear();
        }
    }

    /**
     * Shows the console if the show-console preference is set to true
     */
    public void showConsoleIfNeeded() {
        if (LogConsolePrefs.of(application.getPreferencesRepository()).isShowConsole()) {
            appear();
        }
    }

    /**
     * Appends a message and/or an exception
     */
    public void appendMessage(String level, String message, Throwable ex, AttributeSet style) {

        if (loggingStopped) {
            return;
        }

        Document doc = view.getLogView().getDocument();

        //truncate if needed
        if (doc.getLength() > TEXT_MAX_LENGTH) {
            clear();
        }

        StringBuilder newText = new StringBuilder(FORMAT.format(new Date()))
                //.append(System.getProperty("line.separator"))
                .append(" ").append(level.toUpperCase()).append(": ");

        if (message != null) {
            // Append the message
            newText.append(message).append(System.lineSeparator());
        }

        if (ex != null) {
            // Append the stack trace
            StringWriter out = new StringWriter();
            PrintWriter printer = new PrintWriter(out);

            ex.printStackTrace(printer);
            printer.flush();

            newText.append(out).append(System.lineSeparator());
        }

        try {
            doc.insertString(doc.getLength(), newText.toString(), style);
            view.getLogView().setCaretPosition(doc.getLength() - 1);

        } catch (BadLocationException ignored) {
            //should not happen
        }
    }

    @Override
    public Component getView() {
        return view;
    }

    /**
     * Stop logging and don't print any more messages to text area
     */
    public void stopLogging() {
        loggingStopped = true;
    }
}
