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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DisplayEvent;
import org.apache.cayenne.modeler.event.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.QueryDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.pref.ProjectStatePreferences;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.CayenneMapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;

public final class ProjectStateUtil {

    private static ProjectStatePreferences preferences;

    private ProjectStateUtil() {
    }

    public static void saveLastState(ProjectController controller) {
        DisplayEvent displayEvent = controller.getLastDisplayEvent();
        Object[] multiplyObjects = controller.getCurrentPaths();

        if (displayEvent == null && multiplyObjects == null) {
            return;
        }

        preferences = controller.getProjectStatePreferences();
        if (preferences.getCurrentPreference() == null) {
            return;
        }

        try {
            preferences.getCurrentPreference().clear();
        } catch (BackingStoreException e) {
            // ignore exception
        }

        if (displayEvent != null) {
            DisplayEventType.valueOf(displayEvent.getClass().getSimpleName()).saveLastDisplayEvent(controller);
        } else {
            DisplayEventType.MultipleObjectsDisplayEvent.saveLastDisplayEvent(controller);
        }
    }

    public static void fireLastState(ProjectController controller) {
        preferences = controller.getProjectStatePreferences();

        String displayEventName = preferences.getEvent();
        if (!displayEventName.isEmpty()) {
            DisplayEventType.valueOf(displayEventName).fireLastDisplayEvent(controller);
        }
    }

    private static enum DisplayEventType {

        DomainDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DomainDisplayEvent domainDisplayEvent = new DomainDisplayEvent(this, dataChannel);
                controller.fireDomainDisplayEvent(domainDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(DomainDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
            }
        },

        DataNodeDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
                if (dataNode == null) {
                    return;
                }

                DataNodeDisplayEvent dataNodeDisplayEvent = new DataNodeDisplayEvent(this, dataChannel, dataNode);
                controller.fireDataNodeDisplayEvent(dataNodeDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(DataNodeDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setNode(controller.getCurrentDataNode().getName());
            }
        },

        DataMapDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                DataMapDisplayEvent dataMapDisplayEvent = new DataMapDisplayEvent(this, dataMap, dataChannel, dataNode);
                controller.fireDataMapDisplayEvent(dataMapDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(DataMapDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setNode(controller.getCurrentDataNode() != null ? controller.getCurrentDataNode().getName() : "");
                preferences.setDataMap(controller.getCurrentDataMap().getName());
            }
        },

        EntityDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Entity entity = getLastEntity(dataMap);
                if (entity == null) {
                    return;
                }

