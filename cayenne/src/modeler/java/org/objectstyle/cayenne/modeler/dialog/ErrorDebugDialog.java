/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.modeler.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.CayenneModelerFrame;
import org.objectstyle.cayenne.modeler.util.CayenneDialog;
import org.objectstyle.cayenne.modeler.util.PanelFactory;
import org.objectstyle.cayenne.util.Util;
import org.scopemvc.util.UIStrings;

/**
 * Displays CayenneModeler exceptions and warning messages.
 * 
 * @author Andrei Adamchik
 */
public class ErrorDebugDialog extends CayenneDialog implements ActionListener {
    protected JButton close;
    protected JButton showHide;
    protected JTextArea exText = new JTextArea();
    protected JPanel exPanel;
    protected Throwable throwable;
    protected boolean detailed;

    public static void guiException(Throwable th) {
        if (th != null) {
            th.printStackTrace();
        }

        ErrorDebugDialog dialog =
            new ErrorDebugDialog(Application.getFrame(), "CayenneModeler Error", th, true);
        dialog.show();
    }

    public static void guiWarning(Throwable th, String message) {
        if (th != null) {
            th.printStackTrace();
        }

        WarningDialog dialog = new WarningDialog(Application.getFrame(), message, th, false);
        dialog.show();
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

        super(owner, title, true);

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
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
            String version = UIStrings.get("cayenne.version");
            version = (version != null) ? version : "(unknown)";

            String buildDate = UIStrings.get("cayenne.build.date");
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
