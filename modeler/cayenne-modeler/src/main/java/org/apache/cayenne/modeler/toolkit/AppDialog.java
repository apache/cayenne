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

package org.apache.cayenne.modeler.toolkit;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Base for modeler dialogs that merge presentation and logic into a single component.
 * Holds the {@link Application} reference and provides the small set of utilities
 * dialogs typically need: centering on owner, ESC-to-close, error reporting.
 */
public abstract class AppDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDialog.class);

    protected final Application app;

    protected AppDialog(Application app, Window owner, String title, ModalityType modality) {
        super(owner, title, modality);
        this.app = app;
    }

    protected void centerOnOwner() {
        Window owner = getOwner();
        if (owner == null) {
            return;
        }

        Dimension ownerSize = owner.getSize();
        Dimension size = getSize();
        Point ownerLocation = owner.isShowing() ? owner.getLocationOnScreen() : new Point(0, 0);

        int x = ownerLocation.x + ownerSize.width / 2 - size.width / 2;
        int y = ownerLocation.y + ownerSize.height / 2 - size.height / 2;
        setLocation(x, y);
    }

    /**
     * Closes the dialog on ESC, honoring the dialog's defaultCloseOperation.
     */
    protected void makeCloseableOnEscape() {
        KeyStroke escReleased = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);
        ActionListener closeAction = e -> {
            if (!isVisible()) {
                return;
            }
            switch (getDefaultCloseOperation()) {
                case JDialog.HIDE_ON_CLOSE:
                    setVisible(false);
                    break;
                case JDialog.DISPOSE_ON_CLOSE:
                    setVisible(false);
                    dispose();
                    break;
                case JDialog.DO_NOTHING_ON_CLOSE:
                default:
                    break;
            }
        };
        getRootPane().registerKeyboardAction(closeAction, escReleased, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Standard dialog launch: pack, set dispose-on-close, center on owner, wire ESC, show.
     * Blocks until the dialog is closed for modal dialogs.
     */
    public void open() {
        pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        centerOnOwner();
        makeCloseableOnEscape();
        setVisible(true);
    }

    /**
     * Logs the error and shows a modal error dialog anchored on this dialog.
     */
    protected void reportError(String title, Throwable t) {
        Throwable unwound = Util.unwindException(t);
        LOGGER.info("Error in " + getClass().getName(), unwound);
        JOptionPane.showMessageDialog(this, unwound.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
