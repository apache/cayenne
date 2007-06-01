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
package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.util.Util;

/**
 * A superclass of CayenneModeler controllers.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayenneController {

    private static final Logger logObj = Logger.getLogger(CayenneController.class);

    protected CayenneController parent;
    protected Application application;

    public CayenneController(CayenneController parent) {
        this.application = (parent != null) ? parent.getApplication() : null;
        this.parent = parent;
    }

    public CayenneController(Application application) {
        this.application = application;
    }

    public Application getApplication() {
        return application;
    }

    public CayenneController getParent() {
        return parent;
    }

    public abstract Component getView();

    /**
     * Returns last file system directory visited by user for this component. If there is
     * no such directory set up in the preferences, creates a new object, setting its path
     * to the parent last directory or to the user HOME directory.
     */
    public FSPath getLastDirectory() {
        // find start directory in preferences
        FSPath path = (FSPath) getViewDomain().getDetail("lastDir", FSPath.class, true);

        if (path.getPath() == null) {

            String pathString = (getParent() != null) ? getParent()
                    .getLastDirectory()
                    .getPath() : System.getProperty("user.home");
            path.setPath(pathString);
        }

        return path;
    }

    /**
     * Returns preference domaing for this component view.
     */
    protected Domain getViewDomain() {
        return getApplication().getPreferenceDomain().getSubdomain(getView().getClass());
    }

    /**
     * Utility method to provide a visual indication an execution error. This
     * implementation logs an error and pops up a dialog window with error message.
     */
    protected void reportError(String title, Throwable th) {
        th = Util.unwindException(th);
        logObj.info("Error in " + getClass().getName(), th);

        JOptionPane.showMessageDialog(
                getView(),
                th.getMessage(),
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Centers view on parent window.
     */
    protected void centerView() {
        Window parentWindow = parent.getWindow();

        Dimension parentSize = parentWindow.getSize();
        Dimension windowSize = getView().getSize();
        Point parentLocation = new Point(0, 0);
        if (parentWindow.isShowing()) {
            parentLocation = parentWindow.getLocationOnScreen();
        }

        int x = parentLocation.x + parentSize.width / 2 - windowSize.width / 2;
        int y = parentLocation.y + parentSize.height / 2 - windowSize.height / 2;

        getView().setLocation(x, y);
    }

    /**
     * If this view or a parent view is a JDialog, makes it closeable on ESC hit. Dialog
     * "defaultCloseOperation" property is taken into account when processing ESC button
     * click.
     */
    protected void makeCloseableOnEscape() {

        Window window = getWindow();
        if (!(window instanceof JDialog)) {
            return;
        }

        final JDialog dialog = (JDialog) window;

        KeyStroke escReleased = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);
        ActionListener closeAction = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (dialog.isVisible()) {
                    switch (dialog.getDefaultCloseOperation()) {
                        case JDialog.HIDE_ON_CLOSE:
                            dialog.setVisible(false);
                            break;
                        case JDialog.DISPOSE_ON_CLOSE:
                            dialog.setVisible(false);
                            dialog.dispose();
                            break;
                        case JDialog.DO_NOTHING_ON_CLOSE:
                        default:
                            break;
                    }
                }
            }
        };
        dialog.getRootPane().registerKeyboardAction(
                closeAction,
                escReleased,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Finds a Window in the view hierarchy.
     */
    public Window getWindow() {
        Component view = getView();
        while (view != null) {
            if (view instanceof Window) {
                return (Window) view;
            }

            view = view.getParent();
        }

        return null;
    }

    /**
     * Finds a JFrame in the view hierarchy.
     */
    public JFrame getFrame() {
        Component view = getView();
        while (view != null) {
            if (view instanceof JFrame) {
                return (JFrame) view;
            }

            view = view.getParent();
        }

        return null;
    }
}