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
package org.objectstyle.cayenne.modeler.editor.datanode;

import java.awt.Component;

import org.objectstyle.cayenne.modeler.CayenneModelerController;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.project.ProjectDataSource;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.BindingDelegate;
import org.objectstyle.cayenne.swing.ObjectBinding;

/**
 * @author Andrei Adamchik
 */
public class JDBCDataSourceEditor extends DataSourceEditor {

    protected JDBCDataSourceView view;

    public JDBCDataSourceEditor(ProjectController parent,
            BindingDelegate nodeChangeProcessor) {
        super(parent, nodeChangeProcessor);
    }

    public Component getView() {
        return view;
    }

    protected void prepareBindings(BindingBuilder builder) {
        this.view = new JDBCDataSourceView();

        fieldAdapters = new ObjectBinding[6];
        fieldAdapters[0] = builder.bindToTextField(
                view.getUserName(),
                "node.dataSource.dataSourceInfo.userName");
        fieldAdapters[1] = builder.bindToTextField(
                view.getPassword(),
                "node.dataSource.dataSourceInfo.password");
        fieldAdapters[2] = builder.bindToTextField(
                view.getUrl(),
                "node.dataSource.dataSourceInfo.dataSourceUrl");
        fieldAdapters[3] = builder.bindToTextField(
                view.getDriver(),
                "node.dataSource.dataSourceInfo.jdbcDriver");
        fieldAdapters[4] = builder.bindToTextField(
                view.getMaxConnections(),
                "node.dataSource.dataSourceInfo.maxConnections");
        fieldAdapters[5] = builder.bindToTextField(
                view.getMinConnections(),
                "node.dataSource.dataSourceInfo.minConnections");

        // one way binding
        builder.bindToAction(view.getSyncWithLocal(), "syncDataSourceAction()");

    }

    public void syncDataSourceAction() {
        CayenneModelerController mainController = getApplication().getFrameController();

        if (getNode() == null || getNode().getDataSource() == null) {
            return;
        }

        ProjectDataSource projectDS = (ProjectDataSource) getNode().getDataSource();

        ProjectController parent = (ProjectController) getParent();
        String key = parent.getDataNodePreferences().getLocalDataSource();
        if (key == null) {
            mainController.updateStatus("No Local DataSource selected for node...");
            return;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) parent
                .getApplicationPreferenceDomain()
                .getDetail(key, DBConnectionInfo.class, false);

        if (dataSource != null) {
            if (dataSource.copyTo(projectDS.getDataSourceInfo())) {
                refreshView();
                super.nodeChangeProcessor.modelUpdated(null, null, null);
                mainController.updateStatus(null);
            }
            else {
                mainController.updateStatus("DataNode is up to date...");
            }
        }
        else {
            mainController.updateStatus("Invalid Local DataSource selected for node...");
        }
    }
}