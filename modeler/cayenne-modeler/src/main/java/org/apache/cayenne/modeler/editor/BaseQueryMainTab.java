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

package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;

abstract class BaseQueryMainTab extends JPanel {
    protected ProjectController mediator;
    protected TextAdapter name;
    protected JComboBox<ObjEntity> queryRoot;

    BaseQueryMainTab(ProjectController mediator) {
        this.mediator = mediator;
    }

    protected void initQueryRoot() {
        queryRoot = Application.getWidgetFactory().createComboBox();
        AutoCompletion.enable(queryRoot);
        queryRoot.setRenderer(CellRenderers.listRendererWithIcons());

        RootSelectionHandler rootHandler = new RootSelectionHandler(this);

        queryRoot.addActionListener(rootHandler);
        queryRoot.addFocusListener(rootHandler);
        queryRoot.getEditor().getEditorComponent().addFocusListener(rootHandler);
    }

    public TextAdapter getNameField() {
        return name;
    }

    public JComboBox<ObjEntity> getQueryRoot() {
        return queryRoot;
    }

    public ProjectController getMediator() {
        return mediator;
    }

    /**
     * Initializes Query name from string.
     */
    void setQueryName(String newName) {
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

        DataMap map = mediator.getCurrentDataMap();
        QueryDescriptor matchingQuery = map.getQueryDescriptor(newName);

        if (matchingQuery == null) {
            // completely new name, set new name for entity
            QueryEvent e = new QueryEvent(this, query, query.getName());
            ProjectUtil.setQueryName(map, query, newName);
            mediator.fireQueryEvent(e);
        } else if (matchingQuery != query) {
            // there is a query with the same name
            throw new ValidationException("There is another query named '"
                    + newName
                    + "'. Use a different name.");
        }
    }

    abstract QueryDescriptor getQuery();
}
