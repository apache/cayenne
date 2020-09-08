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

import java.awt.Color;
import java.util.Collection;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

/**
 * Class for building ER-graph, based on DbEntity information
 */
class DbGraphBuilder extends BaseGraphBuilder implements DbEntityListener,
        DbAttributeListener, DbRelationshipListener {

    static final Color ENTITY_COLOR = new Color(197, 253, 252);

    @Override
    protected Collection<? extends Entity> getEntities(DataMap map) {
        return map.getDbEntities();
    }

    @Override
    protected void postProcessEntity(Entity entity, DefaultGraphCell cell) {
        super.postProcessEntity(entity, cell);

        GraphConstants.setBackground(cell.getAttributes(), ENTITY_COLOR);
        GraphConstants.setOpaque(cell.getAttributes(), true);
    }

    @Override
    protected EntityCellMetadata getCellMetadata(Entity e) {
        return new DbEntityCellMetadata(this, e.getName());
    }

    @Override
    protected DefaultEdge createRelationshipCell(Relationship rel) {
        DefaultEdge edge = super.createRelationshipCell(rel);
        if (edge != null) {
            GraphConstants.setDashPattern(edge.getAttributes(), new float[] {
                    10, 3
            });
        }
        return edge;
    }

    @Override
    public void setProjectController(ProjectController mediator) {
        super.setProjectController(mediator);

        mediator.addDbEntityListener(this);
        mediator.addDbAttributeListener(this);
        mediator.addDbRelationshipListener(this);
    }

    public void destroy() {
        super.destroy();

        mediator.removeDbEntityListener(this);
        mediator.removeDbAttributeListener(this);
        mediator.removeDbRelationshipListener(this);
    }

    public void dbEntityAdded(EntityEvent e) {
        // skip new entities from DbLoader
        if(e.getEntity() instanceof DetectedDbEntity) {
            return;
        }
        insertEntityCell(e.getEntity());
    }

    public void dbEntityChanged(EntityEvent e) {
        remapEntity(e);

        updateEntityCell(e.getEntity());
    }

    public void dbEntityRemoved(EntityEvent e) {
        removeEntityCell(e.getEntity());
    }

    public void dbAttributeAdded(AttributeEvent e) {
        updateEntityCell(e.getEntity());
    }

    public void dbAttributeChanged(AttributeEvent e) {
        updateEntityCell(e.getEntity());
    }

    public void dbAttributeRemoved(AttributeEvent e) {
        updateEntityCell(e.getEntity());
    }

    public void dbRelationshipAdded(RelationshipEvent e) {
        // nothing because relationship does not have target yet
    }

    public void dbRelationshipChanged(RelationshipEvent e) {
        updateRelationshipCell(e.getRelationship());
    }

    public void dbRelationshipRemoved(RelationshipEvent e) {
        remapRelationship(e);
        removeRelationshipCell(e.getRelationship());
    }

    public GraphType getType() {
        return GraphType.ER;
    }
}
