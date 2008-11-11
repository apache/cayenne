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

package org.apache.cayenne.modeler.util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.ProjectController;
import org.scopemvc.view.awt.AWTUtilities;

/**
 * Superclass of CayenneModeler dialogs. Adds support for popping hyperlinks 
 * in the default system browser.
 * 
 */
public class CayenneDialog extends JDialog implements HyperlinkListener {

    public CayenneDialog() throws HeadlessException {
        super();
    }

    public CayenneDialog(Frame owner) throws HeadlessException {
        super(owner);
    }

    public CayenneDialog(Frame owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public CayenneDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public CayenneDialog(Frame owner, String title, boolean modal)
        throws HeadlessException {
        super(owner, title, modal);
    }

    public CayenneDialog(
        Frame owner,
        String title,
        boolean modal,
        GraphicsConfiguration gc) {
        super(owner, title, modal, gc);
    }

    public CayenneDialog(Dialog owner) throws HeadlessException {
        super(owner);
    }

    public CayenneDialog(Dialog owner, boolean modal) throws HeadlessException {
        super(owner, modal);
    }

    public CayenneDialog(Dialog owner, String title) throws HeadlessException {
        super(owner, title);
    }

    public CayenneDialog(Dialog owner, String title, boolean modal)
        throws HeadlessException {
        super(owner, title, modal);
    }

    public CayenneDialog(
        Dialog owner,
        String title,
        boolean modal,
        GraphicsConfiguration gc)
        throws HeadlessException {
        super(owner, title, modal, gc);
    }

    public CayenneDialog(CayenneModelerFrame frame, String title, boolean modal) {
        super(frame, title, modal);
    }

    /**
     * Makes dialog closeable when ESC button is clicked.
     */
    protected void initCloseOnEscape() {
        // make dialog closable on escape
        // TODO: Note that if a dialog contains subcomponents
        // that use ESC for their own purposes (like editable JTable or JComboBox),
        // this code will still close the dialog  (e.g. not just an expanded 
        // ComboBox). To fix it see this advise (Swing is Fun!!):
        //
        //   http://www.eos.dk/pipermail/swing/2001-June/000789.html

        KeyStroke escReleased = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);
        ActionListener closeAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (CayenneDialog.this.isVisible()) {
                    // dispatch window closing event
                    WindowEvent windowClosing =
                        new WindowEvent(CayenneDialog.this, WindowEvent.WINDOW_CLOSING);
                    CayenneDialog.super.processWindowEvent(windowClosing);
                }
            }
        };
        getRootPane().registerKeyboardAction(
            closeAction,
            escReleased,
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 
     * Centers this dialog relative to the parent Window 
     */
    public void centerWindow() {
        AWTUtilities.centreOnWindow(getParentEditor(), this);
    }

    public CayenneModelerFrame getParentEditor() {
        return (CayenneModelerFrame) super.getParent();
    }

    /** 
     * Opens hyperlink in the default browser.
     */
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            BrowserControl.displayURL(event.getURL().toExternalForm());
        }
    }

    /**
     * Returns current CayenneModeler mediator.
     */
    public ProjectController getMediator() {
        return Application.getInstance().getFrameController().getProjectController();
    }

    protected void dialogInit() {
        super.dialogInit();
        initCloseOnEscape();
    }
}
