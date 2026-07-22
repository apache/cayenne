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

package org.apache.cayenne.modeler.ui.project.editor.datanode.custom;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.project.editor.datanode.DataSourcePanel;

import java.awt.BorderLayout;

public class CustomDataSourcePanel extends DataSourcePanel {

    private final CMUndoableTextField factoryName;
    private final CMUndoableTextField locationHint;
    private String currentFactoryName;

    public CustomDataSourcePanel(Application app, Runnable nodeChangeProcessor) {
        super(app, nodeChangeProcessor);

        this.factoryName = new CMUndoableTextField(app.getUndoManager());
        this.locationHint = new CMUndoableTextField(app.getUndoManager());

        initLayout();
        initBindings();
    }

    private void initLayout() {
        FormLayout layout = new FormLayout("right:80dlu, $lcgap, fill:200dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("Custom Data Source Factory");
        builder.append("Factory Class:", factoryName);
        builder.append("Location Hint (optional):", locationHint);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        factoryName.addCommitListener(v -> {
            this.currentFactoryName = v;
            nodeChangeProcessor.run();
        });
        locationHint.addCommitListener(v -> {
            getNode().setParameters(v);
            nodeChangeProcessor.run();
        });
    }

    @Override
    public void setNode(DataNodeDescriptor node) {
        this.currentFactoryName = node.getDataSourceFactoryType();
        super.setNode(node);
    }

    @Override
    protected void refreshView() {
        factoryName.setText(currentFactoryName);
        locationHint.setText(getNode().getParameters());
    }

    public String getFactoryName() {
        return currentFactoryName;
    }
}
