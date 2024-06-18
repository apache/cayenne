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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DataRowStoreFactory;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataChannelDescriptorMerger;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * A {@link DataChannel} provider that provides a single instance of DataDomain
 * configured per configuration supplied via injected
 * {@link DataChannelDescriptorLoader}.
 * 
 * @since 3.1
 */
public class DataDomainProvider implements Provider<DataDomain> {

	private static final Logger logger = LoggerFactory.getLogger(DataDomainProvider.class);

	@Inject
	protected ResourceLocator resourceLocator;

	@Inject
	protected DataChannelDescriptorMerger descriptorMerger;

	@Inject
	protected DataChannelDescriptorLoader loader;

	/**
	 * @since 4.1
	 */
	@Inject
	protected List<DataChannelQueryFilter> queryFilters;

	/**
	 * @since 4.1
	 */
	@Inject
	protected List<DataChannelSyncFilter> syncFilters;

	@Inject(Constants.DOMAIN_LISTENERS_LIST)
	protected List<Object> listeners;

	@Inject(Constants.PROJECT_LOCATIONS_LIST)
	protected List<String> locations;

	@Inject
	protected Injector injector;

	@Inject
	protected QueryCache queryCache;

	@Inject
	protected RuntimeProperties runtimeProperties;

	@Inject
	protected DataNodeFactory dataNodeFactory;

	@Override
	public DataDomain get() throws ConfigurationException {

		try {
			return createAndInitDataDomain();
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			String causeMessage = e.getMessage();
			String message = causeMessage != null && causeMessage.length() > 0 ? causeMessage : e.getClass().getName();
			throw new DataDomainLoadException("DataDomain startup failed: %s", e, message);
		}
	}

	protected DataDomain createDataDomain(String name) {
		return new DataDomain(name);
	}

	@SuppressWarnings("deprecation")
	protected DataDomain createAndInitDataDomain() throws Exception {

		DataChannelDescriptor descriptor = loadDescriptor();

		DataDomain dataDomain = createDataDomain(descriptor.getName());

		dataDomain.setMaxIdQualifierSize(runtimeProperties.getInt(Constants.MAX_ID_QUALIFIER_SIZE_PROPERTY, -1));

		dataDomain.setQueryCache(new NestedQueryCache(queryCache));
		dataDomain.setEntitySorter(injector.getInstance(EntitySorter.class));
		dataDomain.setEventManager(injector.getInstance(EventManager.class));
		dataDomain.setDataRowStoreFactory(injector.getInstance(DataRowStoreFactory.class));

		dataDomain.initWithProperties(descriptor.getProperties());

		for (DataMap dataMap : descriptor.getDataMaps()) {
			dataDomain.addDataMap(dataMap);
		}

		dataDomain.getEntityResolver().applyDBLayerDefaults();
		dataDomain.getEntityResolver().setValueObjectTypeRegistry(injector.getInstance(ValueObjectTypeRegistry.class));
		dataDomain.getEntityResolver().setValueComparisonStrategyFactory(injector.getInstance(ValueComparisonStrategyFactory.class));
		dataDomain.getEntityResolver().setObjectFactory(injector.getInstance(AdhocObjectFactory.class));

		for (DataNodeDescriptor nodeDescriptor : descriptor.getNodeDescriptors()) {
			addDataNode(dataDomain, nodeDescriptor);
		}

		// init default node
		DataNode defaultNode = null;

		if (descriptor.getDefaultNodeName() != null) {
			defaultNode = dataDomain.getDataNode(descriptor.getDefaultNodeName());
		}

		if (defaultNode == null) {
			Collection<DataNode> allNodes = dataDomain.getDataNodes();
			if (allNodes.size() == 1) {
				defaultNode = allNodes.iterator().next();
			}
		}

		if (defaultNode != null) {
			logger.info("setting DataNode '" + defaultNode.getName() + "' as default, used by all unlinked DataMaps");

			dataDomain.setDefaultNode(defaultNode);
		}

		for (DataChannelQueryFilter filter : queryFilters) {
			dataDomain.addQueryFilter(filter);
		}

		for (DataChannelSyncFilter filter : syncFilters) {
			dataDomain.addSyncFilter(filter);
		}

		for (Object listener : listeners) {
			dataDomain.addListener(listener);
		}

		return dataDomain;
	}

	/**
	 * @since 4.0
     */
	protected DataChannelDescriptor loadDescriptor() {
		DataChannelDescriptor descriptor = locations.isEmpty() ? new DataChannelDescriptor() : loadDescriptorFromConfigs();

		String nameOverride = runtimeProperties.get(Constants.DOMAIN_NAME_PROPERTY);
		if (nameOverride != null) {
			descriptor.setName(nameOverride);
		}

		return descriptor;
	}

	/**
	 * @since 4.0
	 */
	protected DataNode addDataNode(DataDomain dataDomain, DataNodeDescriptor nodeDescriptor) throws Exception {
		DataNode dataNode = dataNodeFactory.createDataNode(nodeDescriptor);

		// DataMaps
		for (String dataMapName : nodeDescriptor.getDataMapNames()) {
			dataNode.addDataMap(dataDomain.getDataMap(dataMapName));
		}

		dataDomain.addNode(dataNode);
		return dataNode;
	}

	private DataChannelDescriptor loadDescriptorFromConfigs() {

		long t0 = System.currentTimeMillis();

		if (logger.isDebugEnabled()) {
			logger.debug("starting configuration loading: " + locations);
		}

		DataChannelDescriptor[] descriptors = new DataChannelDescriptor[locations.size()];

		for (int i = 0; i < locations.size(); i++) {

			String location = locations.get(i);

			Collection<Resource> configurations = resourceLocator.findResources(location);

			if (configurations.isEmpty()) {
				throw new DataDomainLoadException("Configuration resource \"%s\" is not found.", location);
			}

			Resource configurationResource = configurations.iterator().next();

			// no support for multiple configs yet, but this is not a hard error
			if (configurations.size() > 1) {
				logger.info("found " + configurations.size() + " configurations for " + location
						+ ", will use the first one: " + configurationResource.getURL());
			}

			ConfigurationTree<DataChannelDescriptor> tree = loader.load(configurationResource);
			if (!tree.getLoadFailures().isEmpty()) {
				// TODO: andrus 03/10/2010 - log the errors before throwing?
				throw new DataDomainLoadException(tree, "Error loading DataChannelDescriptor");
			}

			descriptors[i] = tree.getRootNode();
		}

		long t1 = System.currentTimeMillis();

		if (logger.isDebugEnabled()) {
			logger.debug("finished configuration loading in " + (t1 - t0) + " ms.");
		}

		return descriptorMerger.merge(descriptors);
	}
}
