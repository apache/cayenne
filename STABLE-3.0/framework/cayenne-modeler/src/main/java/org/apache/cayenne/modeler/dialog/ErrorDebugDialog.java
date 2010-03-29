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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.util.CayenneDialog;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.util.LocalizedStringsHandler;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scopemvc.util.UIStrings;

/**
 * Displays CayenneModeler exceptions and warning messages.
 * 
 */
public class ErrorDebugDialog extends CayenneDialog implements ActionListener {
    private static Log logObj = LogFactory.getLog(ErrorDebugDialog.class);
    
    /**
     * Sole instance of error/warning dialog to disallow showing of multiple dialogs
     */
    private static ErrorDebugDialog instance;
    
    protected JButton close;
    protected JButton showHide;
    protected JTextArea exText = new JTextArea();
    protected JPanel exPanel;
    protected Throwable throwable;
    protected boolean detailed;

    /**
     * Shows an error dialog with stack trace
     */
    public static void guiException(Throwable th) {
        if (th != null) {
            logObj.error("CayenneModeler Error", th);
        }

        ErrorDebugDialog dialog =
            new ErrorDebugDialog(Application.getFrame(), "CayenneModeler Error", th, true, false);
        showDialog(dialog);
    }

    /**
     * Shows an warning dialog with stack trace
     */
    public static void guiWarning(Throwable th, String message) {
        if (th != null) {
            logObj.warn("CayenneModeler Warning", th);
        }

        WarningDialog dialog = new WarningDialog(Application.getFrame(), message, th, false, false);
        showDialog(dialog);
    }
    
    /**
     * Shows an error/warning dialog, closing existing if needed
     */
    private static void showDialog(ErrorDebugDialog dialog) {
        if (instance != null) {
            instance.dispose();
        }
        
        instance = dialog;
        dialog.setVisible(true);
    }

    /**
     * Constructor for ErrorDebugDialog.
     */
    protected ErrorDebugDialog(
    CayenneModelerFrame owner,
        String title,
        Throwable throwable,
        boolean detailed)
        throws HeadlessException {

        this(owner, title, throwable, detailed, true);
    }
    
    /**
     * Constructor for ErrorDebugDialog, allowing to specify 'modal' property
     */
    protected ErrorDebugDialog(
    CayenneModelerFrame owner,
        String title,
        Throwable throwable,
        boolean detailed,
        boolean modal)
        throws HeadlessException {

        super(owner, title, modal);

        setThrowable(Util.unwindException(throwable));
        setDetailed(detailed);
        init();
    }

    protected void init() {
        setResizable(false);

        Container pane = this.getContentPane();
        pane.setLayout(new BorderLayout());

        // info area
        JEditorPane infoText = new JEditorPane("text/html", infoHTML());
        infoText.setBackground(pane.getBackground());
        infoText.setEditable(false);
        // popup hyperlinks
        infoText.addHyperlinkListener(this);

        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoText);
        pane.add(infoPanel, BorderLayout.NORTH);

        // exception area
        if (throwable != null) {
            exText.setEditable(false);
            exText.setLineWrap(true);
            exText.setWrapStyleWord(true);
            exText.setRows(16);
            exText.setColumns(40);
            JScrollPane exScroll =
                new JScrollPane(
                    exText,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            exPanel = new JPanel();
            exPanel.setLayout(new BorderLayout());
            exPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            exPanel.add(exScroll, BorderLayout.CENTER);

            // buttons
            showHide = new JButton("");
            showHide.addActionListener(this);
            if (isDetailed()) {
                showDetails();
            } else {
                hideDetails();
            }
        }

        close = new JButton("Close");
        close.addActionListener(this);

        JButton[] buttons = (showHide != null) ? new JButton[] { showHide, close }
        : new JButton[] { close };
        pane.add(PanelFactory.createButtonPanel(buttons), BorderLayout.SOUTH);
        
        //add a listener to clear static variables, not to produce garbage
        addWindowListener(new WindowAdapter() {
           public void windowClosing(WindowEvent e) {
               instance = null;
           }
        });

        // prepare to display
        this.pack();
        this.centerWindow();
    }

    protected String infoHTML() {
        String bugreportURL = UIStrings.get("cayenne.bugreport.url");
        return "<b><font face='Arial,Helvetica' size='+1' color='red'>"
            + getTitle()
            + "</font></b><br>"
            + "<font face='Arial,Helvetica' size='-1'>Please copy the message below and "
            + "report this error by going to <br>"
            + "<a href='"
            + bugreportURL
            + "'>"
            + bugreportURL
            + "</a></font>";
    }

    protected void setThrowable(Throwable throwable) {
        this.throwable = throwable;

        String text = null;
        if (throwable != null) {
            StringWriter str = new StringWriter();
            PrintWriter out = new PrintWriter(str);

            // first add extra diagnostics
            String version = LocalizedStringsHandler.getString("cayenne.version");
            version = (version != null) ? version : "(unknown)";

            String buildDate = LocalizedStringsHandler.getString("cayenne.build.date");
            buildDate = (buildDate != null) ? buildDate : "(unknown)";

            out.println("CayenneModeler Info");
            out.println("Version: " + version);
            out.println("Build Date: " + buildDate);
            out.println("Exception: ");
            out.println("=================================");
            buildStackTrace(out, throwable);

            try {
                out.close();
                str.close();
            } catch (IOException ioex) {
                // this should never happen
            }
            text = str.getBuffer().toString();
        }

        exText.setText(text);
    }

    protected void buildStackTrace(PrintWriter out, Throwable th) {
        if (th == null) {
            return;
        }

        th.printStackTrace(out);

        Throwable cause = th.getCause();
        if (cause != null) {
            out.println("Caused by:");
            buildStackTrace(out, cause);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == close) {
            this.dispose();
        } else if (e.getSource() == showHide) {
            if (isDetailed()) {
                hideDetails();
            } else {
                showDetails();
            }
            this.pack();
            this.centerWindow();
        }
    }

    protected void hideDetails() {
        getContentPane().remove(exPanel);
        showHide.setText("Show Details");
        setDetailed(false);
    }

    protected void showDetails() {
        getContentPane().add(exPanel, BorderLayout.CENTER);
        showHide.setText("Hide Details");
        setDetailed(true);
    }

    /**
     * Returns the detailed.
     * @return boolean
     */
    public boolean isDetailed() {
        return detailed;
    }

    /**
     * Sets the detailed.
     * @param detailed The detailed to set
     */
    public void setDetailed(boolean detailed) {
        this.detailed = detailed;
    }
}
