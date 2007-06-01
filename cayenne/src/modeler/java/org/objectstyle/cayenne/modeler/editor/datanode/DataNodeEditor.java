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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.conf.JNDIDataSourceFactory;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.modeler.util.DbAdapterInfo;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.BindingDelegate;
import org.objectstyle.cayenne.swing.ObjectBinding;
import org.objectstyle.cayenne.validation.ValidationException;

/**
 * @author Andrei Adamchik
 */
public class DataNodeEditor extends CayenneController {

    protected static final String NO_LOCAL_DATA_SOURCE = "Select DataSource for Local Work...";

    final static String[] standardDataSourceFactories = new String[] {
            DriverDataSourceFactory.class.getName(),
            JNDIDataSourceFactory.class.getName()
    };

    protected DataNodeView view;
    protected DataNode node;
    protected Map datasourceEditors;

    protected Map localDataSources;

    protected DataSourceEditor defaultSubeditor;

    protected BindingDelegate nodeChangeProcessor;
    protected ObjectBinding[] bindings;
    protected ObjectBinding localDataSourceBinding;

    public DataNodeEditor(ProjectController parent) {
        super(parent);

        this.datasourceEditors = new HashMap();
        this.view = new DataNodeView();
        this.localDataSources = new HashMap();

        this.nodeChangeProcessor = new BindingDelegate() {

            public void modelUpdated(
                    ObjectBinding binding,
                    Object oldValue,
                    Object newValue) {

                DataNodeEvent e = new DataNodeEvent(DataNodeEditor.this, node);
                if (binding != null && binding.getComponent() == view.getDataNodeName()) {
                    e.setOldName(oldValue != null ? oldValue.toString() : null);
                }

                ((ProjectController) getParent()).fireDataNodeEvent(e);
            }
        };

        this.defaultSubeditor = new CustomDataSourceEditor(parent, nodeChangeProcessor);

        initController();
    }

    // ======= properties

    public Component getView() {
        return view;
    }

    public DataNode getNode() {
        return node;
    }

    public void setNode(DataNode node) {
        this.node = node;
    }

    public String getAdapterName() {
        // fix no adapter case... does it ever happen now? (it used to when an empty
        // string
        // was typed)
        if (node != null && node.getAdapter() == null) {
            node.setAdapter(new JdbcAdapter());
        }

        return (node != null) ? node.getAdapter().getClass().getName() : null;
    }

    public void setAdapterName(String name) {
        if (node == null) {
            return;
        }

        if (name == null) {
            // simply ignore null name
            return;
        }

        try {
            Class adapterClass = getApplication()
                    .getClassLoadingService()
                    .loadClass(name);
            node.setAdapter((DbAdapter) adapterClass.newInstance());
        }
        catch (Throwable ex) {
            throw new ValidationException("Unknown DbAdapter: " + name);
        }
    }

    public String getFactoryName() {
        return (node != null) ? node.getDataSourceFactory() : null;
    }

    public void setFactoryName(String factoryName) {
        if (node != null) {
            node.setDataSourceFactory(factoryName);
            showDataSourceSubview(factoryName);
        }
    }

    public String getNodeName() {
        return (node != null) ? node.getName() : null;
    }

    public void setNodeName(String newName) {
        if (node == null) {
            return;
        }

        // validate...
        if (newName == null) {
            throw new ValidationException("Empty DataNode Name");
        }

        ProjectController parent = (ProjectController) getParent();
        Configuration config = ((ApplicationProject) parent.getProject())
                .getConfiguration();

        DataNode matchingNode = null;

        Iterator it = config.getDomains().iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
            DataNode nextNode = domain.getNode(newName);

            if (nextNode == node) {
                continue;
            }

            if (nextNode != null) {
                matchingNode = nextNode;
                break;
            }
        }

        if (matchingNode != null) {
            // there is an entity with the same name
            throw new ValidationException("There is another DataNode named '"
                    + newName
                    + "'. Use a different name.");
        }

        // passed validation, set value...

