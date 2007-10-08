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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public abstract class DataSourceEditor extends CayenneController {

    protected ObjectBinding[] fieldAdapters;
    protected DataNode node;
    protected BindingDelegate nodeChangeProcessor;

    public DataSourceEditor(ProjectController controller,
            BindingDelegate nodeChangeProcessor) {
        super(controller);
        this.nodeChangeProcessor = nodeChangeProcessor;
        initBindings();
    }

    public DataNode getNode() {
        return node;
    }

    public void setNode(DataNode node) {
        if (!Util.nullSafeEquals(this.node, node)) {
            this.node = node;

            for (int i = 0; i < fieldAdapters.length; i++) {
                fieldAdapters[i].updateView();
            }
        }
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.setDelegate(nodeChangeProcessor);
        prepareBindings(builder);
    }

    protected abstract void prepareBindings(BindingBuilder builder);

    protected void refreshView() {
        for (int i = 0; i < fieldAdapters.length; i++) {
            fieldAdapters[i].updateView();
        }
    }
}
