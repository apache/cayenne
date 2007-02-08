/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.objectstyle.cayenne.modeler.pref.FSPath;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.control.FileChooser;
import org.objectstyle.cayenne.util.Util;

public class TemplateCreator extends CayenneController {

    protected TemplateCreatorView view;
    protected boolean canceled;
    protected Set existingNames;
    protected PreferenceEditor editor;
    protected Domain domain;

    public TemplateCreator(TemplatePreferences parent) {
        super(parent);

        JDialog parentDialog = (JDialog) SwingUtilities.getAncestorOfClass(
                JDialog.class,
                parent.getView());
        this.view = new TemplateCreatorView(parentDialog);
        this.existingNames = new HashSet();
        this.editor = parent.getEditor();
        this.domain = parent.getTemplateDomain();

        Iterator it = parent.getTemplateEntries().iterator();
        while (it.hasNext()) {
            FSPath path = (FSPath) it.next();
            existingNames.add(path.getKey());
        }
        initBindings();
    }

    public Component getView() {
        return view;
    }

    FSPath getLastTemplateDirectory() {
        // find start directory in preferences

        FSPath path = (FSPath) getViewDomain().getDetail(
                "lastTemplate",
                FSPath.class,
                true);

        if (path.getPath() == null) {
            path.setPath(getLastDirectory().getPath());
        }

        return path;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getOkButton(), "okAction()");

        final FSPath path = getLastTemplateDirectory();
        view.getTemplateChooser().setCurrentDirectory(path.getExistingDirectory(false));
        view.getTemplateChooser().addPropertyChangeListener(
                FileChooser.CURRENT_DIRECTORY_PROPERTY,
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        File directory = view.getTemplateChooser().getCurrentDirectory();
                        path.setDirectory(directory);
                    }
                });
    }

    public void okAction() {
        String templateName = view.getTemplateName().getText();

        if (Util.isEmptyString(templateName)) {
            JOptionPane.showMessageDialog(
                    view,
                    "Enter Template Name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else if (existingNames.contains(templateName)) {
            JOptionPane.showMessageDialog(
                    view,
                    "'" + templateName + "' is already taken, enter a different name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else if (view.getTemplateChooser().getFile() == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Must select an existing template file",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else {
            canceled = false;
            view.dispose();
        }
    }

    public void cancelAction() {
        canceled = true;
        view.dispose();
    }

    /**
     * Pops up a dialog and blocks current thread until the dialog is closed.
     */
    public FSPath startupAction() {
        // this should handle closing via ESC
        canceled = true;

        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();

        view.setVisible(true);
        return createTemplate();
    }

    protected FSPath createTemplate() {
        if (canceled) {
            return null;
        }

        String key = view.getTemplateName().getText();
        File file = view.getTemplateChooser().getFile();
        FSPath path = (FSPath) editor.createDetail(domain, key, FSPath.class);
        path.setPath(file != null ? file.getAbsolutePath() : null);
        return path;
    }
}
