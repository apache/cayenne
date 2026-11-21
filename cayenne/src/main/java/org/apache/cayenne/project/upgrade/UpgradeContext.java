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
    private final List<UpgradeChange> changes;
    private final List<String> obsoleteFiles;

    public UpgradeContext(Resource source, Document document) {
        this.source = source;
        this.document = document;
        this.changes = new ArrayList<>();
        this.obsoleteFiles = new ArrayList<>();
    }

    public Document getDocument() {
        return document;
    }

    /**
     * Returns the upgraded document if the upgrade was lossless, i.e. made no changes that may affect runtime. Such a
     * document can be loaded as is, without saving it or involving the user.
     *
     * @throws ConfigurationException if the upgrade was not lossless, and so requires the project files to be
     *                                upgraded, with the user completing the manual steps
     * @since 5.0
     */
    public Document getDocumentIfLossless() {
        StringBuilder changesText = new StringBuilder();
        for (UpgradeChange change : changes) {
            if (change.affectsRuntime()) {
                changesText.append("\n\n").append(change.message());
                change.details().forEach(d -> changesText.append("\n  ").append(d));
            }
        }

        if (!changesText.isEmpty()) {
            throw new ConfigurationException("""
                    Unable to upgrade configuration from %s in memory, as the upgrade requires manual changes. \
                    Upgrade the project files with CayenneModeler or the Cayenne MCP server.%s""",
                    source.getURL(), changesText);
        }

        LOGGER.warn("""
                Configuration {} was created with an older version of Cayenne and was upgraded to project \
                version {} in memory. Upgrade the project files with CayenneModeler or the Cayenne MCP server \
                to make it permanent""",
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
     * Records a change that the user needs to know about. A change that may affect the runtime must be described
     * fully, as the upgraded project no longer has the information needed to recreate the removed or altered part in
     * code: the message says what happened and what to do, and the details list the facts, such as the settings of a
     * removed DataNode, one per line, as "label: value". Never put secrets (e.g. passwords) in the details.
     *
     * @since 6.0
     */
    public void recordChange(String message, List<String> details, boolean mayAffectRuntime) {
        changes.add(new UpgradeChange(message, details, mayAffectRuntime));
    }

    /**
     * @since 6.0
     */
    public List<UpgradeChange> getChanges() {
        return changes;
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
