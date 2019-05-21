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

package org.apache.cayenne.configuration;

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

/**
 * @since 4.1
 */
public class EmptyConfigurationNodeVisitor implements ConfigurationNodeVisitor<Void> {
    @Override
    public Void visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        return null;
    }

    @Override
    public Void visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        return null;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        return null;
    }

    @Override
    public Void visitObjEntity(ObjEntity entity) {
        return null;
    }

    @Override
    public Void visitDbEntity(DbEntity entity) {
        return null;
    }

    @Override
    public Void visitEmbeddable(Embeddable embeddable) {
        return null;
    }

    @Override
    public Void visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        return null;
    }

    @Override
    public Void visitObjAttribute(ObjAttribute attribute) {
        return null;
    }

    @Override
    public Void visitDbAttribute(DbAttribute attribute) {
        return null;
    }

    @Override
    public Void visitObjRelationship(ObjRelationship relationship) {
        return null;
    }

    @Override
    public Void visitDbRelationship(DbRelationship relationship) {
        return null;
    }

    @Override
    public Void visitProcedure(Procedure procedure) {
        return null;
    }

    @Override
    public Void visitProcedureParameter(ProcedureParameter parameter) {
        return null;
    }

    @Override
    public Void visitQuery(QueryDescriptor query) {
        return null;
    }
}
