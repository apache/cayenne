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
package org.apache.cayenne.configuration;

import org.apache.cayenne.map.DataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default implementation of {@link ProjectMerger}. The general rule of
 * merge is that the order of descriptors on the merge list matters. If there are two
 * conflicting metadata objects belonging to two descriptors, an object from the last
 * project takes precedence over the object from the first one. This way it is easy to
 * override pieces of metadata. This is also similar to how DI modules are merged in
 * Cayenne. So this is how the merge works:
 * <ul>
 * <li>Merged project name is the same as the name of the last project on the merge
 * list.</li>
 * <li>Merged project settings are the same as the settings of the last project
 * on the merge list. I.e. settings are not merged to avoid invalid combinations and
 * unexpected runtime behavior.</li>
 * <li>If there are two or more DataMaps with the same name, only one DataMap is placed in
 * the merged project, the rest are discarded. DataMap with highest index in the
 * project array is chosen per precedence rule above.</li>
 * <li>If there are two or more DataNodes with the same name, only one DataNodes is placed
 * in the merged project, the rest are discarded. DataNodes with highest index in the
 * project array is chosen per precedence rule above.</li>
 * </ul>
 * 
 * @since 3.1
 */
public class DefaultProjectMerger implements ProjectMerger {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DefaultProjectMerger.class);

    public Project merge(Project... projects) {
        if (projects == null || projects.length == 0) {
            throw new IllegalArgumentException("Null or empty descriptors");
        }

        if (projects.length == 1) {
            return projects[0];
        }

        int len = projects.length;

        // merge into a new project; do not alter source descriptors
        Project merged = new Project();
        merged.setName(projects[len - 1].getName());
        merged.setSharedCacheEnabled(projects[len - 1].isSharedCacheEnabled());
        merged.setValidatingObjectsOnCommit(projects[len - 1].isValidatingObjectsOnCommit());

        // iterate in reverse order to reduce add/remove operations
        for (int i = len - 1; i >= 0; i--) {
            Project project = projects[i];

            // DataMaps are merged by reference, as we don't change them
            // TODO: they still have a link to the unmerged project, is it bad?
            for (DataMap map : project.getDataMaps()) {

                // report conflicting DataMap and leave the existing copy
                DataMap existing = merged.getDataMap(map.getName());
                if (existing != null) {

                    LOGGER.info("Discarding overridden DataMap '"
                            + map.getName()
                            + "' from descriptor '"
                            + project.getName()
                            + "'");
                }
                else {

                    LOGGER.info("Using DataMap '"
                            + map.getName()
                            + "' from descriptor '"
                            + project.getName()
                            + "' in merged descriptor");
                    merged.getDataMaps().add(map);
                }
            }
        }

        return merged;
    }
}
