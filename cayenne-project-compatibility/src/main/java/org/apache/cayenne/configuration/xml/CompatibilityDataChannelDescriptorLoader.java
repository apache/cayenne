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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.compatibility.CompatibilityUpgradeService;
import org.apache.cayenne.project.compatibility.DocumentProvider;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeService;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


/**
 * @since 4.1
 */
public class CompatibilityDataChannelDescriptorLoader extends XMLDataChannelDescriptorLoader {

    private static final Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    @Inject
    Provider<UpgradeService> upgradeServiceProvider;

    @Inject
    DocumentProvider documentProvider;

    @Override
    public ConfigurationTree<DataChannelDescriptor> load(Resource configurationResource) throws ConfigurationException {
        if (configurationResource == null) {
            throw new NullPointerException("Null configurationResource");
        }

        if(!(upgradeServiceProvider.get() instanceof CompatibilityUpgradeService)) {
            throw new ConfigurationException("CompatibilityUpgradeService expected");
        }

        CompatibilityUpgradeService upgradeService = (CompatibilityUpgradeService)upgradeServiceProvider.get();

        UpgradeMetaData metaData = upgradeService.getUpgradeType(configurationResource);
        if(metaData.getUpgradeType() == UpgradeType.UPGRADE_NOT_NEEDED) {
            return super.load(configurationResource);
        }

        if(metaData.getUpgradeType() == UpgradeType.DOWNGRADE_NEEDED) {
            throw new ConfigurationException("Unable to load configuration from %s: " +
                    "It was created using a newer version of the Modeler", configurationResource.getURL());
        }

        if(metaData.getUpgradeType() == UpgradeType.INTERMEDIATE_UPGRADE_NEEDED) {
            throw new ConfigurationException("Unable to load configuration from %s: " +
                    "Open the project in the older Modeler to do an intermediate upgrade.", configurationResource.getURL());
        }

        URL configurationURL = configurationResource.getURL();

        upgradeService.upgradeProject(configurationResource);
        Document projectDocument = documentProvider.getDocument(configurationURL);
        if(projectDocument == null) {
            throw new ConfigurationException("Unable to upgrade " + configurationURL);
        }

        logger.info("Loading XML configuration resource from " + configurationURL);

        final DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setConfigurationSource(configurationResource);
        descriptor.setName(nameMapper.configurationNodeName(DataChannelDescriptor.class, configurationResource));

        try {
            DOMSource source = new DOMSource(projectDocument);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transFactory.newTransformer().transform(source, new StreamResult(baos));
            InputSource isource = new InputSource(source.getSystemId());
            isource.setByteStream(new ByteArrayInputStream(baos.toByteArray()));

            XMLReader parser = Util.createXmlReader();
            LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
            loaderContext.addDataMapListener(new DataMapLoaderListener() {
                @Override
                public void onDataMapLoaded(DataMap dataMap) {
                    descriptor.getDataMaps().add(dataMap);
                }
            });

            DataChannelHandler rootHandler = new DataChannelHandler(this, descriptor, loaderContext);
            parser.setContentHandler(rootHandler);
            parser.setErrorHandler(rootHandler);
            parser.parse(isource);
        } catch (Exception e) {
            throw new ConfigurationException("Error loading configuration from %s", e, configurationURL);
        }

        // Finally upgrade model, if needed
        upgradeService.upgradeModel(configurationResource, descriptor);

        return new ConfigurationTree<>(descriptor, null);
    }
}
