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
 * @since 4.1
 */
public class UpgradeContext {

    private final Resource resource;
    private final Document document;
    private final List<String> postUpgradeMessages;

    public UpgradeContext(Resource resource, Document document) {
        this.resource = resource;
        this.document = document;
        this.postUpgradeMessages = new ArrayList<>();
    }

    public Document getDocument() {
        return document;
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Records a message shown to the user once the upgrade is done, describing manual steps required to complete
     * the transition (e.g. regenerating classes or updating application code). Intended for upgrade handlers that
     * detect a condition in this unit that they can't fix automatically.
     *
     * @since 5.0
     */
    public void addPostUpgradeMessage(String message) {
        postUpgradeMessages.add(message);
    }

    /**
     * @since 5.0
     */
    public List<String> getPostUpgradeMessages() {
        return postUpgradeMessages;
    }
}
