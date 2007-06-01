/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.ProjectTreeView;
import org.objectstyle.cayenne.modeler.editor.datanode.DataNodeEditor;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.event.DbEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.DomainDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DomainDisplayListener;
import org.objectstyle.cayenne.modeler.event.EntityDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ObjEntityDisplayListener;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayEvent;
import org.objectstyle.cayenne.modeler.event.ProcedureDisplayListener;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.modeler.pref.ComponentGeometry;
import org.objectstyle.cayenne.pref.Domain;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Main display area split into the project navigation tree on the left and selected
 * object editor on the right.
 */
public class EditorView extends JPanel implements ObjEntityDisplayListener,
        DbEntityDisplayListener, DomainDisplayListener, DataMapDisplayListener,
        DataNodeDisplayListener, ProcedureDisplayListener, QueryDisplayListener {

    private static final String EMPTY_VIEW = "Empty";
    private static final String DOMAIN_VIEW = "Domain";
    private static final String NODE_VIEW = "Node";
    private static final String DATA_MAP_VIEW = "DataMap";
    private static final String OBJ_VIEW = "ObjView";
    private static final String DB_VIEW = "DbView";
    private static final String PROCEDURE_VIEW = "ProcedureView";
    private static final String SELECT_QUERY_VIEW = "SelectQueryView";
    private static final String SQL_TEMPLATE_VIEW = "SQLTemplateView";
    private static final String PROCEDURE_QUERY_VIEW = "ProcedureQueryView";

    protected ProjectController eventController;
    protected JSplitPane splitPane;
    protected Container detailPanel;

    protected CardLayout detailLayout;

    public EditorView(ProjectController eventController) {
        this.eventController = eventController;
        initView();
        initController();
    }

    private void initView() {

        // init widgets
        ProjectTreeView treePanel = new ProjectTreeView(eventController);
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
        detailPanel.add(new JScrollPane(nodeController.getView()), NODE_VIEW);

        Component dataMapView = new DataMapView(eventController);
        detailPanel.add(new JScrollPane(dataMapView), DATA_MAP_VIEW);

        Component procedureView = new ProcedureTabbedView(eventController);
        detailPanel.add(procedureView, PROCEDURE_VIEW);

        Component selectQueryView = new SelectQueryTabbedView(eventController);
        detailPanel.add(selectQueryView, SELECT_QUERY_VIEW);

        Component sqlTemplateView = new SQLTemplateTabbedView(eventController);
        detailPanel.add(sqlTemplateView, SQL_TEMPLATE_VIEW);

        Component procedureQueryView = new ProcedureQueryView(eventController);
        detailPanel.add(new JScrollPane(procedureQueryView), PROCEDURE_QUERY_VIEW);

        Component objDetailView = new ObjEntityTabbedView(eventController);
        detailPanel.add(objDetailView, OBJ_VIEW);

        Component dbDetailView = new DbEntityTabbedView(eventController);
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

        Domain domain = eventController.getApplicationPreferenceDomain().getSubdomain(
                this.getClass());
        ComponentGeometry geometry = (ComponentGeometry) domain.getDetail(
                "splitPane.divider",
                ComponentGeometry.class,
                true);

        geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 150);
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
        else {
            detailLayout.show(detailPanel, EMPTY_VIEW);
        }
    }
}