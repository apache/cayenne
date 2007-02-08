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

package org.apache.cayenne.tools;

import org.apache.log4j.BasicConfigurator;
import org.apache.tools.ant.Task;
import org.apache.cayenne.conf.Configuration;

/**
 * A superclass of Cayenne Ant tasks. Performs some common setup
 * 
 * @author Andrei Adamchik
 * @since 1.2
 */
public abstract class CayenneTask extends Task {

    /**
     * Sets up logging to be in line with the Ant logging system. It should be called by
     * subclasses from the "execute" method.
     */
    protected void configureLogging() {
        Configuration.setLoggingConfigured(true);

        // reset is needed since when multiple Cayenne tasks are loaded via Antlib each
        // one adds its own appender..

        // TODO: this is a really a bad solution ... each task would have to reset shared
        // resource whenever its execution starts...
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new AntAppender(this));
    }
}
