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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.ErrorDebugDialog;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.ProjectPath;

/**
 * Superclass of CayenneModeler actions that implements support for some common
 * functionality, exception handling, etc.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayenneAction extends AbstractAction {

    protected boolean alwaysOn;
    protected Application application;

    /**
     * Creates a named CayenneAction.
     */
    public CayenneAction(String name, Application application) {
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

        setEnabled(false);
    }

    public Application getApplication() {
        return application;
    }

    protected Project getCurrentProject() {
        return application
                .getFrameController()
                .getProjectController()
                .getProject();
    }

    /**
     * Changes the name of this action, propagating the change to all widgets using this
     * action.
     */
    public void setName(String newName) {
        super.putValue(Action.NAME, newName);
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
        public String getText() {
            return (showingText) ? super.getText() : null;
        }

        /**
         * @see javax.swing.AbstractButton#setText(String)
         */
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