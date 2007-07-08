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

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

public class MockPersistenceUnitInfo implements PersistenceUnitInfo {

    public String getPersistenceUnitName() {
        return null;
    }

    public String getPersistenceProviderClassName() {
        return null;
    }

    public PersistenceUnitTransactionType getTransactionType() {
        return null;
    }

    public DataSource getJtaDataSource() {
        return null;
    }

    public DataSource getNonJtaDataSource() {
        return null;
    }

    public List<String> getMappingFileNames() {
        return null;
    }

    public List<URL> getJarFileUrls() {
        return null;
    }

    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    public List<String> getManagedClassNames() {
        return null;
    }

    public boolean excludeUnlistedClasses() {
        return false;
    }

    public Properties getProperties() {
        return null;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void addTransformer(ClassTransformer transformer) {
    }

    public ClassLoader getNewTempClassLoader() {
        return null;
    }

}
