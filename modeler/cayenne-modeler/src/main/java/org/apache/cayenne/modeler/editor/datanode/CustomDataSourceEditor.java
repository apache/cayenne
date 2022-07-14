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

package org.apache.cayenne.modeler.editor.datanode;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;

/**
 */
public class CustomDataSourceEditor extends DataSourceEditor {

    protected CustomDataSourceView view;

    protected String factoryName;

    public CustomDataSourceEditor(ProjectController controller, BindingDelegate nodeChangeProcessor) {
        super(controller, nodeChangeProcessor);
    }

    @Override
    public void setNode(DataNodeDescriptor node) {
        setFactoryName(node.getDataSourceFactoryType());
        super.setNode(node);
    }

    @Override
    protected void prepareBindings(BindingBuilder builder) {
        this.view = new CustomDataSourceView();

        fieldAdapters = new ObjectBinding[2];
        fieldAdapters[0] = builder.bindToTextField(view.getFactoryName(), "factoryName");
        fieldAdapters[1] = builder.bindToTextField(view.getLocationHint(), "node.parameters");
    }

    @Override
    public CustomDataSourceView getView() {
        return view;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }
}
