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
package org.apache.cayenne.modeler.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JPopupMenu;
import javax.swing.border.LineBorder;
import javax.swing.event.UndoableEditEvent;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataMapListener;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.graph.action.EntityDisplayAction;
import org.apache.cayenne.modeler.graph.action.RemoveEntityAction;
import org.apache.cayenne.util.XMLEncoder;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.organic.JGraphOrganicLayout;

/**
 * Base class for building graphs of entities
 */
abstract class BaseGraphBuilder implements GraphBuilder, DataMapListener {

    static final Font EDGE_FONT = new Font("Verdana", 0, 10);

    /**
     * Graph
     */
    protected JGraph graph;

    /**
     * Domain
     */
    protected transient DataChannelDescriptor domain;

    /**
     * Created entity cells. Maps to entity name, since GraphBuilder can be
     * serialized
     */
    protected Map<String, DefaultGraphCell> entityCells;

    /**
     * Created relationship cells Maps to relationship qualified name, since
     * GraphBuilder can be serialized
     */
    protected Map<String, DefaultEdge> relCells;

    /**
     * Created non-isolated objects
     */
    protected List<DefaultGraphCell> createdObjects;

    /**
     * Current project controller
     */
    protected transient ProjectController mediator;

    protected transient Entity selectedEntity;

    transient JPopupMenu popup;

    boolean undoEventsDisabled;

