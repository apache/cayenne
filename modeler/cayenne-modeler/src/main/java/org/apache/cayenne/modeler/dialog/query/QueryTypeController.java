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
package org.apache.cayenne.modeler.dialog.query;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.model.QueryEvent;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.undo.CreateQueryUndoableEdit;

import javax.swing.*;
import java.awt.*;

public class QueryTypeController extends ChildController<ProjectController> {

    protected DataMap dataMap;
    protected DataChannelDescriptor domain;

    protected QueryTypeView view;
    protected String type;

    public QueryTypeController(ProjectController parent) {
        super(parent);

        view = new QueryTypeView();
        initController();

        // by default use object query...
        this.type = QueryDescriptor.SELECT_QUERY;
        this.dataMap = parent.getSelectedDataMap();
        this.domain = (DataChannelDescriptor) parent.getProject().getRootNode();
    }

    @Override
    public Component getView() {
        return view;
    }

    private void initController() {
        view.getCancelButton().addActionListener(e -> view.dispose());
        view.getSaveButton().addActionListener(e -> createQuery());
        view.getObjectSelect().addActionListener(e -> setObjectSelectQuery());
        view.getSqlSelect().addActionListener(e -> setRawSQLQuery());
        view.getProcedureSelect().addActionListener(e -> setProcedureQuery());
        view.getEjbqlSelect().addActionListener(e -> setEjbqlQuery());
    }

    public void startupAction() {
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    /**
     * Action method that creates a query for the specified query type.
     */
    public void createQuery() {
        String queryType = getSelectedQuery();

        // update query...
        QueryDescriptor query = QueryDescriptor.descriptor(queryType);
        query.setName(NameBuilder.builder(query, dataMap).name());
        query.setDataMap(dataMap);

        dataMap.addQueryDescriptor(query);

        getApplication().getUndoManager().addEdit(
                new CreateQueryUndoableEdit(domain, dataMap, query));

        // notify listeners
        fireQueryEvent(this, parent, dataMap, query);
        view.dispose();
    }

    /**
     * Fires events when a query was added
     */
    public static void fireQueryEvent(Object src, ProjectController controller,
                                      DataMap dataMap, QueryDescriptor query) {
        controller.fireQueryEvent(new QueryEvent(src, query, MapEvent.ADD,
                dataMap));
        controller.displayQuery(new QueryDisplayEvent(src, query,
                dataMap, (DataChannelDescriptor) controller.getProject().getRootNode()));
    }

    public String getSelectedQuery() {
        return type;
    }

    public void setObjectSelectQuery() {
        this.type = QueryDescriptor.SELECT_QUERY;
    }

    public void setRawSQLQuery() {
        this.type = QueryDescriptor.SQL_TEMPLATE;
    }

    public void setProcedureQuery() {
        this.type = QueryDescriptor.PROCEDURE_QUERY;
    }

    public void setEjbqlQuery() {
        this.type = QueryDescriptor.EJBQL_QUERY;
    }
}
