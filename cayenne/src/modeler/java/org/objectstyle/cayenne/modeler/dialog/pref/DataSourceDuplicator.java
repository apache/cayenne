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
package org.objectstyle.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.util.Map;

import javax.swing.JOptionPane;

import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.pref.PreferenceEditor;
import org.objectstyle.cayenne.swing.BindingBuilder;

/**
 * @author Andrei Adamchik
 */
public class DataSourceDuplicator extends CayenneController {

    protected DataSourceDuplicatorView view;
    protected PreferenceEditor editor;
    protected Domain domain;
    protected boolean canceled;
    protected Map dataSources;
    protected String prototypeKey;

    public DataSourceDuplicator(DataSourcePreferences parent, String prototypeKey) {
        super(parent);
        this.view = new DataSourceDuplicatorView("Create a copy of \""
                + prototypeKey
                + "\"");
        this.editor = parent.getEditor();
        this.domain = parent.getDataSourceDomain();
        this.dataSources = parent.getDataSources();
        this.prototypeKey = prototypeKey;

        String suggestion = prototypeKey + "0";
        for (int i = 1; i <= dataSources.size(); i++) {
            suggestion = prototypeKey + i;
            if (!dataSources.containsKey(suggestion)) {
                break;
            }
        }

        this.view.getDataSourceName().setText(suggestion);
        initBindings();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getOkButton(), "okAction()");
    }

    public void okAction() {
        if (getName() == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "Enter DataSource Name",
                    null,
                    JOptionPane.WARNING_MESSAGE);
        }
        else if (dataSources.containsKey(getName())) {
            JOptionPane.showMessageDialog(
                    view,
                    "'" + getName() + "' is already in use, enter a different name",
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
    public DBConnectionInfo startupAction() {
        // this should handle closing via ESC
        canceled = true;

        view.setModal(true);
        view.pack();
        view.setResizable(false);
        makeCloseableOnEscape();
        centerView();

        view.show();
        return createDataSource();
    }

    public String getName() {
        String name = view.getDataSourceName().getText();
        return (name.length() > 0) ? name : null;
    }

    protected DBConnectionInfo createDataSource() {
        if (canceled) {
            return null;
        }

        DBConnectionInfo prototype = (DBConnectionInfo) dataSources.get(prototypeKey);
        DBConnectionInfo dataSource = (DBConnectionInfo) editor.createDetail(
                domain,
                getName(),
                DBConnectionInfo.class);

        prototype.copyTo(dataSource);
        return dataSource;
    }

}