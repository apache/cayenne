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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 * Superclass of CayenneModeler actions that implements support for some common
 * functionality, exception handling, etc.
 * 
 */
public abstract class CayenneAction extends AbstractAction {

    protected boolean alwaysOn;
    protected Application application;

    /**
     * Creates a named CayenneAction.
     */
    public CayenneAction(String name, Application application) {
        this(name, application, name);
    }

    /**
     * Creates a named CayenneAction.
     */
    public CayenneAction(String name, Application application, String shortDescription) {
        super(name);
        super.putValue(Action.DEFAULT, name);

        this.application = application;

        Icon icon = createIcon();
        if (icon != null) {
            super.putValue(Action.SMALL_ICON, icon);
        }

        KeyStroke accelerator = getAcceleratorKey();
        if (accelerator != null) {
            super.putValue(Action.ACCELERATOR_KEY, accelerator);
        }
        
        if (shortDescription != null && shortDescription.length() > 0) {
            super.putValue(Action.SHORT_DESCRIPTION, shortDescription);
        }

        setEnabled(false);
    }

    public Application getApplication() {
        return application;
    }

    protected Project getCurrentProject() {
        return application.getFrameController().getProjectController().getProject();
    }

    /**
     * Changes the name of this action, propagating the change to all widgets using this
     * action.
     */
    public void setName(String newName) {
        if (!Util.nullSafeEquals(getValue(Action.NAME), newName)) {
            super.putValue(Action.NAME, newName);
        }
    }

    /**
     * Returns keyboard shortcut for this action. Default implementation returns
     * <code>null</code>.
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * Returns the name of the icon that should be used for buttons. Name will be reolved
     * relative to <code>RESOURCE_PATH</code>. Default implementation returns
     * <code>null</code>.
     */
    public String getIconName() {
        return null;
    }

    /**
     * Creates and returns an ImageIcon that can be used for buttons that rely on this
     * action. Returns <code>null</code> if <code>getIconName</code> returns
     * <code>null</code>.
     */
    public Icon createIcon() {
        String name = getIconName();
        return (name != null) ? ModelerUtil.buildIcon(name) : null;
    }

    /**
     * Returns the key under which this action should be stored in the ActionMap.
     */
    public String getKey() {
        return (String) super.getValue(Action.DEFAULT);
    }

    /**
     * Subclasses must implement this method instead of <code>actionPerformed</code> to
     * allow for exception handling.
     */
    public abstract void performAction(ActionEvent e);

    /**
     * Returns <code>true</code> if the action is enabled for the specified "project
     * path" - a path on the project tree to a currently selected object. Default
     * implementation simply returns <code>false</code>.
     */
    public boolean enableForPath(ProjectPath obj) {
        return false;
    }

    /**
     * Returns current project controller.
     */
    public ProjectController getProjectController() {
        return application.getFrameController().getProjectController();
    }

    /**
     * Internally calls <code>performAction</code>. Traps exceptions that ocurred
     * during action processing.
     */
    public void actionPerformed(ActionEvent e) {
        try {
            performAction(e);
        }
        catch (Throwable th) {
            ErrorDebugDialog.guiException(th);
        }
    }

    /**
     * Factory method that creates a menu item hooked up to this action.
     */
    public JMenuItem buildMenu() {
        return new JMenuItem(this);
    }
    
    /**
     * Factory method that creates a checkbox menu item hooked up to this action.
     */
    public JCheckBoxMenuItem buildCheckBoxMenu() {
        return new JCheckBoxMenuItem(this);
    }

    /**
     * Factory method that creates a button hooked up to this action.
     */
    public JButton buildButton() {
        return new CayenneToolbarButton(this);
    }

    /**
     * Returns true if this action is always enabled.
     * 
     * @return boolean
     */
    public boolean isAlwaysOn() {
        return alwaysOn;
    }

    /**
     * Sets the alwaysOn.
     * 
     * @param alwaysOn The alwaysOn to set
     */
    public void setAlwaysOn(boolean alwaysOn) {
        this.alwaysOn = alwaysOn;

        if (alwaysOn) {
            super.setEnabled(true);
        }
    }

    /**
     * Overrides super implementation to take into account "alwaysOn" flag.
     */
    @Override
    public void setEnabled(boolean b) {
        if (!isAlwaysOn()) {
            super.setEnabled(b);
        }
    }

    /**
     * On changes in action text, will update toolbar tip instead.
     */
    final class CayenneToolbarButton extends JButton {

        protected boolean showingText;

        /**
         * Constructor for CayenneMenuItem.
         */
        public CayenneToolbarButton(Action a) {
            super();
            setAction(a);
        }

        /**
         * Returns the showingText.
         */
        public boolean isShowingText() {
            return showingText;
        }

        /**
         * Sets the showingText.
         */
        public void setShowingText(boolean showingText) {
            this.showingText = showingText;
        }

        /**
         * @see javax.swing.AbstractButton#getText()
         */
        @Override
        public String getText() {
            return (showingText) ? super.getText() : null;
        }

        /**
         * @see javax.swing.AbstractButton#setText(String)
         */
        @Override
        public void setText(String text) {
            if (showingText) {
                super.setText(text);
            }
            else {
                super.setToolTipText(text);
            }
        }
    }
}
