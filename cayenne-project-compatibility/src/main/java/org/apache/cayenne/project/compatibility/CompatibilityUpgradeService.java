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

package org.apache.cayenne.project.compatibility;

import java.util.List;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.upgrade.DefaultUpgradeService;
import org.apache.cayenne.project.upgrade.UpgradeUnit;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.w3c.dom.Document;

/**
 * @since 4.1
 */
public class CompatibilityUpgradeService extends DefaultUpgradeService {

    @Inject
    DocumentProvider documentProvider;

    public CompatibilityUpgradeService(@Inject List<UpgradeHandler> handlerList) {
        super(handlerList);
    }

    @Override
    public Resource upgradeProject(Resource resource) {
        List<UpgradeHandler> handlerList = getHandlersForVersion(loadProjectVersion(resource));
        List<UpgradeUnit> upgradeUnits = upgradeDOM(resource, handlerList);

        for(UpgradeUnit unit : upgradeUnits) {
            documentProvider.putDocument(unit.getResource().getURL(), unit.getDocument());
        }

        return resource;
    }

    public Resource upgradeDataMap(Resource resource) {
        List<UpgradeHandler> handlerList = getHandlersForVersion(loadProjectVersion(resource));
        Document document =  Util.readDocument(resource.getURL());
        UpgradeUnit upgradeUnit = new UpgradeUnit(resource, document);
        for(UpgradeHandler handler : handlerList) {
            handler.processDataMapDom(upgradeUnit);
        }
        documentProvider.putDocument(upgradeUnit.getResource().getURL(), upgradeUnit.getDocument());
        return upgradeUnit.getResource();
    }

    public void upgradeModel(Resource resource, DataChannelDescriptor descriptor) {
        List<UpgradeHandler> handlerList = getHandlersForVersion(loadProjectVersion(resource));
        for(UpgradeHandler handler : handlerList) {
            handler.processModel(descriptor);
        }
    }

}
