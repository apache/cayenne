/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.jpa.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.persistence.PersistenceException;

import org.apache.cayenne.jpa.example.entity.Department;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.map.DataMap;

/**
 * A helper class that creates a new test Derby database. Uses Cayenne API to bootstrap
 * the DB, but the schema check query is actaully mapped with annotations.
 * 
 * @author Andrus Adamchik
 */
class DerbySetupHelper {

    static final String DERBY_SYSTEM_PROPERTY = "derby.system.home";

    private DataContext context;

    void setupDatabase() {
        if (context == null) {
            context = DataContext.createDataContext();
        }
        if (checkDBSetupNeeded()) {
            setupDemoSchema();
        }
    }

    void prepareDerby() throws PersistenceException {
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("derby.properties");

        if (in != null) {
            Properties props = new Properties();

            try {
                props.load(in);
            }
            catch (IOException e) {
                throw new PersistenceException("Error reading properties", e);
            }

            System.getProperties().putAll(props);
        }

        // setup Derby home to be Java TMP directory if not set explicitly

        if (System.getProperty(DERBY_SYSTEM_PROPERTY) == null) {
            System.setProperty(DERBY_SYSTEM_PROPERTY, System
                    .getProperty("java.io.tmpdir"));
        }

    }

    /**
     * Runs a test query to see if a schema is initialized.
     */
    private boolean checkDBSetupNeeded() {
        try {
            context.performNonSelectingQuery("SchemaCheck");
            return false;
        }
        catch (Throwable th) {
            return true;
        }
    }

    private void setupDemoSchema() {
        DataMap map = context
                .getEntityResolver()
                .lookupObjEntity(Department.class)
                .getDataMap();
        DataNode node = context.getParentDataDomain().lookupDataNode(map);
        DbGenerator generator = new DbGenerator(node.getAdapter(), map);
        try {
            generator.runGenerator(node.getDataSource());
        }
        catch (Exception e) {
            throw new PersistenceException("Error generating DB schema", e);
        }
    }
}
