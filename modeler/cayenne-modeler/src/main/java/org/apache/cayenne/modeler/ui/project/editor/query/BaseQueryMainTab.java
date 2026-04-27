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

package org.apache.cayenne.modeler.ui.project.editor.query;

import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.combo.AutoCompletion;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;

public abstract class BaseQueryMainTab extends JPanel {

    protected final ProjectController controller;
    protected CayenneUndoableTextField name;
    protected JComboBox<ObjEntity> queryRoot;

    protected BaseQueryMainTab(ProjectController controller) {
        this.controller = controller;
    }

    protected void initQueryRoot() {
        queryRoot = WidgetFactory.createComboBox();
        AutoCompletion.enable(queryRoot);
        queryRoot.setRenderer(Renderers.listRendererWithIcons());

        RootSelectionHandler rootHandler = new RootSelectionHandler(this);

        queryRoot.addActionListener(rootHandler);
        queryRoot.addFocusListener(rootHandler);
        queryRoot.getEditor().getEditorComponent().addFocusListener(rootHandler);
    }

    public CayenneUndoableTextField getNameField() {
        return name;
    }

    public JComboBox<ObjEntity> getQueryRoot() {
        return queryRoot;
    }

    public ProjectController getController() {
        return controller;
    }

    /**
     * Initializes Query name from string.
     */
    protected void setQueryName(String newName) {
        if (newName != null && newName.trim().length() == 0) {
            newName = null;
        }

        QueryDescriptor query = getQuery();

        if (query == null) {
            return;
        }

        if (Util.nullSafeEquals(newName, query.getName())) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("SelectQuery name is required.");
        }

        DataMap map = controller.getSelectedDataMap();
        QueryDescriptor matchingQuery = map.getQueryDescriptor(newName);

        if (matchingQuery == null) {
            // completely new name, set new name for entity
            String oldName = query.getName();
            QueryEvent e = QueryEvent.ofChange(this, query, oldName);
            query.setName(newName);
            query.setDataMap(map);
            map.removeQueryDescriptor(oldName);
            map.addQueryDescriptor(query);
            MappingNamespace ns = map.getNamespace();
            if (ns instanceof EntityResolver) {
                ((EntityResolver) ns).refreshMappingCache();
            }
            controller.fireQueryEvent(e);
        } else if (matchingQuery != query) {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    protected abstract QueryDescriptor getQuery();
}
