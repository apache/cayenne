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
package org.apache.cayenne.project;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.Query;

public class DefaultConfigurationNodeParentGetter implements ConfigurationNodeParentGetter {

    private ConfigurationNodeVisitor<ConfigurationNode> parentGetter;

    public DefaultConfigurationNodeParentGetter() {
        parentGetter = new ParentGetter();
    }

    public ConfigurationNode getParent(ConfigurationNode node) {
        return node.acceptVisitor(parentGetter);
    }

    class ParentGetter extends BaseConfigurationNodeVisitor<ConfigurationNode> {

        @Override
        public ConfigurationNode visitDataMap(DataMap dataMap) {
            return dataMap.getDataChannelDescriptor();
        }

        @Override
        public ConfigurationNode visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
            return nodeDescriptor.getDataChannelDescriptor();
        }

        @Override
        public ConfigurationNode visitDbAttribute(DbAttribute attribute) {
            return (ConfigurationNode) attribute.getParent();
        }

        @Override
        public ConfigurationNode visitDbEntity(DbEntity entity) {
            return entity.getDataMap();
        }

        @Override
        public ConfigurationNode visitDbRelationship(DbRelationship relationship) {
            return (ConfigurationNode) relationship.getParent();
        }

        @Override
        public ConfigurationNode visitEmbeddable(Embeddable embeddable) {
            return embeddable.getDataMap();
        }

        @Override
        public ConfigurationNode visitEmbeddableAttribute(EmbeddableAttribute attribute) {
            return attribute.getEmbeddable();
        }

        @Override
        public ConfigurationNode visitObjAttribute(ObjAttribute attribute) {
            return (ConfigurationNode) attribute.getParent();
        }

        @Override
        public ConfigurationNode visitObjEntity(ObjEntity entity) {
            return (ConfigurationNode) entity.getParent();
        }

        @Override
        public ConfigurationNode visitObjRelationship(ObjRelationship relationship) {
            return (ConfigurationNode) relationship.getParent();
        }

        @Override
        public ConfigurationNode visitProcedure(Procedure procedure) {
            return (ConfigurationNode) procedure.getParent();
        }

        @Override
        public ConfigurationNode visitProcedureParameter(ProcedureParameter parameter) {
            return (ConfigurationNode) parameter.getParent();
        }

        @Override
        public ConfigurationNode visitQuery(Query query) {
            return query.getDataMap();
        }
    }
}
