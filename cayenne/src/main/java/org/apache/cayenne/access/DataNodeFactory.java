/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.DataNodeDescriptor;

import java.util.Collection;

/**
 * A factory for creating DataNodes from descriptors.
 * 
 * @since 4.0
 */
public interface DataNodeFactory {

    /**
     * Creates a node from its descriptor. The DataMaps are the ones the node is going to serve; the node itself has
     * no use for them, but its initialization, such as a schema update, may.
     *
     * @since 6.0
     */
    DataNode createDataNode(String dataChannelName, DataNodeDescriptor nodeDescriptor, Collection<DataMap> dataMaps);
}
