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
import org.apache.cayenne.map.ObjEntity;

import java.util.Objects;

/**
 * A
 *
 * @since 4.0
 */
public class NameBuilder {

    protected ConfigurationNode nodeToName;
    protected ConfigurationNode namingContext;
    protected String dupesPattern;
    protected String baseName;

    protected NameBuilder(ConfigurationNode nodeToName) {
        this.nodeToName = Objects.requireNonNull(nodeToName);
        this.dupesPattern = "%s%d";
    }

    public static NameBuilder builder(ConfigurationNode node) {
        return new NameBuilder(node);
    }

    public static NameBuilder builder(ConfigurationNode node, ConfigurationNode namingContext) {
        return new NameBuilder(node).in(namingContext);
    }

    /**
     * A special builder starter for callback methods. Eventually callback methods will be made into ConfigurationNodes,
     * and we can use regular {@link #builder(ConfigurationNode)} methods to name them.
     */
    // TODO: fold CallbackMethod to org.apache.cayenne.map package and make it a ConfigurationNode
    // then we can use normal API for it... for now have to keep a special one-off method...
    public static NameBuilder builderForCallbackMethod(ObjEntity namingContext) {
        return new CallbackNameBuilder().in(namingContext);
    }

    public NameBuilder in(ConfigurationNode namingContext) {
        this.namingContext = Objects.requireNonNull(namingContext);
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
        String baseName = this.baseName != null && this.baseName.length() > 0
                ? this.baseName
                : nodeToName.acceptVisitor(DefaultBaseNameVisitor.INSTANCE);

        String normalizedBaseName = nodeToName.acceptVisitor(new NormalizationVisitor(baseName));
        return nodeToName.acceptVisitor(new DeduplicationVisitor(namingContext, normalizedBaseName, dupesPattern));
    }
}
