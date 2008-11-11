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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.cayenne.jpa.conf.JpaDataSourceFactory;

/**
 * A <code>javax.persistence.spi.PersistenceUnitInfo</code> implementor used by Cayenne
 * JPA provider.
 * 
 */
public abstract class JpaUnit implements PersistenceUnitInfo {

    protected String persistenceUnitName;
    protected List<String> mappingFileNames;
    protected List<URL> jarFileUrls;
    protected List<String> managedClassNames;
    protected URL persistenceUnitRootUrl;
    protected boolean excludeUnlistedClasses;
    protected Properties properties;
    protected String description;

    // properties not exposed directly
    protected ClassLoader classLoader;

    public JpaUnit() {

        this.mappingFileNames = new ArrayList<String>(2);
        this.jarFileUrls = new ArrayList<URL>(2);
        this.managedClassNames = new ArrayList<String>(30);
        this.properties = new Properties();

        setDefaultClassLoader();
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public String getPersistenceProviderClassName() {
        return getProperty(Provider.PROVIDER_PROPERTY);
    }

    /**
     * Adds a {@link ClassTransformer} to the persistence unit.
     * <h3>JPA Specification, 7.1.4:</h3>
     * Add a transformer supplied by the provider that will be called for every new class
     * definition or class redefinition that gets loaded by the loader returned by the
     * PersistenceInfo.getClassLoader method. The transformer has no effect on the result
     * returned by the PersistenceInfo.getTempClassLoader method. Classes are only
     * transformed once within the same classloading scope, regardless of how many
     * persistence units they may be a part of.
     * 
     * @param transformer A provider-supplied transformer that the Container invokes at
     *            class-(re)definition time
     */
    public abstract void addTransformer(ClassTransformer transformer);

    public PersistenceUnitTransactionType getTransactionType() {
        String type = getProperty(Provider.TRANSACTION_TYPE_PROPERTY);

        // default JTA type is somewhat arbitrary as application-managed EntityManagers
        // will use resource-local, while container-managed will use JTA. Normally whoever
        // created this unit will set the right value.
        return type != null
                ? PersistenceUnitTransactionType.valueOf(type)
                : PersistenceUnitTransactionType.JTA;
    }

    String getProperty(String key) {
        return properties.getProperty(key);
    }

    JpaDataSourceFactory getJpaDataSourceFactory() {
        String factory = getProperty(Provider.DATA_SOURCE_FACTORY_PROPERTY);

        if (factory == null) {
            throw new JpaProviderException("No value for '"
                    + Provider.DATA_SOURCE_FACTORY_PROPERTY
                    + "' property - can't build DataSource factory.");
        }

        try {
            // use app class loader - this is not the class to enhance...
            return (JpaDataSourceFactory) Class.forName(
                    factory,
                    true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        }
        catch (Throwable th) {
            throw new JpaProviderException("Error instantiating a JPADataSourceFactory: "
                    + factory, th);
        }
    }

    public DataSource getJtaDataSource() {
        String name = getProperty(Provider.JTA_DATA_SOURCE_PROPERTY);
        return getJpaDataSourceFactory().getJtaDataSource(name, this);
    }

    public DataSource getNonJtaDataSource() {
        String name = getProperty(Provider.NON_JTA_DATA_SOURCE_PROPERTY);
        return getJpaDataSourceFactory().getNonJtaDataSource(name, this);
    }

    public List<String> getMappingFileNames() {
        return mappingFileNames;
    }

    public List<URL> getJarFileUrls() {
        return jarFileUrls;
    }

    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitRootUrl;
    }

    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    /**
     * Returns whether classes not listed in the persistence.xml descriptor file should be
     * excluded from persistence unit. Should be ignored in J2SE environment.
     */
    public boolean excludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    public Properties getProperties() {
        return properties;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Creates and returns a child of the main unit ClassLoader.
     */
    public ClassLoader getNewTempClassLoader() {
        return new JpaUnitClassLoader(Thread.currentThread().getContextClassLoader());
    }

    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }

    public void addJarFileUrl(String jarName) {
        // resolve URLs relative to the unit root

        if (persistenceUnitRootUrl == null) {
            throw new IllegalStateException("Persistence Unit Root URL is not set");
        }

        try {
            this.jarFileUrls.add(new URL(persistenceUnitRootUrl, jarName));
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Jar file name:" + jarName, e);
        }
    }

    public void setPersistenceUnitName(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    /**
     * Sets new "main" ClassLoader of this unit.
     */
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            setDefaultClassLoader();
        }
        else {
            this.classLoader = classLoader;
        }
    }

    protected void setDefaultClassLoader() {
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public void addManagedClassName(String managedClassName) {
        this.managedClassNames.add(managedClassName);
    }

    public void addMappingFileName(String mappingFileName) {
        this.mappingFileNames.add(mappingFileName);
    }

    public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
        this.persistenceUnitRootUrl = persistenceUnitRootUrl;
    }

    public void addProperties(Map<?, ?> properties) {
        this.properties.putAll(properties);
    }

    public void putProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
