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

import org.apache.cayenne.resource.Resource;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A context for an XML document of a project or a DataMap being upgraded.
 *
 * @since 4.1
 */
public class UpgradeContext {

    private final Resource resource;
    private final Document document;
    private final List<String> changeNotifications;
    private final List<String> changesAffectingRuntime;
    private final List<String> obsoleteFiles;

    public UpgradeContext(Resource resource, Document document) {
        this.resource = resource;
        this.document = document;
        this.changeNotifications = new ArrayList<>();
        this.changesAffectingRuntime = new ArrayList<>();
        this.obsoleteFiles = new ArrayList<>();
    }

    public Document getDocument() {
        return document;
    }

    public Resource getResource() {
        return resource;
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