                EntityDisplayEvent entityDisplayEvent = new EntityDisplayEvent(this, entity, dataMap, dataNode, dataChannel);
                if (entity instanceof ObjEntity) {
                    controller.fireObjEntityDisplayEvent(entityDisplayEvent);
                } else if (entity instanceof DbEntity) {
                    controller.fireDbEntityDisplayEvent(entityDisplayEvent);
                }
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                if (controller.getCurrentObjAttributes().length != 0 || controller.getCurrentDbAttributes().length != 0) {
                    AttributeDisplayEvent.saveLastDisplayEvent(controller);
                } else if (controller.getCurrentObjRelationships().length != 0 || controller.getCurrentDbRelationships().length != 0) {
                    RelationshipDisplayEvent.saveLastDisplayEvent(controller);
                } else {
                    preferences.setEvent(EntityDisplayEvent.toString());
                    preferences.setDomain(controller.getCurrentDataChanel().getName());
                    preferences.setNode(controller.getCurrentDataNode() != null ? controller.getCurrentDataNode().getName() : "");
                    preferences.setDataMap(controller.getCurrentDataMap().getName());

                    if (controller.getCurrentObjEntity() != null) {
                        preferences.setObjEntity(controller.getCurrentObjEntity().getName());
                        preferences.setDbEntity(null);
                    } else if (controller.getCurrentDbEntity() != null) {
                        preferences.setDbEntity(controller.getCurrentDbEntity().getName());
                        preferences.setObjEntity(null);
                    }
                }
            }
        },

        AttributeDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Entity entity = getLastEntity(dataMap);
                if (entity == null) {
                    return;
                }

                Attribute[] attributes = getLastEntityAttributes(entity);

                EntityDisplayEvent entityDisplayEvent = new EntityDisplayEvent(this, entity, dataMap, dataNode, dataChannel);
                AttributeDisplayEvent attributeDisplayEvent = new AttributeDisplayEvent(this, attributes, entity, dataMap, dataChannel);

                if (entity instanceof ObjEntity) {
                    controller.fireObjEntityDisplayEvent(entityDisplayEvent);
                    controller.fireObjAttributeDisplayEvent(attributeDisplayEvent);
                } else if (entity instanceof DbEntity) {
                    controller.fireDbEntityDisplayEvent(entityDisplayEvent);
                    controller.fireDbAttributeDisplayEvent(attributeDisplayEvent);
                }
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(AttributeDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setNode(controller.getCurrentDataNode() != null ? controller.getCurrentDataNode().getName() : "");
                preferences.setDataMap(controller.getCurrentDataMap().getName());

                if (controller.getCurrentObjEntity() != null) {
                    preferences.setObjEntity(controller.getCurrentObjEntity().getName());
                    preferences.setObjAttrs(parseToString(controller.getCurrentObjAttributes()));
                    preferences.setDbEntity(null);
                } else if (controller.getCurrentDbEntity() != null) {
                    preferences.setDbEntity(controller.getCurrentDbEntity().getName());
                    preferences.setDbAttrs(parseToString(controller.getCurrentDbAttributes()));
                    preferences.setObjEntity(null);
                }
            }
        },

        RelationshipDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Entity entity = getLastEntity(dataMap);
                if (entity == null) {
                    return;
                }

                Relationship[] relationships = getLastEntityRelationships(entity);

                EntityDisplayEvent entityDisplayEvent = new EntityDisplayEvent(this, entity, dataMap, dataNode, dataChannel);
                RelationshipDisplayEvent displayEvent = new RelationshipDisplayEvent(this, relationships, entity, dataMap, dataChannel);

                if (entity instanceof ObjEntity) {
                    controller.fireObjEntityDisplayEvent(entityDisplayEvent);
                    controller.fireObjRelationshipDisplayEvent(displayEvent);
                } else if (entity instanceof DbEntity) {
                    controller.fireDbEntityDisplayEvent(entityDisplayEvent);
                    controller.fireDbRelationshipDisplayEvent(displayEvent);
                }
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(RelationshipDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setNode(controller.getCurrentDataNode() != null ? controller.getCurrentDataNode().getName() : "");
                preferences.setDataMap(controller.getCurrentDataMap().getName());

                if (controller.getCurrentObjEntity() != null) {
                    preferences.setObjEntity(controller.getCurrentObjEntity().getName());
                    preferences.setObjRels(parseToString(controller.getCurrentObjRelationships()));
                    preferences.setDbEntity(null);
                } else if (controller.getCurrentDbEntity() != null) {
                    preferences.setDbEntity(controller.getCurrentDbEntity().getName());
                    preferences.setDbRels(parseToString(controller.getCurrentDbRelationships()));
                    preferences.setObjEntity(null);
                }
            }
        },

        EmbeddableDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Embeddable embeddable = dataMap.getEmbeddable(preferences.getEmbeddable());
                if (embeddable == null) {
                    return;
                }

                EmbeddableDisplayEvent embeddableDisplayEvent = new EmbeddableDisplayEvent(this, embeddable, dataMap, dataChannel);
                controller.fireEmbeddableDisplayEvent(embeddableDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                if (controller.getCurrentEmbAttributes().length != 0) {
                    EmbeddableAttributeDisplayEvent.saveLastDisplayEvent(controller);
                } else {
                    preferences.setEvent(EmbeddableDisplayEvent.toString());
                    preferences.setDomain(controller.getCurrentDataChanel().getName());
                    preferences.setDataMap(controller.getCurrentDataMap().getName());
                    preferences.setEmbeddable(controller.getCurrentEmbeddable().getClassName());
                }
            }
        },

        EmbeddableAttributeDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Embeddable embeddable = dataMap.getEmbeddable(preferences.getEmbeddable());
                if (embeddable == null) {
                    return;
                }

                EmbeddableDisplayEvent embeddableDisplayEvent = new EmbeddableDisplayEvent(this, embeddable, dataMap, dataChannel);
                controller.fireEmbeddableDisplayEvent(embeddableDisplayEvent);

                EmbeddableAttribute[] embeddableAttributes = getLastEmbeddableAttributes(embeddable);
                EmbeddableAttributeDisplayEvent attributeDisplayEvent = new EmbeddableAttributeDisplayEvent(this, embeddable, embeddableAttributes, dataMap, dataChannel);
                controller.fireEmbeddableAttributeDisplayEvent(attributeDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(EmbeddableAttributeDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setDataMap(controller.getCurrentDataMap().getName());
                preferences.setEmbeddable(controller.getCurrentEmbeddable().getClassName());

                EmbeddableAttribute[] currentEmbAttributes = controller.getCurrentEmbAttributes();
                if (currentEmbAttributes == null) {
                    preferences.setEmbAttrs("");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (EmbeddableAttribute embeddableAttribute : currentEmbAttributes) {
                        sb.append(embeddableAttribute.getName()).append(",");
                    }
                    preferences.setEmbAttrs(sb.toString());
                }
            }
        },

        ProcedureDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Procedure procedure = dataMap.getProcedure(preferences.getProcedure());
                if (procedure == null) {
                    return;
                }

                ProcedureDisplayEvent procedureDisplayEvent = new ProcedureDisplayEvent(this, procedure, dataMap, dataChannel);
                controller.fireProcedureDisplayEvent(procedureDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                if (controller.getCurrentProcedureParameters().length != 0) {
                    ProcedureParameterDisplayEvent.saveLastDisplayEvent(controller);
                } else {
                    preferences.setEvent(ProcedureDisplayEvent.toString());
                    preferences.setDomain(controller.getCurrentDataChanel().getName());
                    preferences.setDataMap(controller.getCurrentDataMap().getName());
                    preferences.setProcedure(controller.getCurrentProcedure().getName());
                }
            }
        },

        ProcedureParameterDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Procedure procedure = dataMap.getProcedure(preferences.getProcedure());
                if (procedure == null) {
                    return;
                }

                ProcedureDisplayEvent procedureDisplayEvent = new ProcedureDisplayEvent(this, procedure, dataMap, dataChannel);
                controller.fireProcedureDisplayEvent(procedureDisplayEvent);

                ProcedureParameter[] procedureParameters = getLastProcedureParameters(procedure);
                ProcedureParameterDisplayEvent procedureParameterDisplayEvent =
                        new ProcedureParameterDisplayEvent(this, procedureParameters, procedure, dataMap, dataChannel);
                controller.fireProcedureParameterDisplayEvent(procedureParameterDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(ProcedureParameterDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setDataMap(controller.getCurrentDataMap().getName());
                preferences.setProcedure(controller.getCurrentProcedure().getName());
                preferences.setProcedureParams(parseToString(controller.getCurrentProcedureParameters()));
            }
        },

        QueryDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
                if (!dataChannel.getName().equals(preferences.getDomain())) {
                    return;
                }

                DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
                if (dataMap == null) {
                    return;
                }

                Query query = dataMap.getQuery(preferences.getQuery());
                if (query == null) {
                    return;
                }

                QueryDisplayEvent queryDisplayEvent = new QueryDisplayEvent(this, query, dataMap, dataChannel);
                controller.fireQueryDisplayEvent(queryDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(QueryDisplayEvent.toString());
                preferences.setDomain(controller.getCurrentDataChanel().getName());
                preferences.setDataMap(controller.getCurrentDataMap().getName());
                preferences.setQuery(controller.getCurrentQuery().getName());
            }
        },

        MultipleObjectsDisplayEvent {
            @Override
            void fireLastDisplayEvent(ProjectController controller) {
                DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();

                String parentObjectName = preferences.getParentObject();
                ConfigurationNode parentObject;
                ConfigurationNode[] multipleObjects;

                if (dataChannel.getDataMap(parentObjectName) != null) {
                    DataMap dataMap = dataChannel.getDataMap(parentObjectName);
                    parentObject = dataMap;
                    multipleObjects = getLastMultipleObjects(dataMap);
                } else if (dataChannel.getNodeDescriptor(parentObjectName) != null) {
                    DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(parentObjectName);
                    parentObject = dataNode;
                    multipleObjects = getLastMultipleObjects(dataNode);
                } else {
                    parentObject = dataChannel;
                    multipleObjects = getLastMultipleObjects(dataChannel);
                }

                MultipleObjectsDisplayEvent multipleDisplayEvent = new MultipleObjectsDisplayEvent(this, multipleObjects, parentObject);
                controller.fireMultipleObjectsDisplayEvent(multipleDisplayEvent);
            }

            @Override
            void saveLastDisplayEvent(ProjectController controller) {
                preferences.setEvent(MultipleObjectsDisplayEvent.toString());
                preferences.setParentObject(getObjectName(controller.getCurrentParentPath()));

                Object[] multipleObjects = controller.getCurrentPaths();
                if (multipleObjects == null) {
                    preferences.setMultipleObjects("");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (Object object : multipleObjects) {
                        String objectName = getObjectName(object);
                        if (!objectName.isEmpty()) {
                            sb.append(objectName).append(",");
                        }
                    }
                    preferences.setMultipleObjects(sb.toString());
                }
            }
        };

        abstract void fireLastDisplayEvent(ProjectController controller);

        abstract void saveLastDisplayEvent(ProjectController controller);
    }

    private static Entity getLastEntity(DataMap dataMap) {
        return !preferences.getObjEntity().isEmpty()
                ? dataMap.getObjEntity(preferences.getObjEntity())
                : dataMap.getDbEntity(preferences.getDbEntity());
    }

    private static Attribute[] getLastEntityAttributes(Entity entity) {
        return (entity instanceof ObjEntity)
                ? getLastObjEntityAttributes((ObjEntity) entity)
                : getLastDbEntityAttributes((DbEntity) entity);
    }

    private static Relationship[] getLastEntityRelationships(Entity entity) {
        return (entity instanceof ObjEntity)
                ? getLastObjEntityRelationships((ObjEntity) entity)
                : getLastDbEntityRelationships((DbEntity) entity);
    }

    private static ObjAttribute[] getLastObjEntityAttributes(ObjEntity objEntity) {
        List<ObjAttribute> attributeList = new ArrayList<ObjAttribute>();
        ObjAttribute[] attributes = new ObjAttribute[0];

        String objAttrs = preferences.getObjAttrs();
        if (!objAttrs.isEmpty()) {
            String[] lastObjAttrs = objAttrs.split(",");
            for (String objAttrName : lastObjAttrs) {
                attributeList.add(objEntity.getAttribute(objAttrName));
            }
        }

        return attributeList.toArray(attributes);
    }

    private static DbAttribute[] getLastDbEntityAttributes(DbEntity dbEntity) {
        List<DbAttribute> attributeList = new ArrayList<DbAttribute>();
        DbAttribute[] attributes = new DbAttribute[0];

        String dbAttrs = preferences.getDbAttrs();
        if (!dbAttrs.isEmpty()) {
            String[] lastDbAttrs = dbAttrs.split(",");
            for (String dbAttrName : lastDbAttrs) {
                attributeList.add(dbEntity.getAttribute(dbAttrName));
            }
        }

        return attributeList.toArray(attributes);
    }

    private static ObjRelationship[] getLastObjEntityRelationships(ObjEntity objEntity) {
        List<ObjRelationship> relationshipList = new ArrayList<ObjRelationship>();
        ObjRelationship[] relationships = new ObjRelationship[0];

        String objRels = preferences.getObjRels();
        if (!objRels.isEmpty()) {
            String[] lastObjRels = objRels.split(",");
            for (String objRelName : lastObjRels) {
                relationshipList.add(objEntity.getRelationship(objRelName));
            }
        }

        return relationshipList.toArray(relationships);
    }

    private static DbRelationship[] getLastDbEntityRelationships(DbEntity dbEntity) {
        List<DbRelationship> relationshipList = new ArrayList<DbRelationship>();
        DbRelationship[] relationships = new DbRelationship[0];

        String dbRels = preferences.getDbRels();
        if (!dbRels.isEmpty()) {
            String[] lastDbRels = dbRels.split(",");
            for (String dbRelName : lastDbRels) {
                relationshipList.add(dbEntity.getRelationship(dbRelName));
            }
        }

        return relationshipList.toArray(relationships);
    }

    private static EmbeddableAttribute[] getLastEmbeddableAttributes(Embeddable embeddable) {
        List<EmbeddableAttribute> embeddableAttributeList = new ArrayList<EmbeddableAttribute>();
        EmbeddableAttribute[] attributes = new EmbeddableAttribute[0];

        String embAttrs = preferences.getEmbAttrs();
        if (!embAttrs.isEmpty()) {
            String[] lastEmbAttrs = embAttrs.split(",");
            for (String embAttrName : lastEmbAttrs) {
                embeddableAttributeList.add(embeddable.getAttribute(embAttrName));
            }
        }

        return embeddableAttributeList.toArray(attributes);
    }

    private static ProcedureParameter[] getLastProcedureParameters(Procedure procedure) {
        List<ProcedureParameter> procedureParameterList = new ArrayList<ProcedureParameter>();
        ProcedureParameter[] parameters = new ProcedureParameter[0];

        String procedureParams = preferences.getProcedureParams();
        if (!procedureParams.isEmpty()) {
            String[] lastProcedureParams = procedureParams.split(",");
            for (String procedureParamName : lastProcedureParams) {
                for (ProcedureParameter procedureParameter : procedure.getCallParameters()) {
                    if (procedureParameter.getName().equals(procedureParamName)) {
                        procedureParameterList.add(procedureParameter);
                    }
                }
            }
        }

        return procedureParameterList.toArray(parameters);
    }

    private static ConfigurationNode[] getLastMultipleObjects(DataChannelDescriptor dataChannel) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<ConfigurationNode>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (!multipleObjects.isEmpty()) {
            String[] lastMultipleObjects = multipleObjects.split(",");

            outer:
            for (String objectName : lastMultipleObjects) {
                if (dataChannel.getName().equals(objectName)) {
                    configurationNodeList.add(dataChannel);
                    continue outer;
                }

                for (DataNodeDescriptor dataNode : dataChannel.getNodeDescriptors()) {
                    if (dataNode.getName().equals(objectName)) {
                        configurationNodeList.add(dataNode);
                        continue outer;
                    }
                }

                for (DataMap dataMap : dataChannel.getDataMaps()) {
                    if (dataMap.getName().equals(objectName)) {
                        configurationNodeList.add(dataMap);
                        continue outer;
                    }
                }
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    private static ConfigurationNode[] getLastMultipleObjects(DataNodeDescriptor dataNode) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<ConfigurationNode>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (!multipleObjects.isEmpty()) {
            String[] lastMultipleObjects = multipleObjects.split(",");
            for (String objectName : lastMultipleObjects) {
                if (dataNode.getDataMapNames().contains(objectName)) {
                    configurationNodeList.add(dataNode.getDataChannelDescriptor().getDataMap(objectName));
                }
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    private static ConfigurationNode[] getLastMultipleObjects(DataMap dataMap) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<ConfigurationNode>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (!multipleObjects.isEmpty()) {
            String[] lastMultipleObjects = multipleObjects.split(",");
            for (String objectName : lastMultipleObjects) {
                if (dataMap.getObjEntity(objectName) != null) {
                    configurationNodeList.add(dataMap.getObjEntity(objectName));
                } else if (dataMap.getDbEntity(objectName) != null) {
                    configurationNodeList.add(dataMap.getDbEntity(objectName));
                } else if (dataMap.getEmbeddable(objectName) != null) {
                    configurationNodeList.add(dataMap.getEmbeddable(objectName));
                } else if (dataMap.getProcedure(objectName) != null) {
                    configurationNodeList.add(dataMap.getProcedure(objectName));
                } else if (dataMap.getQuery(objectName) != null) {
                    configurationNodeList.add(dataMap.getQuery(objectName));
                }
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    private static String getObjectName(Object object) {
        if (object instanceof CayenneMapEntry) {
            return ((CayenneMapEntry) object).getName();
        } else if (object instanceof DataChannelDescriptor) {
            return ((DataChannelDescriptor) object).getName();
        } else if (object instanceof DataNodeDescriptor) {
            return ((DataNodeDescriptor) object).getName();
        } else if (object instanceof DataMap) {
            return ((DataMap) object).getName();
        } else if (object instanceof Embeddable) {
            return ((Embeddable) object).getClassName();
        } else if (object instanceof Query) {
            return ((Query) object).getName();
        } else {
            return "";
        }
    }

    private static String parseToString(CayenneMapEntry[] array) {
        if (array == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (CayenneMapEntry entry : array) {
            sb.append(entry.getName()).append(",");
        }

        return sb.toString();
    }

}
