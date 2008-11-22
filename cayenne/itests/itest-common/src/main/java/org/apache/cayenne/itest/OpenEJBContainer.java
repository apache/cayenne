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
package org.apache.cayenne.itest;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.apache.cayenne.project.CayenneUserDir;
import org.apache.cayenne.util.Util;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.openejb.client.LocalInitialContextFactory;

/**
 * A test OpenEJB container object that provides JNDI and JTA environment to the
 * integration tests.
 * 
 */
public class OpenEJBContainer {

    private static OpenEJBContainer singletonContainer;

    public static OpenEJBContainer getContainer() {
        if (singletonContainer == null) {
            singletonContainer = new OpenEJBContainer();
        }

        return singletonContainer;
    }

    private File openEjbHome;
    private GeronimoTransactionManager txManager;

    private OpenEJBContainer() {
        setupOpenEJBHome();
        setupContainerProperties();

        try {
            bootstrapContainer();
        }
        catch (Exception e) {
            throw new RuntimeException("Error bootrapping OpenEJB container", e);
        }
    }

    public TransactionSynchronizationRegistry getTxSyncRegistry() {
        return txManager;
    }

    public TransactionManager getTxManager() {
        return txManager;
    }

    public boolean isActiveTransaction() {
        int status = txManager.getTransactionStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
    }

    private void setupOpenEJBHome() {

        File currentDir = new File(System.getProperty("user.dir"));
        openEjbHome = new File(currentDir, "target" + File.separatorChar + "openejb-out");
        openEjbHome.mkdirs();

        File openEjbConf = new File(openEjbHome, "conf");
        openEjbConf.mkdirs();

        File openEjbLogs = new File(openEjbConf, "logs");
        openEjbLogs.mkdirs();

        // to see Cayenne output, copy default logging properties to openejb dir
        File logConfig = new File(openEjbConf, "logging.properties");
        File defaultLogConfig = CayenneUserDir.getInstance().resolveFile(
                "cayenne-log.properties");

        if (defaultLogConfig.exists() && !Util.copy(defaultLogConfig, logConfig)) {
            throw new RuntimeException("Can't copy logging configuration to " + logConfig);
        }
    }

    private void setupContainerProperties() {
        System.setProperty(
                Context.INITIAL_CONTEXT_FACTORY,
                LocalInitialContextFactory.class.getName());
        System.setProperty("openejb.home", openEjbHome.getAbsolutePath());
    }

    private void bootstrapContainer() throws Exception {

        // somehow OpenEJB LocalInitialContextFactory requires 2 IC's to be initilaized to
        // fully bootstrap the environment, so do one empty run here, and then use a
        // different IC for binding the environment.
        new InitialContext();

        this.txManager = new GeronimoTransactionManager();
        new InitialContext().bind(
                "java:comp/TransactionSynchronizationRegistry",
                txManager);
    }
}
