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

package org.apache.cayenne.modeler.ui.project.editor;

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.DataDomainView;
import org.apache.cayenne.modeler.ui.project.editor.datamap.DataMapView;
import org.apache.cayenne.modeler.ui.project.editor.datanode.DataNodeEditorPanel;
import org.apache.cayenne.modeler.ui.project.editor.dbentity.DbEntityView;
import org.apache.cayenne.modeler.ui.project.editor.embeddable.EmbeddableView;
import org.apache.cayenne.modeler.ui.project.editor.objentity.ObjEntityView;
import org.apache.cayenne.modeler.ui.project.editor.procedure.ProcedureQueryView;
import org.apache.cayenne.modeler.ui.project.editor.procedure.ProcedureTabbedView;
import org.apache.cayenne.modeler.ui.project.editor.query.ejbql.EjbqlTabbedView;
import org.apache.cayenne.modeler.ui.project.editor.query.selectquery.SelectQueryTabbedView;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateTabbedView;

import javax.swing.*;
import java.awt.*;

/**
 * Card-layout panel that shows the editor matching the currently selected project object.
 */
public class EditorPanelView extends JPanel {

    private static final String EMPTY_VIEW = "Empty";
    private static final String DOMAIN_VIEW = "Domain";
    private static final String NODE_VIEW = "Node";
    private static final String DATA_MAP_VIEW = "DataMap";
    private static final String OBJ_VIEW = "ObjView";
    private static final String DB_VIEW = "DbView";
    private static final String EMBEDDABLE_VIEW = "EmbeddableView";
    private static final String PROCEDURE_VIEW = "ProcedureView";
    private static final String SELECT_QUERY_VIEW = "SelectQueryView";
    private static final String SQL_TEMPLATE_VIEW = "SQLTemplateView";
    private static final String PROCEDURE_QUERY_VIEW = "ProcedureQueryView";
    private static final String EJBQL_QUERY_VIEW = "EjbqlQueryView";

    private final CardLayout detailLayout;

    private final DbEntityView dbDetailView;
    private final ObjEntityView objDetailView;
    private final EmbeddableView embeddableView;
    private final DataDomainView dataDomainView;
    private final DataMapView dataMapView;
    private final ProcedureTabbedView procedureView;
    private final SQLTemplateTabbedView sqlTemplateView;
    private final EjbqlTabbedView ejbqlQueryView;

    public EditorPanelView(ProjectSession session) {

        detailLayout = new CardLayout();
        setLayout(detailLayout);

        // some but not all panels must be wrapped in a scroll pane
        // those that are not wrapped usually have their own scrollers
        // in subpanels...

        add(new JPanel(), EMPTY_VIEW);

        dataDomainView = new DataDomainView(session);
        add(dataDomainView, DOMAIN_VIEW);

        DataNodeEditorPanel nodeEditor = new DataNodeEditorPanel(session.app(), session);
        add(new JScrollPane(nodeEditor), NODE_VIEW);

        dataMapView = new DataMapView(session);
        add(dataMapView, DATA_MAP_VIEW);

        procedureView = new ProcedureTabbedView(session);
        add(procedureView, PROCEDURE_VIEW);

        SelectQueryTabbedView selectQueryView = new SelectQueryTabbedView(session);
        add(selectQueryView, SELECT_QUERY_VIEW);

        sqlTemplateView = new SQLTemplateTabbedView(session);
        add(sqlTemplateView, SQL_TEMPLATE_VIEW);

        Component procedureQueryView = new ProcedureQueryView(session);
        add(new JScrollPane(procedureQueryView), PROCEDURE_QUERY_VIEW);

        ejbqlQueryView = new EjbqlTabbedView(session);
        add(ejbqlQueryView, EJBQL_QUERY_VIEW);

        embeddableView = new EmbeddableView(session);
        add(embeddableView, EMBEDDABLE_VIEW);

        objDetailView = new ObjEntityView(session);
        add(objDetailView, OBJ_VIEW);

        dbDetailView = new DbEntityView(session);
        add(dbDetailView, DB_VIEW);

        session.addDomainDisplayListener(e -> detailLayout.show(this, e.getDomain() == null ? EMPTY_VIEW : DOMAIN_VIEW));
        session.addDataNodeDisplayListener(e -> detailLayout.show(this, e.getDataNode() == null ? EMPTY_VIEW : NODE_VIEW));
        session.addDataMapDisplayListener(e -> detailLayout.show(this, e.getDataMap() == null ? EMPTY_VIEW : DATA_MAP_VIEW));
        session.addObjEntityDisplayListener(e -> detailLayout.show(this, e.getEntity() == null ? EMPTY_VIEW : OBJ_VIEW));
        session.addDbEntityDisplayListener(e -> detailLayout.show(this, e.getEntity() == null ? EMPTY_VIEW : DB_VIEW));
        session.addProcedureDisplayListener(e -> detailLayout.show(this, e.getProcedure() == null ? EMPTY_VIEW : PROCEDURE_VIEW));
        session.addQueryDisplayListener(this::querySelected);
        session.addMultipleObjectsDisplayListener(e -> detailLayout.show(this, EMPTY_VIEW));
        session.addEmbeddableDisplayListener(e -> detailLayout.show(this, e.getEmbeddable() == null ? EMPTY_VIEW : EMBEDDABLE_VIEW));
    }

    public SQLTemplateTabbedView getSqlTemplateView() {
        return sqlTemplateView;
    }

    public EjbqlTabbedView getEjbqlQueryView() {
        return ejbqlQueryView;
    }

    public ProcedureTabbedView getProcedureView() {
        return procedureView;
    }

    public EmbeddableView getEmbeddableView() {
        return embeddableView;
    }

    public DbEntityView getDbDetailView() {
        return dbDetailView;
    }

    public ObjEntityView getObjDetailView() {
        return objDetailView;
    }

    public DataDomainView getDataDomainView() {
        return dataDomainView;
    }

    public DataMapView getDataMapView() {
        return dataMapView;
    }

    private void querySelected(QueryDisplayEvent e) {
        QueryDescriptor query = e.getQuery();

        switch (query.getType()) {
            case QueryDescriptor.SELECT_QUERY:
                detailLayout.show(this, SELECT_QUERY_VIEW);
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                detailLayout.show(this, SQL_TEMPLATE_VIEW);
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                detailLayout.show(this, PROCEDURE_QUERY_VIEW);
                break;
            case QueryDescriptor.EJBQL_QUERY:
                detailLayout.show(this, EJBQL_QUERY_VIEW);
                break;
            default:
                detailLayout.show(this, EMPTY_VIEW);
        }
    }
}
