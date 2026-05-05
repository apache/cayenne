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

package org.apache.cayenne.modeler.ui.project.editor.datanode;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.util.Util;

/**
 * Base for the inner panels of {@link DataNodeEditorPanel} that edit the DataSource portion
 * of a {@link DataNodeDescriptor}. Selection between concrete panels is driven by the chosen
 * DataSource factory in the parent editor.
 */
public abstract class DataSourcePanel extends AppPanel {

    private DataNodeDescriptor node;
    protected final Runnable nodeChangeProcessor;

    protected DataSourcePanel(Application app, Runnable nodeChangeProcessor) {
        super(app);
        this.nodeChangeProcessor = nodeChangeProcessor;
    }

    public DataNodeDescriptor getNode() {
        return node;
    }

    public void setNode(DataNodeDescriptor node) {
        if (!Util.nullSafeEquals(this.node, node)) {
            this.node = node;
            refreshView();
        }
    }

    protected abstract void refreshView();
}
