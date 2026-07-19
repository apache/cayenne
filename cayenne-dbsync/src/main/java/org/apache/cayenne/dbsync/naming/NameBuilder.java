/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * A builder of names for model objects. Ensures that newly generated names do not conflict with the names of siblings
 * under the same parent node. Name generation can be performed based on default base names for each model object type,
 * or with a user-provided base name.
 *
 * @since 4.0
 */
public class NameBuilder {

    /**
     * Creates a builder for naming the given node under the given parent. The parent may be null for a top-level
     * node (such as a project root) that has no siblings to clash with.
     */
    public static NameBuilder of(ConfigurationNode node, ConfigurationNode parent) {
        return new NameBuilder(node, parent);
    }

    private final ConfigurationNode nodeToName;
    private final ConfigurationNode parent;
    private String dupesPattern;
    private String preferredName;

    private NameBuilder(ConfigurationNode nodeToName, ConfigurationNode parent) {
        this.nodeToName = Objects.requireNonNull(nodeToName);
        this.parent = parent;
        this.dupesPattern = "%s%d";
    }

    public NameBuilder dupesPattern(String dupesPattern) {
        this.dupesPattern = Objects.requireNonNull(dupesPattern);
        return this;
    }

    public NameBuilder preferredName(String preferredName) {
        this.preferredName = preferredName;
        return this;
    }

    public String build() {
        String baseName = this.preferredName != null && !this.preferredName.isEmpty()
                ? this.preferredName
                : defaultBaseName(nodeToName);

        String normalizedBaseName = normalize(nodeToName, baseName);
        return deduplicate(nodeToName, normalizedBaseName);
    }

    private static String defaultBaseName(ConfigurationNode node) {
        return switch (node) {
            case DataChannelDescriptor ignored -> "project";
            case DataNodeDescriptor ignored -> "datanode";
            case DataMap ignored -> "datamap";
            case ObjEntity ignored -> "ObjEntity";
            case DbEntity ignored -> "db_entity";
            case Embeddable ignored -> "Embeddable";
            case EmbeddableAttribute ignored -> "untitledAttr";
            case ObjAttribute ignored -> "untitledAttr";
            case DbAttribute ignored -> "untitledAttr";
            case ObjRelationship ignored -> "untitledRel";
            case DbRelationship ignored -> "untitledRel";
            case Procedure ignored -> "procedure";
            case ProcedureParameter ignored -> "UntitledProcedureParameter";
            case QueryDescriptor ignored -> "query";
            case CallbackNode ignored -> "onEvent";
            default -> throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
        };
    }

    private static String normalize(ConfigurationNode node, String baseName) {
        return switch (node) {
            case ObjEntity ignored -> NameUtil.capitalize(baseName);
            case Embeddable ignored -> NameUtil.capitalize(baseName);
            case EmbeddableAttribute ignored -> NameUtil.uncapitalize(baseName);
            case ObjAttribute ignored -> NameUtil.uncapitalize(baseName);
            case ObjRelationship ignored -> NameUtil.uncapitalize(baseName);
            case DbRelationship ignored -> NameUtil.uncapitalize(baseName);
            default -> baseName;
        };
    }

    private String deduplicate(ConfigurationNode node, String baseName) {

        // a top-level node (e.g. a DataChannelDescriptor or a project-level DataMap) has no siblings to clash with
        if (parent == null) {
            return baseName;
        }

        Predicate<String> nameChecker = switch (node) {
            case DataNodeDescriptor ignored -> this::dataNodeExists;
            case DataMap ignored -> name -> ((DataChannelDescriptor) parent).getDataMap(name) != null;
            case ObjEntity ignored -> name -> ((DataMap) parent).getObjEntity(name) != null;
            case DbEntity ignored -> name -> ((DataMap) parent).getDbEntity(name) != null;
            case Embeddable ignored -> this::embeddableExists;
            case EmbeddableAttribute ignored -> name -> ((Embeddable) parent).getAttribute(name) != null;
            case ObjAttribute ignored -> this::objEntityPropertyExists;
            case DbAttribute ignored -> this::dbEntityPropertyExists;
            case ObjRelationship ignored -> this::objEntityPropertyExists;
            case DbRelationship ignored -> this::dbEntityPropertyExists;
            case Procedure ignored -> name -> ((DataMap) parent).getProcedure(name) != null;
            case ProcedureParameter ignored -> this::procedureParameterExists;
            case QueryDescriptor ignored -> name -> ((DataMap) parent).getQueryDescriptor(name) != null;
            case CallbackNode ignored -> this::callbackMethodExists;
            default -> throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
        };

        int c = 1;
        String name = baseName;
        while (nameChecker.test(name)) {
            name = String.format(dupesPattern, baseName, c++);
        }
        return name;
    }

    private boolean dataNodeExists(String name) {
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) parent;
        for (DataNodeDescriptor dataNodeDescriptor : dataChannelDescriptor.getNodeDescriptors()) {
            if (dataNodeDescriptor.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean embeddableExists(String name) {
        DataMap map = (DataMap) parent;
        return map.getEmbeddable(map.getNameWithDefaultPackage(name)) != null;
    }

    private boolean procedureParameterExists(String name) {
        // it doesn't matter if we create a parameter with a duplicate name. parameters are positional anyway.
        // still try to use unique names for visual consistency
        Procedure procedure = (Procedure) parent;
        for (ProcedureParameter parameter : procedure.getCallParameters()) {
            if (name.equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean dbEntityPropertyExists(String name) {
        DbEntity entity = (DbEntity) parent;
        // check if either attribute or relationship name matches...
        return entity.getAttribute(name) != null || entity.getRelationship(name) != null;
    }

    private boolean objEntityPropertyExists(String name) {
        ObjEntity entity = (ObjEntity) parent;

        // check if either attribute or relationship name matches...
        if (entity.getAttribute(name) != null || entity.getRelationship(name) != null) {
            return true;
        }

        //  check if there's a callback method that shadows attribute getter (unlikely, but still)
        String conflictingCallback = "get" + NameUtil.capitalize(name);
        return entity.getCallbackMethods().contains(conflictingCallback);
    }

    private boolean callbackMethodExists(String name) {
        ObjEntity entity = (ObjEntity) parent;

        if (entity.getCallbackMethods().contains(name)) {
            return true;
        }

        if (name.startsWith("get")) {
            String conflictingProperty = NameUtil.uncapitalize(name.substring(3));

            // check if either attribute or relationship name matches...
            return entity.getAttribute(conflictingProperty) != null
                    || entity.getRelationship(conflictingProperty) != null;
        }

        return false;
    }
}
