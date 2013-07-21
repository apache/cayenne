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
package org.apache.cayenne.configuration;

import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.validation.ValidationFailure;

/**
 * A tree of configuration nodes that contains extra information about the nodes, such
 * as load errors.
 * 
 * @since 3.1
 */
public class ConfigurationTree<T extends ConfigurationNode> {

    protected T rootNode;
    protected Collection<ValidationFailure> loadFailures;

    public ConfigurationTree(T rootNode) {
        this.rootNode = rootNode;
    }

    public ConfigurationTree(T rootNode, Collection<ValidationFailure> loadFailures) {
        this.rootNode = rootNode;
        this.loadFailures = loadFailures;
    }

    public T getRootNode() {
        return rootNode;
    }

    public Collection<ValidationFailure> getLoadFailures() {
        return loadFailures != null ? loadFailures : Collections.EMPTY_LIST;
    }
}
