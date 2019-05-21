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

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;

public enum DisplayEventTypes {

    DomainDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new DomainDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return DomainDisplayEvent.class.getSimpleName();
        }
    },

    DataNodeDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new DataNodeDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return DataNodeDisplayEvent.class.getSimpleName();
        }
    },

    DataMapDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new DataMapDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return DataMapDisplayEvent.class.getSimpleName();
        }
    },

    EntityDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            if (controller.getCurrentObjAttributes().length != 0 || controller.getCurrentDbAttributes().length != 0) {
                return new AttributeDisplayEventType(controller);
            } else if (controller.getCurrentObjRelationships().length != 0 || controller.getCurrentDbRelationships().length != 0) {
                return new RelationshipDisplayEventType(controller);
            } else {
                return new EntityDisplayEventType(controller);
            }
        }

        @Override
        public String toString() {
            return EntityDisplayEvent.class.getSimpleName();
        }
    },

    AttributeDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new AttributeDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return AttributeDisplayEvent.class.getSimpleName();
        }
    },

    RelationshipDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new RelationshipDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return RelationshipDisplayEvent.class.getSimpleName();
        }
    },

    EmbeddableDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            if (controller.getCurrentEmbAttributes().length != 0) {
                return new EmbeddableAttributeDisplayEventType(controller);
            } else {
                return new EmbeddableDisplayEventType(controller);
            }
        }

        @Override
        public String toString() {
            return EmbeddableDisplayEvent.class.getSimpleName();
        }
    },

    EmbeddableAttributeDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new EmbeddableAttributeDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return EmbeddableAttributeDisplayEvent.class.getSimpleName();
        }
    },

    ProcedureDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            if (controller.getCurrentProcedureParameters().length != 0) {
                return new ProcedureParameterDisplayEventType(controller);
            } else {
                return new ProcedureDisplayEventType(controller);
            }
        }

        @Override
        public String toString() {
            return ProcedureDisplayEvent.class.getSimpleName();
        }
    },

    ProcedureParameterDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new ProcedureParameterDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return ProcedureParameterDisplayEvent.class.getSimpleName();
        }
    },

    QueryDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new QueryDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return QueryDisplayEvent.class.getSimpleName();
        }
    },

    MultipleObjectsDisplayEvent {
        @Override
        DisplayEventType createDisplayEventType(ProjectController controller) {
            return new MultipleObjectsDisplayEventType(controller);
        }

        @Override
        public String toString() {
            return MultipleObjectsDisplayEvent.class.getSimpleName();
        }
    };

    abstract DisplayEventType createDisplayEventType(ProjectController controller);

}
