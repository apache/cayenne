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

import org.apache.cayenne.map.DataMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A default implementation of {@link DataChannelDescriptorMerger}. The general rule of
 * merge is that the order of descriptors on the merge list matters. If there are two
 * conflicting metadata objects belonging to two descriptors, an object from the last
 * descriptor takes precedence over the object from the first one. This way it is easy to
 * override pieces of metadata. This is also similar to how DI modules are merged in
 * Cayenne. So this is how the merge works:
 * <ul>
 * <li>Merged descriptor name is the same as the name of the last descriptor on the merge
 * list.</li>
 * <li>Merged descriptor properties are the same as the properties of the last descriptor
 * on the merge list. I.e. properties are not merged to avoid invalid combinations and
 * unexpected runtime behavior.</li>
 * <li>If there are two or more DataMaps with the same name, only one DataMap is placed in
 * the merged descriptor, the rest are discarded. DataMap with highest index in the
 * descriptor array is chosen per precedence rule above.</li>
 * <li>If there are two or more DataNodes with the same name, only one DataNodes is placed
 * in the merged descriptor, the rest are discarded. DataNodes with highest index in the
 * descriptor array is chosen per precedence rule above.</li>
 * </ul>
 * 
 * @since 3.1
 */
public class DefaultDataChannelDescriptorMerger implements DataChannelDescriptorMerger {

    private static Log logger = LogFactory
            .getLog(DefaultDataChannelDescriptorMerger.class);

    public DataChannelDescriptor merge(DataChannelDescriptor... descriptors) {
        if (descriptors == null || descriptors.length == 0) {
            throw new IllegalArgumentException("Null or empty descriptors");
        }

        if (descriptors.length == 1) {
            return descriptors[0];
        }

        int len = descriptors.length;

        // merge into a new descriptor; do not alter source descriptors
        DataChannelDescriptor merged = new DataChannelDescriptor();
        merged.setName(descriptors[len - 1].getName());
        merged.getProperties().putAll(descriptors[len - 1].getProperties());

        // iterate in reverse order to reduce add/remove operations
        for (int i = len - 1; i >= 0; i--) {
            DataChannelDescriptor descriptor = descriptors[i];

            // DataMaps are merged by reference, as we don't change them
            // TODO: they still have a link to the unmerged descriptor, is it bad?
            for (DataMap map : descriptor.getDataMaps()) {

                // report conflicting DataMap and leave the existing copy
                DataMap existing = merged.getDataMap(map.getName());
                if (existing != null) {

                    logger.info("Discarding overridden DataMap '"
                            + map.getName()
                            + "' from descriptor '"
                            + descriptor.getName()
                            + "'");
                }
                else {

                    logger.info("Using DataMap '"
                            + map.getName()
                            + "' from descriptor '"
                            + descriptor.getName()
                            + "' in merged descriptor");
                    merged.getDataMaps().add(map);
                }
            }

            // DataNodes are merged by copy as we may modify them (changing map linking)
            for (DataNodeDescriptor node : descriptor.getNodeDescriptors()) {

                DataNodeDescriptor existing = merged.getNodeDescriptor(node.getName());
                if (existing != null) {
                    logger.info("Discarding overridden DataNode '"
                            + node.getName()
                            + "' from descriptor '"
                            + descriptor.getName()
                            + "'");

                    for (String mapName : node.getDataMapNames()) {
                        if (!existing.getDataMapNames().contains(mapName)) {
                            existing.getDataMapNames().add(mapName);
                        }
                    }
                }
                else {
                    logger.info("Using DataNode '"
                            + node.getName()
                            + "' from descriptor '"
                            + descriptor.getName()
                            + "' in merged descriptor");
                    merged
                            .getNodeDescriptors()
                            .add(cloneDataNodeDescriptor(node, merged));
                }
            }
        }

        return merged;
    }

    protected DataNodeDescriptor cloneDataNodeDescriptor(
            DataNodeDescriptor original,
            DataChannelDescriptor targetOwner) {
        DataNodeDescriptor clone = new DataNodeDescriptor(original.getName());

        // do not clone 'configurationSource' as we may change the structure of the node

        clone.setAdapterType(original.getAdapterType());
        clone.setDataChannelDescriptor(targetOwner);
        clone.setDataSourceDescriptor(original.getDataSourceDescriptor());
        clone.setDataSourceFactoryType(original.getDataSourceFactoryType());
        clone.setParameters(original.getParameters());
        clone.setSchemaUpdateStrategyType(original.getSchemaUpdateStrategyType());

        clone.getDataMapNames().addAll(original.getDataMapNames());

        return clone;
    }
}
