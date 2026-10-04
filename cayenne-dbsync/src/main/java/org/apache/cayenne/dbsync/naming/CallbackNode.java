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

import org.apache.cayenne.configuration.ProjectNode;
import org.apache.cayenne.configuration.ProjectNodeVisitor;

/**
 * A pseudo-node representing an {@link org.apache.cayenne.map.ObjEntity} callback method. Callback methods are not
 * {@link ProjectNode}s in the model, so this stand-in lets them flow through the {@link NameBuilder} naming
 * algorithm like any other node.
 *
 * @since 5.0
 */
public class CallbackNode implements ProjectNode {

    @Override
    public <T> T acceptVisitor(ProjectNodeVisitor<T> visitor) {
        return null;
    }
}
