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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.swing.BoundComponent;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A superclass of CayenneModeler controllers.
 * 
 */
public abstract class CayenneController implements BoundComponent {

    private static Log logObj = LogFactory.getLog(CayenneController.class);

    protected CayenneController parent;
    protected Application application;
    protected PropertyChangeSupport propertyChangeSupport;

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

    /**
     * Returns the vie wassociated with this Controller.
     */
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
     * Returns preference domain for this component view.
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
        th.printStackTrace();

        JOptionPane.showMessageDialog(getView(),
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
        dialog.getRootPane().registerKeyboardAction(closeAction,
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

    /**
     * Fires property change event. Exists for the benefit of subclasses.
     */
    protected void firePropertyChange(
            String propertyName,
            Object oldValue,
            Object newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Adds a listener for property change events.
     */
    public void addPropertyChangeListener(
            String expression,
            PropertyChangeListener listener) {

        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }

        propertyChangeSupport.addPropertyChangeListener(expression, listener);
    }

    /**
     * Default implementation is a noop. Override to handle parent binding updates.
     */
    public void bindingUpdated(String expression, Object newValue) {
        // do nothing...
    }
}
