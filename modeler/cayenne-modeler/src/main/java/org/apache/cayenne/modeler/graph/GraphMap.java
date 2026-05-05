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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Map that stores graph builders <b>for a single domain</b> by their type
 * and has additional methods to set currently selected graph and serialize to XML
 */
public class GraphMap extends HashMap<GraphType, GraphBuilder> {
    /**
     * type that is currently selected
     */
    GraphType selectedType;

    /**
     * Domain
     */
    DataChannelDescriptor domain;

    /**
     * Graph state parsed from the project XML and waiting for a {@link GraphBuilder}
     * to be created — applied at first {@link #createGraphBuilder} for a given type.
     * Kept off the live event bus so XML load doesn't subscribe to a not-yet-open project.
     */
    private final Map<GraphType, ParkedState> parkedStates = new EnumMap<>(GraphType.class);

    public GraphMap(DataChannelDescriptor domain) {
        this.domain = domain;
    }

    /**
     * Returns domain
     */
    public DataChannelDescriptor getDomain() {
        return domain;
    }

    /**
     * Returns type that is currently selected
     */
    public GraphType getSelectedType() {
        return selectedType;
    }

    /**
     * Sets type that is currently selected
     */
    public void setSelectedType(GraphType selectedType) {
        this.selectedType = selectedType;
    }

    public GraphBuilder createGraphBuilder(ProjectSession session, GraphType type, boolean doLayout) {
        try {
            ParkedState parked = parkedStates.remove(type);
            GraphBuilder builder = type.getBuilderClass().getDeclaredConstructor().newInstance();
            builder.buildGraph(session, domain, doLayout && parked == null);
            if (parked != null) {
                parked.applyTo(builder);
            }
            put(type, builder);

            return builder;
        } catch (Exception e) {
            throw new CayenneRuntimeException("Could not instantiate GraphBuilder", e);
        }
    }

    /**
     * Stashes graph state parsed from the project XML for a given type. The state is
     * applied to a freshly built {@link GraphBuilder} the first time
     * {@link #createGraphBuilder} is invoked for that type.
     */
    public void parkParsedState(GraphType type, double scale, Map<String, Map<String, ?>> entityProperties) {
        parkedStates.put(type, new ParkedState(scale, entityProperties));
    }

    /**
     * Returns true if there is parsed state waiting to be applied for the given type.
     */
    public boolean hasParkedState(GraphType type) {
        return parkedStates.containsKey(type);
    }

    private static final class ParkedState {
        private final double scale;
        private final Map<String, Map<String, ?>> entityProperties;

        ParkedState(double scale, Map<String, Map<String, ?>> entityProperties) {
            this.scale = scale;
            this.entityProperties = entityProperties;
        }

        void applyTo(GraphBuilder builder) {
            builder.getGraph().setScale(scale);

            Map<DefaultGraphCell, Map<String, ?>> cellProperties = new HashMap<>();
            for (Map.Entry<String, Map<String, ?>> entry : entityProperties.entrySet()) {
                DefaultGraphCell cell = builder.getEntityCell(entry.getKey());
                cellProperties.put(cell, entry.getValue());
            }

            // apply without polluting the undo stack
            builder.getGraph().getGraphLayoutCache().getModel().removeUndoableEditListener(builder);
            builder.getGraph().getGraphLayoutCache().edit(cellProperties, null, null, new UndoableEdit[0]);
            builder.getGraph().getGraphLayoutCache().getModel().addUndoableEditListener(builder);
        }
    }
}
