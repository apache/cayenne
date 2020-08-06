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
package org.apache.cayenne.modeler.graph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.graph.action.RebuildGraphAction;
import org.apache.cayenne.modeler.graph.action.SaveAsImageAction;
import org.apache.cayenne.modeler.graph.action.ZoomInAction;
import org.apache.cayenne.modeler.graph.action.ZoomOutAction;
import org.jgraph.JGraph;

/**
 * Tab for editing graphical representation of a dataDomain
 */
public class DataDomainGraphTab extends JPanel implements DomainDisplayListener, ItemListener {

    /**
     * mediator instance
     */
    ProjectController mediator;

    /**
     * Diagram selection combo
     */
    JComboBox diagramCombo;

    /**
     * Scrollpane that the graph will be added to
     */
    JScrollPane scrollPane;

    /**
     * Current graph
     */
    JGraph graph;

    /**
     * Current domain
     */
    DataChannelDescriptor domain;

    /**
     * True to invoke rebuild next time component becomes visible
     */
    boolean needRebuild;

    GraphRegistry graphRegistry;

    public DataDomainGraphTab(ProjectController mediator) {
        this.mediator = mediator;
        initView();
    }

    private void initView() {
        needRebuild = true;
        mediator.addDomainDisplayListener(this);

        setLayout(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        GraphType[] types = GraphType.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getName();
        }

        diagramCombo = Application.getWidgetFactory().createComboBox(names, false);
        diagramCombo.addItemListener(this);

        toolbar.add(new RebuildGraphAction(this, Application.getInstance()).buildButton(1));
        toolbar.add(new SaveAsImageAction(this, Application.getInstance()).buildButton(3));
        toolbar.addSeparator();
        toolbar.add(new ZoomInAction(this, Application.getInstance()).buildButton(1));
        toolbar.add(new ZoomOutAction(this, Application.getInstance()).buildButton(3));

        toolbar.addSeparator();
        toolbar.add(new JLabel("Diagram: "));
        toolbar.add(diagramCombo);
        add(toolbar, BorderLayout.NORTH);

        scrollPane = new JScrollPane();
        add(scrollPane);
    }

    public void currentDomainChanged(DomainDisplayEvent e) {
        if (e instanceof EntityDisplayEvent) {
            // selecting an event

            // choose type of diagram
            Entity entity = ((EntityDisplayEvent) e).getEntity();
            diagramCombo.setSelectedIndex(entity instanceof ObjEntity ? 1 : 0);
            refresh();

            GraphBuilder builder = getGraphRegistry().getGraphMap(domain).get(getSelectedType());

            Object cell = builder.getEntityCell(entity.getName());

            if (cell != null) {
                graph.setSelectionCell(cell);
                graph.scrollCellToVisible(cell);
            }
        } else if (domain != e.getDomain()) {
            needRebuild = true;
            domain = e.getDomain();

            if (isVisible()) {
                refresh();
            }
        }
    }

    /**
     * Rebuilds graph from a domain, if it is not yet built Otherwise, takes it
     * from cache
     */
    public void refresh() {
        if (needRebuild && domain != null) {
            graph = getGraphRegistry().loadGraph(mediator, domain, getSelectedType());
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

            JDialog dialog = pane.createDialog(Application.getFrame(), "Confirm Rebuild");
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
        graphRegistry = mediator.getApplication().getMetaData().get(domain, GraphRegistry.class);
        if (graphRegistry == null) {
            graphRegistry = new GraphRegistry();
            mediator.getApplication().getMetaData().add(domain, graphRegistry);
        }

        return graphRegistry;
    }
}
