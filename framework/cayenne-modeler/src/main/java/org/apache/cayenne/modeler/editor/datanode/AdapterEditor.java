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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ModelerDbAdapter;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

/**
 */
public class AdapterEditor extends CayenneController {

    protected AdapterView view;
    protected DataNode node;
    protected ObjectBinding adapterNameBinding;

    public AdapterEditor(CayenneController parent) {
        super(parent);

        this.view = new AdapterView();
        initController();
    }

    protected void initController() {
        // init bindings
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        adapterNameBinding = builder.bindToTextField(
                view.getCustomAdapter(),
                "adapterName");

        // init listenersК
        ((ProjectController) getParent())
                .addDataNodeDisplayListener(new DataNodeDisplayListener() {

                    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                        refreshView(e.getDataNode());
                    }
                });

        getView().addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                refreshView(node != null ? node : ((ProjectController) getParent())
                        .getCurrentDataNode());
            }
        });
    }

    protected void refreshView(DataNode node) {
        this.node = node;

        if (node == null) {
            getView().setVisible(false);
            return;
        }

        adapterNameBinding.updateView();
    }

    public Component getView() {
        return view;
    }

    public String getAdapterName() {
        if (node == null) {
            return null;
        }

        DbAdapter adapter = node.getAdapter();

        // TODO, Andrus, 11/3/2005 - to simplify this logic, it would be nice to
        // consistently load CustomDbAdapter... this would require an ability to set a
        // load delegate in OpenProjectAction
        if (adapter == null) {
            return null;
        }
        else if (adapter instanceof ModelerDbAdapter) {
            return ((ModelerDbAdapter) adapter).getAdapterClassName();
        }
        // don't do "instanceof" here, as we maybe dealing with a custom subclass...
        else if (adapter.getClass() == AutoAdapter.class) {
            return null;
        }
        else {
            return adapter.getClass().getName();
        }
    }

    public void setAdapterName(String name) {
        if (node == null) {
            return;
        }

        ModelerDbAdapter adapter = new ModelerDbAdapter(name, node.getDataSource());
        adapter.validate();
        node.setAdapter(adapter);
        
        DataNodeEvent e = new DataNodeEvent(AdapterEditor.this, node);
        ((ProjectController) getParent()).fireDataNodeEvent(e);
    }
}
