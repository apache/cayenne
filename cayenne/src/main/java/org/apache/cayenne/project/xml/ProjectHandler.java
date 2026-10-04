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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public final class ProjectHandler extends VersionAwareHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLProjectLoader.class);

    static final String PROJECT_TAG = "project";

    private final ConfigurationNameMapper nameMapper;
    private final DataMapLoader dataMapLoader;
    Project project;

    public ProjectHandler(Project project, LoaderContext loaderContext, ConfigurationNameMapper nameMapper,
                          DataMapLoader dataMapLoader) {
        super(loaderContext, PROJECT_TAG);
        this.nameMapper = nameMapper;
        this.dataMapLoader = dataMapLoader;
        this.project = project;
        setTargetNamespace(Project.SCHEMA_XSD);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        // the name of the root tag before version 14
        if ("domain".equals(localName)) {
            validateVersion(namespaceURI, attributes);
        }

        // both settings are true by default
        if (PROJECT_TAG.equals(localName)) {
            project.setSharedCacheEnabled(!"false".equals(attributes.getValue("sharedCache")));
            project.setValidatingObjectsOnCommit(!"false".equals(attributes.getValue("validateOnCommit")));
        }

        return super.processElement(namespaceURI, localName, attributes);
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {

        if (localName.equals(PROJECT_TAG)) {
            return new ProjectChildrenHandler(this, nameMapper, dataMapLoader);
        }

        LOGGER.info(unexpectedTagMessage(localName, PROJECT_TAG));
        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }
}
