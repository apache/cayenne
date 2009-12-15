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
package org.apache.cayenne.project2;

import org.apache.cayenne.configuration.ConfigurationNode;

/**
 * A model of a Cayenne mapping project. A project consists of descriptors for
 * DataChannel, DataNodes and DataMaps and associated filesystem files they are loaded
 * from and saved to.
 * 
 * @since 3.1
 */
// do we even need a project wrapper around ConfigurationNode, as currently it does
// nothing?? Maybe in the future make it store configuration Resources for the project
// nodes to avoid attaching them to descriptors?
public class Project {

    protected ConfigurationNode rootNode;

    public Project(ConfigurationNode rootNode) {
        this.rootNode = rootNode;
    }

    public ConfigurationNode getRootNode() {
        return rootNode;
    }
}
