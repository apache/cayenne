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

package org.apache.cayenne.project.upgrade;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A context for an XML document of a project or a DataMap being upgraded.
 *
 * @since 4.1
 */
public class UpgradeContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeContext.class);

    private final Resource source;
    private final Document document;
    private final List<String> changeNotifications;
    private final List<String> changesAffectingRuntime;
    private final List<String> obsoleteFiles;

    public UpgradeContext(Resource source, Document document) {
        this.source = source;
        this.document = document;
        this.changeNotifications = new ArrayList<>();
        this.changesAffectingRuntime = new ArrayList<>();
        this.obsoleteFiles = new ArrayList<>();
    }

    public Document getDocument() {
        return document;
    }

    /**
     * Returns the upgraded document if the upgrade was lossless, i.e. made no changes that may affect runtime. Such a
     * document can be loaded as is, without saving it or involving the user.
     *
     * @throws ConfigurationException if the upgrade was not lossless, and so requires the project to be upgraded in
     *                                CayenneModeler
     * @since 5.0
     */
    public Document getDocumentIfLossless() {
        if (!changesAffectingRuntime.isEmpty()) {
            throw new ConfigurationException("""
                    Unable to upgrade configuration from %s in memory, as the upgrade requires manual changes. \
                    Open the project in CayenneModeler to upgrade it. %s""",
                    source.getURL(), String.join(" ", changesAffectingRuntime));
        }

        LOGGER.warn("""
                Configuration {} was created with an older version of Cayenne and was upgraded to project \
                version {} in memory. Open the project in CayenneModeler to upgrade its XML permanently""",
                source.getURL(), UpgradeHandler.CURRENT_VERSION);

        return document;
    }

    /**
     * @since 5.0
     */
    public Resource getSource() {
        return source;
    }

    /**
     * Records a message about manual steps that the user needs to take after the upgrade.
     *
     * @since 5.0
     */
    public void recordChange(String message, boolean mayAffectRuntime) {
        changeNotifications.add(message);
        if (mayAffectRuntime) {
            changesAffectingRuntime.add(message);
        }
    }

    /**
     * @since 5.0
     */
    public List<String> getChangeNotifications() {
        return changeNotifications;
    }

    /**
     * @since 5.0
     */
    public List<String> getChangesAffectingRuntime() {
        return changesAffectingRuntime;
    }

    /**
     * Records a file that the upgraded document no longer references, as a path relative to the document's own
     * location. A caller that saves the upgraded document may delete it.
     *
     * @since 5.0
     */
    public void addObsoleteFile(String relativePath) {
        obsoleteFiles.add(relativePath);
    }

    /**
     * @since 5.0
     */
    public List<String> getObsoleteFiles() {
        return obsoleteFiles;
    }
}
