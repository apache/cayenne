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

package org.apache.cayenne.jpa;

import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.cayenne.instrument.InstrumentUtil;
import org.apache.cayenne.jpa.conf.UnitLoader;
import org.apache.cayenne.jpa.instrument.InstrumentingUnitFactory;

/**
 * <code>PersistenceProvider</code> implementation that doesn't provide its own ORM
 * stack. Useful as a base implementation of concrete providers.
 * 
 * @author Andrus Adamchik
 */
public abstract class JpaPersistenceProvider implements PersistenceProvider {

    // common properties
    public static final String PROVIDER_PROPERTY = "javax.persistence.provider";
    public static final String TRANSACTION_TYPE_PROPERTY = "javax.persistence.transactionType";
    public static final String JTA_DATA_SOURCE_PROPERTY = "javax.persistence.jtaDataSource";
    public static final String NON_JTA_DATA_SOURCE_PROPERTY = "javax.persistence.nonJtaDataSource";

    // provider-specific properties
    public static final String DATA_SOURCE_FACTORY_PROPERTY = "org.apache.cayenne.jpa.jpaDataSourceFactory";
    public static final String UNIT_FACTORY_PROPERTY = "org.apache.cayenne.jpa.jpaUnitFactory";

    public static final String INSTRUMENTING_FACTORY_CLASS = InstrumentingUnitFactory.class
            .getName();

    protected boolean validateDescriptors;
    protected UnitLoader unitLoader;
    protected Properties defaultProperties;

    /**
     * Creates a new JpaPersistenceProvider configuring it with default appropriate for
     * running in conatiner.
     */
    public JpaPersistenceProvider() {
        this(false);
    }

    /**
     * A constructor that allows to specify whether descriptors should be validated
     * against schema.
     */
    public JpaPersistenceProvider(boolean validateDescriptors) {
        this.validateDescriptors = validateDescriptors;
        this.defaultProperties = new Properties();

        configureEnvironmentProperties();
        configureDefaultProperties();
    }

    /**
     * Loads default properties from the Java environment.
     */
    protected void configureEnvironmentProperties() {
        String dsFactory = System.getProperty(DATA_SOURCE_FACTORY_PROPERTY);
        if (dsFactory != null) {
            defaultProperties.put(DATA_SOURCE_FACTORY_PROPERTY, dsFactory);
        }

        String transactionType = System.getProperty(TRANSACTION_TYPE_PROPERTY);
        if (transactionType != null) {
            defaultProperties.put(TRANSACTION_TYPE_PROPERTY, transactionType);
        }

        String unitFactory = System.getProperty(UNIT_FACTORY_PROPERTY);
        if (unitFactory == null && InstrumentUtil.isAgentLoaded()) {
            unitFactory = INSTRUMENTING_FACTORY_CLASS;
        }

        if (unitFactory != null) {
            defaultProperties.put(UNIT_FACTORY_PROPERTY, unitFactory);
        }
    }

  

    /**
     * Configures default properties.
     */
    protected void configureDefaultProperties() {
        if (!defaultProperties.containsKey(DATA_SOURCE_FACTORY_PROPERTY)) {
            defaultProperties.put(
                    DATA_SOURCE_FACTORY_PROPERTY,
                    DefaultDataSourceFactory.class.getName());
        }

        if (!defaultProperties.containsKey(TRANSACTION_TYPE_PROPERTY)) {
            defaultProperties.put(
                    TRANSACTION_TYPE_PROPERTY,
                    PersistenceUnitTransactionType.JTA.name());
        }
    }

    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {

        // TODO: Andrus, 2/11/2006 - cache loaded units (or factories)...

        JpaUnit ui = getUnitLoader().loadUnit(emName);

        if (ui == null) {
            return null;
        }

        // override properties
        if (map != null) {
            ui.addProperties(map);
        }

        // set default properties if they are not set explicitly
        Properties properties = ui.getProperties();
        for (Map.Entry property : defaultProperties.entrySet()) {
            if (!properties.containsKey(property.getKey())) {
                properties.put(property.getKey(), property.getValue());
            }
        }

        // check if we are allowed to handle this unit (JPA Spec, 7.2)
        String provider = ui.getPersistenceProviderClassName();
        if (provider != null && !provider.equals(this.getClass().getName())) {
            return null;
        }

        return createContainerEntityManagerFactory(ui, map);
    }

    /**
     * Returns unit loader, lazily creating it on first invocation.
     */
    protected UnitLoader getUnitLoader() {
        if (unitLoader == null) {

            JpaUnitFactory factory = null;

            String unitFactoryName = getDefaultProperty(UNIT_FACTORY_PROPERTY);
            if (unitFactoryName != null) {

                try {
                    Class factoryClass = Class.forName(unitFactoryName, true, Thread
                            .currentThread()
                            .getContextClassLoader());

                    factory = (JpaUnitFactory) factoryClass.newInstance();
                }
                catch (Exception e) {
                    throw new JpaProviderException("Error loading unit infor factory '"
                            + unitFactoryName
                            + "'", e);
                }
            }

            this.unitLoader = new UnitLoader(factory, validateDescriptors);
        }

        return unitLoader;
    }

    /**
     * @param info metadata for use by PersistenceProvider.
     * @param map a map of integration=level properties for use by the persistence
     *            provider. May be null if no properties are specified.
     */
    public abstract EntityManagerFactory createContainerEntityManagerFactory(
            PersistenceUnitInfo info,
            Map map);

    public String getDefaultProperty(String key) {
        return defaultProperties.getProperty(key);
    }
}
