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
package org.apache.cayenne.projecttools;

import org.apache.cayenne.project.BaseProjectNodeVisitor;
import org.apache.cayenne.project.ProjectNode;
import org.apache.cayenne.project.ProjectNodeVisitor;
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
import org.apache.cayenne.map.QueryDescriptor;

public class DefaultProjectNodeParentGetter implements ProjectNodeParentGetter {

    private ProjectNodeVisitor<ProjectNode> parentGetter;

    public DefaultProjectNodeParentGetter() {
        parentGetter = new ParentGetter();
    }

    public ProjectNode getParent(ProjectNode node) {
        return node.acceptVisitor(parentGetter);
    }

    class ParentGetter extends BaseProjectNodeVisitor<ProjectNode> {

        @Override
        public ProjectNode visitDataMap(DataMap dataMap) {
            return dataMap.getProject();
        }

        @Override
        public ProjectNode visitDbAttribute(DbAttribute attribute) {
            return attribute.getEntity();
        }

        @Override
        public ProjectNode visitDbEntity(DbEntity entity) {
            return entity.getDataMap();
        }

        @Override
        public ProjectNode visitDbRelationship(DbRelationship relationship) {
            return relationship.getSourceEntity();
        }

        @Override
        public ProjectNode visitEmbeddable(Embeddable embeddable) {
            return embeddable.getDataMap();
        }

        @Override
        public ProjectNode visitEmbeddableAttribute(EmbeddableAttribute attribute) {
            return attribute.getEmbeddable();
        }

        @Override
        public ProjectNode visitObjAttribute(ObjAttribute attribute) {
            return attribute.getEntity();
        }

        @Override
        public ProjectNode visitObjEntity(ObjEntity entity) {
            return entity.getDataMap();
        }

        @Override
        public ProjectNode visitObjRelationship(ObjRelationship relationship) {
            return relationship.getSourceEntity();
        }

        @Override
        public ProjectNode visitProcedure(Procedure procedure) {
            return procedure.getDataMap();
        }

        @Override
        public ProjectNode visitProcedureParameter(ProcedureParameter parameter) {
            return parameter.getProcedure();
        }

        @Override
        public ProjectNode visitQuery(QueryDescriptor query) {
            return query.getDataMap();
        }
    }
}
