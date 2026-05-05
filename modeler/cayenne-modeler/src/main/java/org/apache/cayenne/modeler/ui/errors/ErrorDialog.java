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


package org.apache.cayenne.modeler.ui.errors;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppDialog;
import org.apache.cayenne.modeler.toolkit.buttons.CMButtonPanel;
import org.apache.cayenne.modeler.toolkit.url.UrlOpener;
import org.apache.cayenne.util.LocalizedStringsHandler;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorDialog extends AppDialog {

    private static final String BUGREPORT_URL = "https://issues.apache.org/jira/browse/CAY";

    public ErrorDialog(Application app, String title, Throwable throwable) {

        super(app, app.getFrame(), title, ModalityType.MODELESS);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        setResizable(false);

        Container pane = this.getContentPane();
        pane.setLayout(new BorderLayout());

        JEditorPane infoText = new JEditorPane("text/html", infoHTML(title));
        infoText.setBackground(pane.getBackground());
        infoText.setEditable(false);
      
        infoText.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                UrlOpener.displayURL(e.getURL().toExternalForm());
            }
        });

        JPanel infoPanel = new JPanel();
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoText);
        pane.add(infoPanel, BorderLayout.NORTH);


        if (throwable != null) {
            JTextArea exText = new JTextArea();

            exText.setText(throwableText(throwable));

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

            JPanel exPanel = new JPanel();
            exPanel.setLayout(new BorderLayout());
            exPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            exPanel.add(exScroll, BorderLayout.CENTER);
            getContentPane().add(exPanel, BorderLayout.CENTER);
        }

        getRootPane().setDefaultButton(close);
        pane.add(new CMButtonPanel(close), BorderLayout.SOUTH);
    }

    private static String infoHTML(String title) {
        return "<b><font face='Arial,Helvetica' size='+1' color='red'>"
                + title
                + "</font></b><br>"
                + "<font face='Arial,Helvetica' size='-1'>Please copy the message below and "
                + "report this error by going to <br>"
                + "<a href='"
                + BUGREPORT_URL
                + "'>"
                + BUGREPORT_URL
                + "</a></font>";
    }

    private static String throwableText(Throwable throwable) {

        if (throwable == null) {
            return null;
        }

        throwable = Util.unwindException(throwable);

        StringWriter str = new StringWriter();
        PrintWriter out = new PrintWriter(str);

        // first add extra diagnostics

        out.println("CayenneModeler Info");
        out.println("Version: " + LocalizedStringsHandler.getString("cayenne.version"));
        out.println("Build Date: " + LocalizedStringsHandler.getString("cayenne.build.date"));
        out.println("Exception: ");
        out.println("=================================");
        buildStackTrace(out, throwable);

        try {
            out.close();
            str.close();
        } catch (IOException ioex) {
            // this should never happen
        }
        return str.getBuffer().toString();
    }

    private static void buildStackTrace(PrintWriter out, Throwable th) {
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
}
