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
package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.resource.Resource;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

/**
 * @since 3.1
 * @since 4.1 moved from org.apache.cayenne.configuration package
 */
public class XMLDataChannelDescriptorLoader implements DataChannelDescriptorLoader {

	private static final Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

	/**
	 * Versions of project XML files that this loader can read.
	 * See DefaultUpgradeService.MIN/MAX_SUPPORTED_VERSION in cayenne-project.
	 */
	static final String[] SUPPORTED_PROJECT_VERSIONS = {"10", "11"};

	public static final String DEFAULT_PROJECT_VERSION = "10";

	static {
		Arrays.sort(SUPPORTED_PROJECT_VERSIONS);
	}

	/**
	 * @deprecated the caller should use password resolving strategy instead of
	 *             resolving the password on the spot. For one thing this can be
	 *             used in the Modeler and no password may be available.
	 */
	@Deprecated
	static String passwordFromURL(URL url) {
		InputStream inputStream;
		String password = null;

		try {
			inputStream = url.openStream();
			password = passwordFromInputStream(inputStream);
		} catch (IOException exception) {
			// Log the error while trying to open the stream. A null
			// password will be returned as a result.
			logger.warn(exception.getMessage(), exception);
		}

		return password;
	}

	/**
	 * @deprecated the caller should use password resolving strategy instead of
	 *             resolving the password on the spot. For one thing this can be
	 *             used in the Modeler and no password may be available.
	 */
	@Deprecated
	static String passwordFromInputStream(InputStream inputStream) {
		String password = null;

		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));) {

			password = bufferedReader.readLine();
		} catch (IOException exception) {
			logger.warn(exception.getMessage(), exception);
		} finally {

			try {
				inputStream.close();
			} catch (IOException ignored) {
			}
		}

		return password;
	}

	@Inject
	protected Provider<XMLReader> xmlReaderProvider;

	@Inject
	protected DataMapLoader dataMapLoader;

	@Inject
	protected ConfigurationNameMapper nameMapper;

	@Inject
	protected AdhocObjectFactory objectFactory;

	@Inject
	protected HandlerFactory handlerFactory;

	@Override
	public ConfigurationTree<DataChannelDescriptor> load(Resource configurationResource) throws ConfigurationException {

		if (configurationResource == null) {
			throw new NullPointerException("Null configurationResource");
		}

		URL configurationURL = configurationResource.getURL();

		logger.info("Loading XML configuration resource from " + configurationURL);

		final DataChannelDescriptor descriptor = new DataChannelDescriptor();
		descriptor.setConfigurationSource(configurationResource);
		descriptor.setName(nameMapper.configurationNodeName(DataChannelDescriptor.class, configurationResource));

		try(InputStream in = configurationURL.openStream()) {
			XMLReader parser = xmlReaderProvider.get();
			LoaderContext loaderContext = new LoaderContext(parser, handlerFactory);
			loaderContext.addDataMapListener(dataMap -> descriptor.getDataMaps().add(dataMap));

			DataChannelHandler rootHandler = new DataChannelHandler(this, descriptor, loaderContext);
			parser.setContentHandler(rootHandler);
			parser.setErrorHandler(rootHandler);
			InputSource input = new InputSource(in);
			input.setSystemId(configurationURL.toString());
			parser.parse(input);

            loaderContext.dataChannelLoaded(descriptor);
		} catch (Exception e) {
			throw new ConfigurationException("Error loading configuration from %s", e, configurationURL);
		}

		// TODO: andrus 03/10/2010 - actually provide load failures here...
		return new ConfigurationTree<>(descriptor, null);
	}

}
