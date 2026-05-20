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
import org.apache.cayenne.modeler.log.LogAppender;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.ui.action.ShowLogConsoleAction;
import org.apache.cayenne.util.Util;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 * Modeler log console — a panel with a text area filled with log messages of the application.
 * Always docked into the main frame.
 */
public class LogConsole extends AppPanel implements LogAppender {

    private static final int TEXT_MAX_LENGTH = 500000;

    private static final MutableAttributeSet ERROR_STYLE;
    private static final MutableAttributeSet FATAL_STYLE;
    private static final MutableAttributeSet WARN_STYLE;
    private static final MutableAttributeSet INFO_STYLE;
    private static final MutableAttributeSet DEBUG_STYLE;

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

    private final JTextComponent logView;
    private final JButton clearItem;
    private final JButton copyItem;
    private final JButton closeItem;

    public LogConsole(Application app) {
        super(app);
        this.logView = new JTextPane();
        this.clearItem = new AppAction.CayenneToolbarButton(null, 0);
        this.copyItem = new AppAction.CayenneToolbarButton(null, 0);
        this.closeItem = new AppAction.CayenneToolbarButton(null, 0);

        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        JToolBar buttonsBar = new JToolBar();
        buttonsBar.setBorder(TopBorder.create());
        buttonsBar.setFloatable(false);

        copyItem.setIcon(IconFactory.buildIcon("icon-copy.png"));
        copyItem.setText("Copy");
        buttonsBar.add(copyItem);

        clearItem.setIcon(IconFactory.buildIcon("icon-trash.png"));
        clearItem.setText("Clear");
        buttonsBar.add(clearItem);

        buttonsBar.add(Box.createHorizontalGlue());

        closeItem.setIcon(IconFactory.buildIcon("icon-remove.png"));
        closeItem.setText("Close");
        buttonsBar.add(closeItem);

        add(buttonsBar, BorderLayout.NORTH);

        logView.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logView.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logView);
        scrollPane.setBorder(TopBorder.create());
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initBindings() {
        clearItem.addActionListener(e -> clear());
        copyItem.addActionListener(e -> copy());

        // Bind to the same action as the "Show log console" menu item so the menu's
        // checkbox state and the close button stay unified through Action.SELECTED_KEY.
        closeItem.setAction(app.getActionManager().getAction(ShowLogConsoleAction.class));
        closeItem.setIcon(IconFactory.buildIcon("icon-remove.png"));
        closeItem.setText("Close");
    }

    public void clear() {
        logView.setText("");
    }

    public void copy() {
        String selectedText = logView.getSelectedText();

        if (Util.isEmptyString(selectedText)) {
            Document doc = logView.getDocument();
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

    public void toggle() {
        if (new LogConsolePrefs(app.getPrefsLocator()).toggleShowConsole()) {
            appear();
        } else {
            disappear();
        }
    }

    public void showConsoleIfNeeded() {
        if (new LogConsolePrefs(app.getPrefsLocator()).isShowConsole()) {
            appear();
        }
    }

    private void appear() {
        app.getFrame().setDockComponent(this);
    }

    private void disappear() {
        app.getFrame().setDockComponent(null);
    }

    @Override
    public void appendMessage(String level, String formattedMessage) {
        Document doc = logView.getDocument();

        if (doc.getLength() > TEXT_MAX_LENGTH) {
            clear();
        }

        try {
            doc.insertString(doc.getLength(), formattedMessage, styleFor(level));
            logView.setCaretPosition(doc.getLength() - 1);
        } catch (BadLocationException ignored) {
        }
    }

    private static AttributeSet styleFor(String level) {
        return switch (level) {
            case "ERROR" -> ERROR_STYLE;
            case "FATAL" -> FATAL_STYLE;
            case "WARN" -> WARN_STYLE;
            case "INFO" -> INFO_STYLE;
            default -> DEBUG_STYLE;
        };
    }
}
