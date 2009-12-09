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

package org.apache.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.ProjectTreeView;
import org.apache.cayenne.modeler.editor.datanode.DataNodeEditor;
import org.apache.cayenne.modeler.editor.dbentity.DbEntityTabbedView;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayListener;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayListener;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayListener;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.commons.logging.LogFactory;

/**
 * Main display area split into the project navigation tree on the left and selected
 * object editor on the right.
 */
public class EditorView extends JPanel implements ObjEntityDisplayListener,
        DbEntityDisplayListener, DomainDisplayListener, DataMapDisplayListener,
        DataNodeDisplayListener, ProcedureDisplayListener, QueryDisplayListener,
        MultipleObjectsDisplayListener, EmbeddableDisplayListener {

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

    protected ProjectController eventController;
    protected JSplitPane splitPane;
    protected Container detailPanel;
    protected CardLayout detailLayout;
    private ProjectTreeView treePanel;
    
    private DbEntityTabbedView dbDetailView;
    private ObjEntityTabbedView objDetailView;
    private EmbeddableTabbedView embeddableView;
    private DataMapTabbedView dataMapView;
    private ProcedureTabbedView procedureView;
    private SelectQueryTabbedView selectQueryView;
    private SQLTemplateTabbedView sqlTemplateView;
    private EjbqlTabbedView ejbqlQueryView;
    private JTabbedPane dataNodeView;

    
    public SelectQueryTabbedView getSelectQueryView() {
        return selectQueryView;
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

    public ProjectTreeView getProjectTreeView() {
        return treePanel;
    }

    public EmbeddableTabbedView getEmbeddableView() {
        return embeddableView;
    }
    
    public DbEntityTabbedView getDbDetailView() {
        return dbDetailView;
    }

    public ObjEntityTabbedView getObjDetailView() {
        return objDetailView;
    }
    
    public DataMapTabbedView getDataMapView() {
        return dataMapView;
    }
    
    public JTabbedPane getDataNodeView() {
        return dataNodeView;
    }

    public EditorView(ProjectController eventController) {
        this.eventController = eventController;
        initView();
        initController();
    }

    private void initView() {

        // init widgets
        treePanel = new ProjectTreeView(eventController);
        treePanel.setMinimumSize(new Dimension(50, 200));

        this.detailPanel = new JPanel();
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);

        // assemble...

        this.detailLayout = new CardLayout();
        detailPanel.setLayout(detailLayout);

        // some but not all panels must be wrapped in a scroll pane
        // those that are not wrapped usually have there own scrollers
        // in subpanels...

        detailPanel.add(new JPanel(), EMPTY_VIEW);

        Component domainView = new DataDomainView(eventController);
        detailPanel.add(new JScrollPane(domainView), DOMAIN_VIEW);

        DataNodeEditor nodeController = new DataNodeEditor(eventController);
        detailPanel.add(nodeController.getView(), NODE_VIEW);

        Component dataMapView = new DataMapTabbedView(eventController);
        detailPanel.add(dataMapView, DATA_MAP_VIEW);

        Component procedureView = new ProcedureTabbedView(eventController);
        detailPanel.add(procedureView, PROCEDURE_VIEW);

        Component selectQueryView = new SelectQueryTabbedView(eventController);
        detailPanel.add(selectQueryView, SELECT_QUERY_VIEW);

        Component sqlTemplateView = new SQLTemplateTabbedView(eventController);
        detailPanel.add(sqlTemplateView, SQL_TEMPLATE_VIEW);

        Component procedureQueryView = new ProcedureQueryView(eventController);
        detailPanel.add(new JScrollPane(procedureQueryView), PROCEDURE_QUERY_VIEW);

        Component ejbqlQueryView = new EjbqlTabbedView(eventController);
        detailPanel.add(ejbqlQueryView, EJBQL_QUERY_VIEW);

        embeddableView = new EmbeddableTabbedView(eventController);
        detailPanel.add(new JScrollPane(embeddableView), EMBEDDABLE_VIEW);

        objDetailView = new ObjEntityTabbedView(eventController);
        detailPanel.add(objDetailView, OBJ_VIEW);

        dbDetailView = new DbEntityTabbedView(eventController);
        detailPanel.add(dbDetailView, DB_VIEW);

        splitPane.setLeftComponent(new JScrollPane(treePanel));
        splitPane.setRightComponent(detailPanel);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private void initController() {
        eventController.addDomainDisplayListener(this);
        eventController.addDataNodeDisplayListener(this);
        eventController.addDataMapDisplayListener(this);
        eventController.addObjEntityDisplayListener(this);
        eventController.addDbEntityDisplayListener(this);
        eventController.addProcedureDisplayListener(this);
        eventController.addQueryDisplayListener(this);
        eventController.addMultipleObjectsDisplayListener(this);
        eventController.addEmbeddableDisplayListener(this);

        /**
         * Moving this to try-catch block per CAY-940. Exception will be stack-traced
         */
        try {
            Domain domain = eventController
                    .getApplicationPreferenceDomain()
                    .getSubdomain(this.getClass());
            ComponentGeometry geometry = (ComponentGeometry) domain.getDetail(
                    "splitPane.divider",
                    ComponentGeometry.class,
                    true);

            geometry
                    .bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 150);
        }
        catch (Exception ex) {
            LogFactory.getLog(getClass()).error("Cannot bind divider property", ex);
        }
    }

    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        if (e.getProcedure() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, PROCEDURE_VIEW);
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        if (e.getDomain() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DOMAIN_VIEW);
    }

    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
        if (e.getDataNode() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, NODE_VIEW);
    }

    public void currentDataMapChanged(DataMapDisplayEvent e) {
        if (e.getDataMap() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DATA_MAP_VIEW);
    }

    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, OBJ_VIEW);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        if (e.getEntity() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, DB_VIEW);
    }

    public void currentQueryChanged(QueryDisplayEvent e) {
        Query query = e.getQuery();

        if (query instanceof SelectQuery) {
            detailLayout.show(detailPanel, SELECT_QUERY_VIEW);
        }
        else if (query instanceof SQLTemplate) {
            detailLayout.show(detailPanel, SQL_TEMPLATE_VIEW);
        }
        else if (query instanceof ProcedureQuery) {
            detailLayout.show(detailPanel, PROCEDURE_QUERY_VIEW);
        }
        else if (query instanceof EJBQLQuery) {
            detailLayout.show(detailPanel, EJBQL_QUERY_VIEW);
        }
        else {
            detailLayout.show(detailPanel, EMPTY_VIEW);
        }
    }

    public void currentObjectsChanged(MultipleObjectsDisplayEvent e) {
        detailLayout.show(detailPanel, EMPTY_VIEW);
    }

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        if (e.getEmbeddable() == null)
            detailLayout.show(detailPanel, EMPTY_VIEW);
        else
            detailLayout.show(detailPanel, EMBEDDABLE_VIEW);
    }
}
