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

import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.ProjectTreeView;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CollapseTreeAction;
import org.apache.cayenne.modeler.action.FilterAction;
import org.apache.cayenne.modeler.dialog.datadomain.FilterController;
import org.apache.cayenne.modeler.editor.datanode.DataNodeEditorController;
import org.apache.cayenne.modeler.editor.dbentity.DbEntityTabbedView;
import org.apache.cayenne.modeler.event.display.QueryDisplayEvent;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Main display area split into the project navigation tree on the left and selected object editor on the right.
 */
public class EditorPanel extends JPanel {

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

    private final Container detailPanel;
    private final CardLayout detailLayout;
    private final ProjectTreeView treePanel;

    private final DbEntityTabbedView dbDetailView;
    private final ObjEntityTabbedView objDetailView;
    private final EmbeddableTabbedView embeddableView;
    private final DataDomainTabbedView dataDomainView;
    private final DataMapTabbedView dataMapView;
    private final ProcedureTabbedView procedureView;
    private final SQLTemplateTabbedView sqlTemplateView;
    private final EjbqlTabbedView ejbqlQueryView;
    private final JTabbedPane dataNodeView;

    private final FilterController filterController;

    public FilterController getFilterController() {
        return filterController;
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

    public DataDomainTabbedView getDataDomainView() {
        return dataDomainView;
    }

    public DataMapTabbedView getDataMapView() {
        return dataMapView;
    }

    public JTabbedPane getDataNodeView() {
        return dataNodeView;
    }

    public EditorPanel(ProjectController controller) {

        ActionManager actionManager = controller.getApplication().getActionManager();
        actionManager.getAction(CollapseTreeAction.class).setAlwaysOn(true);
        actionManager.getAction(FilterAction.class).setAlwaysOn(true);

        setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 1));

        JToolBar barPanel = new JToolBar();
        barPanel.setFloatable(false);
        barPanel.setMinimumSize(new Dimension(75, 30));
        barPanel.setBorder(BorderFactory.createEmptyBorder());
        barPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton collapseButton = actionManager.getAction(CollapseTreeAction.class).buildButton(1);
        JButton filterButton = actionManager.getAction(FilterAction.class).buildButton(3);
        filterButton.setPreferredSize(new Dimension(30, 30));
        collapseButton.setPreferredSize(new Dimension(30, 30));
        barPanel.add(filterButton);
        barPanel.add(collapseButton);

        treePanel = new ProjectTreeView(controller);
        treePanel.setMinimumSize(new Dimension(75, 180));

        JPanel treeNavigatePanel = new JPanel();
        treeNavigatePanel.setMinimumSize(new Dimension(75, 220));
        treeNavigatePanel.setLayout(new BorderLayout());
        treeNavigatePanel.add(treePanel, BorderLayout.CENTER);

        detailPanel = new JPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerSize(2);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        JPanel leftPanel = new JPanel(new BorderLayout());

        // assemble...
        detailLayout = new CardLayout();
        detailPanel.setLayout(detailLayout);

        // some but not all panels must be wrapped in a scroll pane
        // those that are not wrapped usually have their own scrollers
        // in subpanels...

        detailPanel.add(new JPanel(), EMPTY_VIEW);

        dataDomainView = new DataDomainTabbedView(controller);
        detailPanel.add(dataDomainView, DOMAIN_VIEW);

        DataNodeEditorController nodeController = new DataNodeEditorController(controller);
        detailPanel.add(nodeController.getView(), NODE_VIEW);

        dataNodeView = nodeController.getTabComponent();

        dataMapView = new DataMapTabbedView(controller);
        detailPanel.add(dataMapView, DATA_MAP_VIEW);

        procedureView = new ProcedureTabbedView(controller);
        detailPanel.add(procedureView, PROCEDURE_VIEW);

        SelectQueryTabbedView selectQueryView = new SelectQueryTabbedView(controller);
        detailPanel.add(selectQueryView, SELECT_QUERY_VIEW);

        sqlTemplateView = new SQLTemplateTabbedView(controller);
        detailPanel.add(sqlTemplateView, SQL_TEMPLATE_VIEW);

        Component procedureQueryView = new ProcedureQueryView(controller);
        detailPanel.add(new JScrollPane(procedureQueryView), PROCEDURE_QUERY_VIEW);

        ejbqlQueryView = new EjbqlTabbedView(controller);
        detailPanel.add(ejbqlQueryView, EJBQL_QUERY_VIEW);

        embeddableView = new EmbeddableTabbedView(controller);
        detailPanel.add(embeddableView, EMBEDDABLE_VIEW);

        objDetailView = new ObjEntityTabbedView(controller);
        detailPanel.add(objDetailView, OBJ_VIEW);

        dbDetailView = new DbEntityTabbedView(controller);
        detailPanel.add(dbDetailView, DB_VIEW);

        leftPanel.add(barPanel, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scrollPane = new JScrollPane(treeNavigatePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(detailPanel);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        this.filterController = new FilterController(treePanel);

        controller.addDomainDisplayListener(e -> detailLayout.show(detailPanel, e.getDomain() == null ? EMPTY_VIEW : DOMAIN_VIEW));
        controller.addDataNodeDisplayListener(e -> detailLayout.show(detailPanel, e.getDataNode() == null ? EMPTY_VIEW : NODE_VIEW));
        controller.addDataMapDisplayListener(e -> detailLayout.show(detailPanel, e.getDataMap() == null ? EMPTY_VIEW : DATA_MAP_VIEW));
        controller.addObjEntityDisplayListener(e -> detailLayout.show(detailPanel, e.getEntity() == null ? EMPTY_VIEW : OBJ_VIEW));
        controller.addDbEntityDisplayListener(e -> detailLayout.show(detailPanel, e.getEntity() == null ? EMPTY_VIEW : DB_VIEW));
        controller.addProcedureDisplayListener(e -> detailLayout.show(detailPanel, e.getProcedure() == null ? EMPTY_VIEW : PROCEDURE_VIEW));
        controller.addQueryDisplayListener(this::querySelected);
        controller.addMultipleObjectsDisplayListener(e -> detailLayout.show(detailPanel, EMPTY_VIEW));
        controller.addEmbeddableDisplayListener(e -> detailLayout.show(detailPanel, e.getEmbeddable() == null ? EMPTY_VIEW : EMBEDDABLE_VIEW));

        // Moving this to try-catch block per CAY-940. Exception will be stack-traced
        try {
            ComponentGeometry geometry = new ComponentGeometry(this.getClass(), getClass().getSimpleName() + "/splitPane/divider");
            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 300);
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }
    }

    private void querySelected(QueryDisplayEvent e) {
        QueryDescriptor query = e.getQuery();

        switch (query.getType()) {
            case QueryDescriptor.SELECT_QUERY:
                detailLayout.show(detailPanel, SELECT_QUERY_VIEW);
                break;
            case QueryDescriptor.SQL_TEMPLATE:
                detailLayout.show(detailPanel, SQL_TEMPLATE_VIEW);
                break;
            case QueryDescriptor.PROCEDURE_QUERY:
                detailLayout.show(detailPanel, PROCEDURE_QUERY_VIEW);
                break;
            case QueryDescriptor.EJBQL_QUERY:
                detailLayout.show(detailPanel, EJBQL_QUERY_VIEW);
                break;
            default:
                detailLayout.show(detailPanel, EMPTY_VIEW);
        }
    }
}