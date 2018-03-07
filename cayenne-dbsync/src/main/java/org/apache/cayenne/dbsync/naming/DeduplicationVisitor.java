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

import org.apache.cayenne.configuration.ConfigurationNode;
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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * @since 4.0
 */
class DeduplicationVisitor implements ConfigurationNodeVisitor<String> {

    private ConfigurationNode parent;
    private String baseName;
    private String dupesPattern;

    DeduplicationVisitor(ConfigurationNode parent, String baseName, String dupesPattern) {
        this.parent = parent;
        this.baseName = Objects.requireNonNull(baseName);
        this.dupesPattern = Objects.requireNonNull(dupesPattern);
    }

    @Override
    public String visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        // DataChannelDescriptor is top-level. No context or naming conflicts are expected...
        return baseName;
    }

    @Override
    public String visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        return resolve(name -> {
            DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) parent;
            for (DataNodeDescriptor dataNodeDescriptor : dataChannelDescriptor.getNodeDescriptors()) {
                if (dataNodeDescriptor.getName().equals(name)) {
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public String visitDataMap(DataMap dataMap) {
        return resolve(name -> {
            // null context is a situation when DataMap is a
            // top level object of the project
            if (parent == null) {
                return false;
            }

            if (parent instanceof DataChannelDescriptor) {
                DataChannelDescriptor domain = (DataChannelDescriptor) parent;
                return domain.getDataMap(name) != null;
            }
            return false;
        });
    }

    @Override
    public String visitObjEntity(ObjEntity entity) {
        return resolve(name -> ((DataMap) parent).getObjEntity(name) != null);
    }

    @Override
    public String visitDbEntity(DbEntity entity) {
        return resolve(name -> ((DataMap) parent).getDbEntity(name) != null);
    }

    @Override
    public String visitEmbeddable(Embeddable embeddable) {
        return resolve(name -> {
            DataMap map = (DataMap) parent;
            return map.getEmbeddable(map.getNameWithDefaultPackage(name)) != null;
        });
    }

    @Override
    public String visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        return resolve(name -> ((Embeddable) parent).getAttribute(name) != null);
    }

    @Override
    public String visitObjAttribute(ObjAttribute attribute) {
        return resolveObjEntityProperty();
    }

    @Override
    public String visitDbAttribute(DbAttribute attribute) {
        return resolveDbEntityProperty();
    }

    @Override
    public String visitObjRelationship(ObjRelationship relationship) {
        return resolveObjEntityProperty();
    }

    @Override
    public String visitDbRelationship(DbRelationship relationship) {
        return resolveDbEntityProperty();
    }

    @Override
    public String visitProcedure(Procedure procedure) {
        return resolve(name -> ((DataMap) parent).getProcedure(name) != null);
    }

    @Override
    public String visitProcedureParameter(ProcedureParameter parameter) {
        return resolve(name -> {

            // it doesn't matter if we create a parameter with a duplicate name.. parameters are positional anyway..
            // still try to use unique names for visual consistency

            Procedure procedure = (Procedure) parent;
            for (ProcedureParameter parameter1 : procedure.getCallParameters()) {
                if (name.equals(parameter1.getName())) {
                    return true;
                }
            }

            return false;
        });
    }

    @Override
    public String visitQuery(QueryDescriptor query) {
        return resolve(name -> ((DataMap) parent).getQueryDescriptor(name) != null);
    }

    String resolve(Predicate<String> nameChecker) {
        int c = 1;
        String name = baseName;
        while (nameChecker.test(name)) {
            name = String.format(dupesPattern, baseName, c++);
        }

        return name;
    }

    private String resolveDbEntityProperty() {
        return resolve(name -> {
            DbEntity entity = (DbEntity) parent;
            // check if either attribute or relationship name matches...
            return entity.getAttribute(name) != null || entity.getRelationship(name) != null;
        });
    }

    private String resolveObjEntityProperty() {
        return resolve(name -> {
            ObjEntity entity = (ObjEntity) parent;

            // check if either attribute or relationship name matches...
            if (entity.getAttribute(name) != null || entity.getRelationship(name) != null) {
                return true;
            }

            //  check if there's a callback method that shadows attribute getter (unlikely, but still)
            String conflictingCallback = "get" + NameUtil.capitalize(name);
            return entity.getCallbackMethods().contains(conflictingCallback);
        });
    }
}
