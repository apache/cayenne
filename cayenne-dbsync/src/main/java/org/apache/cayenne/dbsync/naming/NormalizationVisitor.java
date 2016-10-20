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
class NormalizationVisitor implements ConfigurationNodeVisitor<String> {

    private String baseName;

    public NormalizationVisitor(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public String visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        return baseName;
    }

    @Override
    public String visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        return baseName;
    }

    @Override
    public String visitDataMap(DataMap dataMap) {
        return baseName;
    }

    @Override
    public String visitObjEntity(ObjEntity entity) {
        return NameUtil.capitalize(baseName);
    }

    @Override
    public String visitDbEntity(DbEntity entity) {
        return baseName;
    }

    @Override
    public String visitEmbeddable(Embeddable embeddable) {
        return NameUtil.capitalize(baseName);
    }

    @Override
    public String visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        return NameUtil.uncapitalize(baseName);
    }

    @Override
    public String visitObjAttribute(ObjAttribute attribute) {
        return NameUtil.uncapitalize(baseName);
    }

    @Override
    public String visitDbAttribute(DbAttribute attribute) {
        return baseName;
    }

    @Override
    public String visitObjRelationship(ObjRelationship relationship) {
        return NameUtil.uncapitalize(baseName);
    }

    @Override
    public String visitDbRelationship(DbRelationship relationship) {
        return NameUtil.uncapitalize(baseName);
    }

    @Override
    public String visitProcedure(Procedure procedure) {
        return baseName;
    }

    @Override
    public String visitProcedureParameter(ProcedureParameter parameter) {
        return baseName;
    }

    @Override
    public String visitQuery(QueryDescriptor query) {
        return baseName;
    }
}
