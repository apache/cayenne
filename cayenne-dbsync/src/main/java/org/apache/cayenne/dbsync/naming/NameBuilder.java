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
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.map.ObjEntity;

import java.util.Objects;

/**
 * A builder of names for model objects. Ensures that newly generated names do not conflict with the names of siblings
 * under the same parent node. Name generation can be performed based on default base names for each model object type,
 * or with a user-provided base name.
 *
 * @since 4.0
 */
public class NameBuilder {

    public static NameBuilder of(ConfigurationNode node) {
        return new NameBuilder(node);
    }

    // TODO: make callback a ConfigurationNode (or remove it from the model all together) to avoid a special case here
    public static NameBuilder ofCallbackMethod() {
        return new CallbackNameBuilder();
    }

    private final ConfigurationNode nodeToName;
    protected ConfigurationNode parent;
    protected String dupesPattern;
    protected String baseName;

    private NameBuilder(ConfigurationNode nodeToName) {
        this.nodeToName = Objects.requireNonNull(nodeToName);
        this.dupesPattern = "%s%d";
    }

    public NameBuilder parent(ConfigurationNode parent) {
        this.parent = Objects.requireNonNull(parent);
        return this;
    }

    public NameBuilder dupesPattern(String dupesPattern) {
        this.dupesPattern = Objects.requireNonNull(dupesPattern);
        return this;
    }

    public NameBuilder baseName(String baseName) {
        this.baseName = baseName;
        return this;
    }

    public String name() {
        String baseName = this.baseName != null && !this.baseName.isEmpty()
                ? this.baseName
                : nodeToName.acceptVisitor(DefaultBaseNameVisitor.INSTANCE);

        String normalizedBaseName = nodeToName.acceptVisitor(new NormalizationVisitor(baseName));
        return nodeToName.acceptVisitor(new DeduplicationVisitor(parent, normalizedBaseName, dupesPattern));
    }

    static class CallbackNameBuilder extends NameBuilder {

        public CallbackNameBuilder() {
            super(new CallbackNode());
        }

        @Override
        public String name() {
            String baseName = this.baseName != null
                    ? this.baseName
                    : "onEvent";

            return new DeduplicationVisitor(parent, baseName, dupesPattern).resolve(name -> {

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
            });
        }
    }

    static class CallbackNode implements ConfigurationNode {

        @Override
        public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
            return null;
        }
    }
}
