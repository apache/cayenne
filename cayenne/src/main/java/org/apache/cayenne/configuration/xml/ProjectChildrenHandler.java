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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.configuration.Project;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
final class ProjectChildrenHandler extends NamespaceAwareNestedTagHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLProjectLoader.class);

    static final String OLD_MAP_TAG = "map";
    static final String DATA_MAP_TAG = "dataMap";
    static final String PROJECT_TAG = "project";


    private XMLProjectLoader xmlProjectLoader;
    private Project project;

    ProjectChildrenHandler(XMLProjectLoader xmlProjectLoader, ProjectHandler parentHandler) {
        super(parentHandler);
        this.xmlProjectLoader = xmlProjectLoader;
        this.project = parentHandler.project;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case OLD_MAP_TAG:
                addMap(attributes);
                return true;

            case PROJECT_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {
        if (DATA_MAP_TAG.equals(localName)) {
            return new DataMapHandler(loaderContext);
        }

        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }

    private void addMap(Attributes attributes) {
        String dataMapName = attributes.getValue("name");
        Resource baseResource = project.getConfigurationSource();

        String dataMapLocation = xmlProjectLoader.nameMapper.configurationLocation(DataMap.class, dataMapName);

        Resource dataMapResource = baseResource.getRelativeResource(dataMapLocation);

        LOGGER.info("Loading XML DataMap resource from {}", dataMapResource.getURL());

        DataMap dataMap = xmlProjectLoader.dataMapLoader.load(dataMapResource);
        dataMap.setName(dataMapName);
        dataMap.setLocation(dataMapLocation);
        dataMap.setProject(project);

        project.getDataMaps().add(dataMap);
    }
}