    public void buildGraph(ProjectController mediator, DataChannelDescriptor domain, boolean doLayout) {
        if (graph != null) {
            // graph already built, exiting silently
            return;
        }

        graph = new JGraph();
        GraphModel model = new DefaultGraphModel();
        graph.setModel(model);

        setProjectController(mediator);
        setDataDomain(domain);

        GraphLayoutCache view = new GraphLayoutCache(model, new DefaultCellViewFactory());
        graph.setGraphLayoutCache(view);

        graph.addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Object selected = graph.getSelectionCell();
                    if (selected != null && selected instanceof DefaultGraphCell) {
                        Object userObject = ((DefaultGraphCell) selected).getUserObject();
                        if (userObject instanceof EntityCellMetadata) {
                            showPopup(e.getPoint(), ((EntityCellMetadata) userObject).fetchEntity());
                        }
                    }
                }
            }
        });

        graph.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent e) {
                // limit scale
                double scale = graph.getScale() / Math.pow(ZOOM_FACTOR, e.getWheelRotation());
                scale = Math.max(scale, 0.1);
                scale = Math.min(scale, 3);
                graph.setScale(scale);
            }
        });

        entityCells = new HashMap<>();
        createdObjects = new ArrayList<>();
        relCells = new HashMap<>();

        /*
         * an array for entities that are not connected to anyone. We add them
         * separately so that layout doesn't touch them
         */
        List<DefaultGraphCell> isolatedObjects = new ArrayList<DefaultGraphCell>();

        /*
         * 1. Add all entities
         */
        for (DataMap map : domain.getDataMaps()) {
            DefaultGraphCell mapCell = new DefaultGraphCell();
            createdObjects.add(mapCell);

            for (Entity entity : getEntities(map)) {
                DefaultGraphCell cell = createEntityCell(entity);

                // mapCell.add(cell);
                // cell.setParent(mapCell);

                List<DefaultGraphCell> array = !isIsolated(domain, entity) ? createdObjects : isolatedObjects;
                array.add(cell);
                array.add((DefaultGraphCell) cell.getChildAt(0)); // port
            }
        }

        /*
         * 2. Add all relationships
         */
        for (DataMap map : domain.getDataMaps()) {
            for (Entity entity : getEntities(map)) {
                DefaultGraphCell sourceCell = entityCells.get(entity.getName());

                postProcessEntity(entity, sourceCell);
            }
        }
        view.insert(createdObjects.toArray());

        if (doLayout) {
            JGraphFacade facade = new JGraphFacade(graph);

            JGraphOrganicLayout layout = new JGraphOrganicLayout();
            layout.setNodeDistributionCostFactor(5000000000000.0);
            layout.setEdgeLengthCostFactor(1000);
            layout.setEdgeCrossingCostFactor(1000000);
            layout.setOptimizeBorderLine(false);
            layout.setOptimizeEdgeDistance(false);

            // JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
            // layout.setInterHierarchySpacing(150.0);
            // layout.setIntraCellSpacing(150.0);
            // layout.setInterRankCellSpacing(150.0);

            // JGraphSimpleLayout layout = new JGraphSimpleLayout(JGraphSimpleLayout.TYPE_TILT, 4000, 2000);
            layout.run(facade);
            Map nested = facade.createNestedMap(true, true); // Obtain a map of
                                                             // the
                                                             // resulting
                                                             // attribute
                                                             // changes from the
                                                             // facade

            edit(nested); // Apply the results to the actual graph
        }

        /*
         * Adding isolated objects
         * 
         * We're placing them so that they will take maximum space in left top
         * corner. The sample order is below:
         * 
         * 1 2 6 7... 3 5 8 ... 4 9... 10 ...
         */
        if (isolatedObjects.size() > 0) {
            int n = isolatedObjects.size() / 2; // number of isolated entities
            int x = (int) Math.ceil((Math.sqrt(1 + 8 * n) - 1) / 2); // side of
                                                                     // triangle

            Dimension pref = graph.getPreferredSize();
            int dx = pref.width / 2 / x; // x-distance between entities
            int dy = pref.height / 2 / x; // y-distance between entities

            int posX = dx / 2;
            int posY = dy / 2;

            int row = 0;

            for (int isolatedIndex = 0; isolatedIndex < isolatedObjects.size();) {
                for (int i = 0; isolatedIndex < isolatedObjects.size() && i < x - row; i++) {
                    GraphConstants.setBounds(isolatedObjects.get(isolatedIndex).getAttributes(),
                            new Rectangle2D.Double(pref.width - posX, pref.height - 3 * posY / 2, 10, 10));
                    isolatedIndex += 2; // because every 2nd object is port
                    posX += dx;
                }
                posX = dx / 2;
                posY += dy / 2;
                row++;
            }
        }

        view.insert(isolatedObjects.toArray());
        graph.getModel().addUndoableEditListener(this);
    }

    protected DefaultGraphCell createEntityCell(Entity entity) {
        DefaultGraphCell cell = new DefaultGraphCell(getCellMetadata(entity));

        GraphConstants.setResize(cell.getAttributes(), true);
        GraphConstants.setBorder(cell.getAttributes(), new LineBorder(Color.BLACK));

        GraphConstants.setEditable(cell.getAttributes(), false);
        entityCells.put(entity.getName(), cell);

        cell.addPort();
        return cell;
    }

    public DefaultGraphCell getEntityCell(String entityName) {
        return entityCells.get(entityName);
    }

    /**
     * Post (i.e. after creation on entity cell) process of the entity
     */
    protected void postProcessEntity(Entity entity, DefaultGraphCell cell) {
        for (Relationship rel : entity.getRelationships()) {
            if (rel.getSourceEntity() != null && rel.getTargetEntity() != null) {
                DefaultEdge edge = createRelationshipCell(rel);
                if (edge != null) {
                    createdObjects.add(edge);
                }
            }
        }
    }

    /**
     * Returns whether an entity is not connected to any other TODO: not fine
     * algorithm, it iterates through all entities and all rels
     */
    protected boolean isIsolated(DataChannelDescriptor domain, Entity entity) {
        if (entity.getRelationships().size() == 0) {
            // searching for rels that have a target="entity"

            for (DataMap map : domain.getDataMaps()) {
                for (Entity source : getEntities(map)) {
                    if (source.getAnyRelationship(entity) != null) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    protected abstract Collection<? extends Entity> getEntities(DataMap map);

    /**
     * Returns label for relationship on the graph, considering its "mandatory"
     * and "to-many" properties
     */
    private static String getRelationshipLabel(Relationship rel) {
        if (rel == null) {
            return null;
        }

        if (rel.isToMany()) {
            return "0..*";
        }
        return rel.isMandatory() ? "1" : "0..1";
    }

    /**
     * Returns metadata (user object) for this cell
     */
    protected abstract EntityCellMetadata getCellMetadata(Entity e);

    protected void showPopup(Point p, Entity entity) {
        selectedEntity = entity;
        if (popup == null) {
            popup = createPopupMenu();
        }
        popup.show(graph, p.x, p.y);
    }

    public Entity getSelectedEntity() {
        return selectedEntity;
    }

    /**
     * Creates popup menu
     */
    protected JPopupMenu createPopupMenu() {
        ActionManager actionManager = Application.getInstance().getActionManager();

        JPopupMenu menu = new JPopupMenu();
        menu.add(new EntityDisplayAction(this).buildMenu());
        menu.addSeparator();
        menu.add(new EntityDisplayAction(this, actionManager.getAction(CreateAttributeAction.class)).buildMenu());
        menu.add(new EntityDisplayAction(this, actionManager.getAction(CreateRelationshipAction.class)).buildMenu());
        menu.addSeparator();
        menu.add(new RemoveEntityAction(this));

        return menu;
    }

    /**
     * Updates specified entity on the graph
     */
    protected void updateEntityCell(Entity e) {
        DefaultGraphCell cell = entityCells.get(e.getName());
        if (cell != null) {
            GraphConstants.setValue(cell.getAttributes(), getCellMetadata(e));
            GraphConstants.setResize(cell.getAttributes(), true);

            Map nested = new HashMap();
            nested.put(cell, cell.getAttributes());

            edit(nested);
        }
    }

    protected void updateRelationshipCell(Relationship rel) {
        if (rel.getSourceEntity() != null && rel.getTargetEntity() != null) {
            DefaultEdge edge = relCells.get(getQualifiedName(rel));
            if (edge != null) {
                updateRelationshipLabels(edge, rel, rel.getReverseRelationship());

                Map nested = new HashMap();
                nested.put(edge, edge.getAttributes());
                edit(nested);
            } else {
                insertRelationshipCell(rel);
            }
        }
    }

    protected void removeEntityCell(Entity e) {
        final DefaultGraphCell cell = entityCells.get(e.getName());
        if (cell != null) {
            runWithUndoDisabled(new Runnable() {

                public void run() {
                    graph.getGraphLayoutCache().remove(new Object[] { cell }, true, true);
                }
            });
            entityCells.remove(e.getName());
        }
    }

    protected void removeRelationshipCell(Relationship rel) {
        final DefaultEdge edge = relCells.get(getQualifiedName(rel));
        if (edge != null) {
            runWithUndoDisabled(new Runnable() {

                public void run() {
                    graph.getGraphLayoutCache().remove(new Object[] { edge });
                }
            });
            relCells.remove(getQualifiedName(rel));
        }
    }

    protected DefaultEdge createRelationshipCell(Relationship rel) {
        if (!relCells.containsKey(getQualifiedName(rel))) {
            Relationship reverse = rel.getReverseRelationship();

            DefaultEdge edge = new DefaultEdge();

            // GraphConstants.setLineStyle(edge.getAttributes(),
            // GraphConstants.STYLE_ORTHOGONAL);
            // GraphConstants.setRouting(edge.getAttributes(),
            // GraphConstants.ROUTING_SIMPLE);

            GraphConstants.setEditable(edge.getAttributes(), false);
            GraphConstants.setLabelAlongEdge(edge.getAttributes(), true);
            GraphConstants.setSelectable(edge.getAttributes(), false);
            GraphConstants.setFont(edge.getAttributes(), EDGE_FONT);

            updateRelationshipLabels(edge, rel, reverse);

            relCells.put(getQualifiedName(rel), edge);

            if (reverse != null) {
                relCells.put(getQualifiedName(reverse), edge);
            }

            return edge;
        }
        return null;
    }

    protected void insertRelationshipCell(Relationship rel) {
        DefaultEdge edge = createRelationshipCell(rel);
        insert(edge);
    }

    protected void insertEntityCell(Entity entity) {
        DefaultGraphCell cell = createEntityCell(entity);

        // putting cell to a random posistion..
        GraphConstants.setBounds(cell.getAttributes(),
                new Rectangle2D.Double(Math.random() * graph.getWidth(), Math.random() * graph.getHeight(), 10, 10));

        // setting graph type-specific attrs
        postProcessEntity(entity, cell);

        insert(cell);
    }

    /**
     * Updates relationship labels for specified relationship edge.
     */
    protected void updateRelationshipLabels(DefaultEdge edge, Relationship rel, Relationship reverse) {
        DefaultGraphCell sourceCell = entityCells.get(rel.getSourceEntity().getName());
        DefaultGraphCell targetCell = entityCells.get(rel.getTargetEntity().getName());

        edge.setSource(sourceCell != null ? sourceCell.getChildAt(0) : null);
        edge.setTarget(targetCell != null ? targetCell.getChildAt(0) : null);

        Object[] labels = { rel.getName() + " " + getRelationshipLabel(rel),
                reverse == null ? "" : reverse.getName() + " " + getRelationshipLabel(reverse) };
        GraphConstants.setExtraLabels(edge.getAttributes(), labels);

        Point2D[] labelPositions = { new Point2D.Double(GraphConstants.PERMILLE * (0.1 + 0.2 * Math.random()), 10),
                new Point2D.Double(GraphConstants.PERMILLE * (0.9 - 0.2 * Math.random()), -10) };
        GraphConstants.setExtraLabelPositions(edge.getAttributes(), labelPositions);
    }

    public JGraph getGraph() {
        return graph;
    }

    public void dataMapAdded(DataMapEvent e) {
    }

    public void dataMapChanged(DataMapEvent e) {
    }

    public void dataMapRemoved(DataMapEvent e) {
        for (Entity entity : getEntities(e.getDataMap())) {
            removeEntityCell(entity);
        }
    }

    public void setProjectController(ProjectController mediator) {
        this.mediator = mediator;

        mediator.addDataMapListener(this);
    }

    public void setDataDomain(DataChannelDescriptor domain) {
        this.domain = domain;
    }

    public DataChannelDescriptor getDataDomain() {
        return domain;
    }

    public void destroy() {
        mediator.removeDataMapListener(this);
    }

    /**
     * Checks if entity name has changed, then changes map key
     */
    protected void remapEntity(EntityEvent e) {
        if (e.isNameChange()) {
            entityCells.put(e.getNewName(), entityCells.remove(e.getOldName()));
        }
    }

    /**
     * Checks if entity name has changed, then changes map key
     */
    protected void remapRelationship(RelationshipEvent e) {
        if (e.isNameChange()) {
            relCells.put(getQualifiedName(e.getRelationship()),
                    relCells.remove(e.getEntity().getName() + "." + e.getOldName()));
        }
    }

    /**
     * Returns qualified name (entity name + relationship name) for a
     * relationship
     */
    static String getQualifiedName(Relationship rel) {
        return rel.getSourceEntity().getName() + "." + rel.getName();
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<graph type=\"");
        encoder.print(getType().toString());
        encoder.print("\" scale=\"");
        encoder.print(String.valueOf(graph.getScale()));
        encoder.println("\">");
        encoder.indent(1);

        for (Entry<String, DefaultGraphCell> entry : entityCells.entrySet()) {
            encoder.print("<entity name=\"");
            encoder.print(entry.getKey());
            encoder.print("\" ");

            DefaultGraphCell cell = entry.getValue();
            Rectangle2D rect = graph.getCellBounds(cell);
            encodeRecangle(encoder, rect);
            encoder.println("/>");
        }

        encoder.indent(-1);
        encoder.println("</graph>");
    }

    private void encodeRecangle(XMLEncoder encoder, Rectangle2D rect) {
        encoder.print("x=\"");
        encoder.print(rect.getX() + "\" y=\"");
        encoder.print(rect.getY() + "\" width=\"");
        encoder.print(rect.getWidth() + "\" height=\"");
        encoder.print(rect.getHeight() + "\" ");
    }

    private void edit(final Map map) {
        runWithUndoDisabled(new Runnable() {

            public void run() {
                graph.getGraphLayoutCache().edit(map);
            }
        });
    }

    private void insert(final Object cell) {
        runWithUndoDisabled(new Runnable() {

            public void run() {
                graph.getGraphLayoutCache().insert(cell);
            }
        });
    }

    private void runWithUndoDisabled(Runnable r) {
        undoEventsDisabled = true;
        try {
            r.run();
        } finally {
            undoEventsDisabled = false;
        }
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        if (!undoEventsDisabled) {
            // graph has been modified
            mediator.setDirty(true);

            Application.getInstance().getUndoManager().undoableEditHappened(e);
        }
    }
}
