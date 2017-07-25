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

package org.apache.cayenne.project.extension.info;

import java.util.Map;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
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
import org.apache.cayenne.project.extension.BaseSaverDelegate;
import org.apache.cayenne.util.Util;

/**
 * @since 4.1
 */
class InfoSaverDelegate extends BaseSaverDelegate {

    private DataChannelMetaData metaData;

    InfoSaverDelegate(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    private Void printComment(ConfigurationNode entity) {
        ObjectInfo info = metaData.get(entity, ObjectInfo.class);
        if(info == null) {
            return null;
        }

        for(Map.Entry<String, String> entry : info.getSortedValues().entrySet()) {
            if(!Util.isEmptyString(entry.getValue())) {
                encoder.start("info:property")
                        .attribute("xmlns:info", InfoExtension.NAMESPACE)
                        .attribute("name", entry.getKey())
                        .attribute("value", entry.getValue())
                        .end();
            }
        }
        return null;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        return printComment(dataMap);
    }

    @Override
    public Void visitObjEntity(ObjEntity entity) {
        return printComment(entity);
    }

    @Override
    public Void visitDbEntity(DbEntity entity) {
        return printComment(entity);
    }

    @Override
    public Void visitEmbeddable(Embeddable embeddable) {
        return printComment(embeddable);
    }

    @Override
    public Void visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        return printComment(attribute);
    }

    @Override
    public Void visitObjAttribute(ObjAttribute attribute) {
        return printComment(attribute);
    }

    @Override
    public Void visitDbAttribute(DbAttribute attribute) {
        return printComment(attribute);
    }

    @Override
    public Void visitObjRelationship(ObjRelationship relationship) {
        return printComment(relationship);
    }

    @Override
    public Void visitDbRelationship(DbRelationship relationship) {
        return printComment(relationship);
    }

    @Override
    public Void visitProcedure(Procedure procedure) {
        return printComment(procedure);
    }

    @Override
    public Void visitProcedureParameter(ProcedureParameter parameter) {
        return printComment(parameter);
    }

    @Override
    public Void visitQuery(QueryDescriptor query) {
        return printComment(query);
    }
}
