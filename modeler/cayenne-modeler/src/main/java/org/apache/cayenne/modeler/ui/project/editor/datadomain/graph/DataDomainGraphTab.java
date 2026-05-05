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
package org.apache.cayenne.modeler.ui.project.editor.datadomain.graph;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayListener;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.graph.GraphBuilder;
import org.apache.cayenne.modeler.graph.GraphRegistry;
import org.apache.cayenne.modeler.graph.GraphType;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.RebuildGraphAction;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.SaveAsImageAction;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.ShowGraphEntityAction;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.ZoomInAction;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.graph.action.ZoomOutAction;
import org.jgraph.JGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Tab for editing graphical representation of a dataDomain
 */
public class DataDomainGraphTab extends ProjectPanel implements DomainDisplayListener, ItemListener {

    JComboBox<String> diagramCombo;
    JScrollPane scrollPane;
    JGraph graph;
    DataChannelDescriptor domain;
    boolean needRebuild;
    GraphRegistry graphRegistry;

    public DataDomainGraphTab(ProjectSession session) {
        super(session);

        needRebuild = true;
        session.addDomainDisplayListener(this);
        session.addObjEntityDisplayListener(this::entityFocused);
        session.addDbEntityDisplayListener(this::entityFocused);

        setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        GraphType[] types = GraphType.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }

        diagramCombo = new CMComboBox<>(names);
        diagramCombo.addItemListener(this);

        toolbar.add(new RebuildGraphAction(this,app()).buildButton(1));
        toolbar.add(new SaveAsImageAction(this, app()).buildButton(3));
        toolbar.addSeparator();
        toolbar.add(new ZoomInAction(this, app()).buildButton(1));
        toolbar.add(new ZoomOutAction(this, app()).buildButton(3));

        toolbar.addSeparator();
        toolbar.add(new JLabel("Diagram: "));
        toolbar.add(diagramCombo);
        add(toolbar, BorderLayout.NORTH);

        scrollPane = new JScrollPane();
        add(scrollPane);
    }

    public void domainSelected(DomainDisplayEvent e) {
        if (domain != e.getDomain()) {
            needRebuild = true;
            domain = e.getDomain();

            if (isVisible()) {
                refresh();
            }
        }
    }

    private void entityFocused(ObjEntityDisplayEvent e) {
        if (e.getSource() instanceof ShowGraphEntityAction) {
            focusEntity(e.getEntity(), e.getDomain());
        }
    }

    private void entityFocused(DbEntityDisplayEvent e) {
        if (e.getSource() instanceof ShowGraphEntityAction) {
            focusEntity(e.getEntity(), e.getDomain());
        }
    }

    private void focusEntity(Entity<?, ?, ?> entity, DataChannelDescriptor entityDomain) {
        if (entity == null) {
            return;
        }

        // align graph domain to selection if needed
        if (domain != entityDomain) {
            domain = entityDomain;
            needRebuild = true;
        }

        // choose type of diagram
        diagramCombo.setSelectedIndex(entity instanceof ObjEntity ? 1 : 0);
        refresh();

        GraphBuilder builder = getGraphRegistry().getGraphMap(domain).get(getSelectedType());

        Object cell = builder.getEntityCell(entity.getName());

        if (cell != null) {
            graph.setSelectionCell(cell);
            graph.scrollCellToVisible(cell);
        }
    }

    /**
     * Rebuilds graph from a domain, if it is not yet built Otherwise, takes it
     * from cache
     */
    public void refresh() {
        if (needRebuild && domain != null) {
            graph = getGraphRegistry().loadGraph(session(), domain, getSelectedType());
            scrollPane.setViewportView(graph);

            needRebuild = false;
        }
    }

    private GraphType getSelectedType() {
        return GraphType.values()[diagramCombo.getSelectedIndex()];
    }

    /**
     * Rebuilds graph, deleting existing if needed
     */
    public void rebuild() {
        if (domain != null) {
            JOptionPane pane = new JOptionPane("Rebuilding graph from domain will cause all user"
                    + " changes to be lost. Continue?", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);

            JDialog dialog = pane.createDialog(app().getFrame(), "Confirm Rebuild");
            dialog.setVisible(true);

            if (pane.getValue().equals(JOptionPane.YES_OPTION)) {
                getGraphRegistry().getGraphMap(domain).remove(getSelectedType());
                itemStateChanged(null);
            }
        }
    }

    public void itemStateChanged(ItemEvent e) {
        needRebuild = true;
        refresh();
    }

    public JGraph getGraph() {
        return graph;
    }

    GraphRegistry getGraphRegistry() {
        graphRegistry = app().getMetaData().get(domain, GraphRegistry.class);
        if (graphRegistry == null) {
            graphRegistry = new GraphRegistry();
            app().getMetaData().add(domain, graphRegistry);
        }

        return graphRegistry;
    }
}