        // TODO: fixme....there is a slight chance that domain is different than the one
        // cached node belongs to
        ProjectUtil.setDataNodeName(parent.getCurrentDataDomain(), node, newName);
    }

    // ======== other stuff

    protected void initController() {
        view.getDataSourceDetail().add(defaultSubeditor.getView(), "default");

        view.getAdapters().setEditable(true);
        view.getFactories().setEditable(true);

        // init combo box choices
        view.getAdapters().setModel(
                new DefaultComboBoxModel(DbAdapterInfo.getStandardAdapters()));
        view.getFactories().setModel(
                new DefaultComboBoxModel(standardDataSourceFactories));

        // init listeners
        ((ProjectController) getParent())
                .addDataNodeDisplayListener(new DataNodeDisplayListener() {

                    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                        refreshView(e.getDataNode());
                    }
                });

        getView().addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                refreshView(getNode() != null
                        ? getNode()
                        : ((ProjectController) getParent()).getCurrentDataNode());
            }
        });

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        localDataSourceBinding = builder.bindToComboSelection(
                view.getLocalDataSources(),
                "parent.dataNodePreferences.localDataSource",
                NO_LOCAL_DATA_SOURCE);

        // use delegate for the rest of them

        builder.setDelegate(nodeChangeProcessor);

        bindings = new ObjectBinding[3];
        bindings[0] = builder.bindToTextField(view.getDataNodeName(), "nodeName");
        bindings[1] = builder.bindToComboSelection(view.getFactories(), "factoryName");
        bindings[2] = builder.bindToComboSelection(view.getAdapters(), "adapterName");

        // one way bindings
        builder.bindToAction(
                view.getConfigLocalDataSources(),
                "dataSourceConfigAction()");
    }

    public void dataSourceConfigAction() {
        PreferenceDialog prefs = new PreferenceDialog(this);
        prefs.showDataSourceEditorAction(view.getLocalDataSources().getSelectedItem());
        prefs.startupAction();

        refreshLocalDataSources();
    }

    protected void refreshLocalDataSources() {
        localDataSources.clear();

        Collection sources = getApplication().getPreferenceDomain().getDetails(
                DBConnectionInfo.class);

        int len = sources.size();
        Object[] keys = new Object[len + 1];

        // a slight chance that a real datasource is called NO_LOCAL_DATA_SOURCE...
        keys[0] = NO_LOCAL_DATA_SOURCE;
        Iterator it = sources.iterator();
        for (int i = 1; i <= len; i++) {
            DBConnectionInfo info = (DBConnectionInfo) it.next();
            keys[i] = info.getKey();
            localDataSources.put(keys[i], info);
        }

        view.getLocalDataSources().setModel(new DefaultComboBoxModel(keys));
        localDataSourceBinding.updateView();
    }

    /**
     * Reinitializes widgets to display selected DataNode.
     */
    protected void refreshView(DataNode node) {
        this.node = node;

        if (node == null) {
            getView().setVisible(false);
            return;
        }

        refreshLocalDataSources();

        getView().setVisible(true);
        for (int i = 0; i < bindings.length; i++) {
            bindings[i].updateView();
        }

        showDataSourceSubview(getFactoryName());
    }

    /**
     * Selects a subview for a currently selected DataSource factory.
     */
    protected void showDataSourceSubview(String factoryName) {
        DataSourceEditor c = (DataSourceEditor) datasourceEditors.get(factoryName);

        // create subview dynamically...
        if (c == null) {

            if (DriverDataSourceFactory.class.getName().equals(factoryName)) {
                c = new JDBCDataSourceEditor(
                        (ProjectController) getParent(),
                        nodeChangeProcessor);
            }
            else if (JNDIDataSourceFactory.class.getName().equals(factoryName)) {
                c = new JNDIDataSourceEditor(
                        (ProjectController) getParent(),
                        nodeChangeProcessor);
            }
            else {
                // special case - no detail view, just show it and bail..
                defaultSubeditor.setNode(getNode());
                view.getDataSourceDetailLayout().show(
                        view.getDataSourceDetail(),
                        "default");
                return;
            }

            datasourceEditors.put(factoryName, c);
            view.getDataSourceDetail().add(c.getView(), factoryName);

            // this is needed to display freshly added panel...
            view.getDataSourceDetail().getParent().validate();
        }

        // this will refresh subview...
        c.setNode(getNode());

        // display the right subview...
        view.getDataSourceDetailLayout().show(view.getDataSourceDetail(), factoryName);
    }

}