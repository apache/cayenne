/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
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
import org.apache.cayenne.map.QueryDescriptor;

/**
 * @since 4.0
 */
class DefaultBaseNameVisitor implements ConfigurationNodeVisitor<String> {

    static final DefaultBaseNameVisitor INSTANCE = new DefaultBaseNameVisitor();

    private DefaultBaseNameVisitor() {
    }

    @Override
    public String visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        return "project";
    }

    @Override
    public String visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        return "datanode";
    }

    @Override
    public String visitDataMap(DataMap dataMap) {
        return "datamap";
    }

    @Override
    public String visitObjEntity(ObjEntity entity) {
        return "ObjEntity";
    }

    @Override
    public String visitDbEntity(DbEntity entity) {
        return "db_entity";
    }

    @Override
    public String visitEmbeddable(Embeddable embeddable) {
        return "Embeddable";
    }

    @Override
    public String visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        return "untitledAttr";
    }

    @Override
    public String visitObjAttribute(ObjAttribute attribute) {
        return "untitledAttr";
    }

    @Override
    public String visitDbAttribute(DbAttribute attribute) {
        return "untitledAttr";
    }

    @Override
    public String visitObjRelationship(ObjRelationship relationship) {
        return "untitledRel";
    }

    @Override
    public String visitDbRelationship(DbRelationship relationship) {
        return "untitledRel";
    }

    @Override
    public String visitProcedure(Procedure procedure) {
        return "procedure";
    }

    @Override
    public String visitProcedureParameter(ProcedureParameter parameter) {
        return "UntitledProcedureParameter";
    }

    @Override
    public String visitQuery(QueryDescriptor query) {
        return "query";
    }
}
