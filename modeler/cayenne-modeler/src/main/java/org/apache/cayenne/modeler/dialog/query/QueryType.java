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
package org.apache.cayenne.modeler.dialog.query;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.WindowConstants;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateQueryUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.query.*;


public class QueryType extends CayenneController{

    protected ProjectController mediator;
    protected DataMap dataMap;
    protected DataChannelDescriptor domain;

    protected QueryTypeView view;
    protected String type;
    
    public QueryType(ProjectController mediator, DataMap root) {
        super(mediator);

        view = new QueryTypeView();
        initController();

        // by default use object query...
        this.type = QueryDescriptor.SELECT_QUERY;
        this.mediator = mediator;
        this.dataMap = mediator.getCurrentDataMap();
        this.domain = (DataChannelDescriptor)mediator.getProject().getRootNode();
    }

    @Override
    public Component getView() {
        return view;
    } 
    
    private void initController() {
        view.getCancelButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                view.dispose();
            }
        });
        view.getSaveButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                createQuery();
            }
        });
        view.getObjectSelect().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                 setObjectSelectQuery();
            }
        });
        view.getSqlSelect().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
               setRawSQLQuery();
            }
        });
        view.getProcedureSelect().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setProcedureQuery();
            }
        });
        view.getEjbqlSelect().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setEjbqlQuery();
            }
        });
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
        String queryName = DefaultUniqueNameGenerator.generate(NameCheckers.query, dataMap);

        QueryDescriptor query = QueryDescriptor.descriptor(queryType);

        query.setName(queryName);
        query.setDataMap(dataMap);
        
        dataMap.addQueryDescriptor(query);

        mediator.getApplication().getUndoManager().addEdit(
                new CreateQueryUndoableEdit(domain, dataMap, query));

        // notify listeners
        fireQueryEvent(this, mediator,dataMap, query);
        view.dispose();
    }

    /**
     * Fires events when a query was added
     */
    public static void fireQueryEvent(Object src, ProjectController mediator,
            DataMap dataMap, QueryDescriptor query) {
        mediator.fireQueryEvent(new QueryEvent(src, query, MapEvent.ADD,
                dataMap));
        mediator.fireQueryDisplayEvent(new QueryDisplayEvent(src, query,
                dataMap, (DataChannelDescriptor)mediator.getProject().getRootNode()));
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
