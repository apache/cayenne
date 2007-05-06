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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * A bridge between Log4J and Ant logging system.
 * 
 * @since 1.2
 * @author Andrei Adamchik
 */
class AntAppender extends AppenderSkeleton {

    Task task;

    AntAppender(Task task) {
        if (task == null) {
            throw new NullPointerException("Null Task");
        }

        this.task = task;
    }

    /**
     * Logs Log4J message at Ant level "Project.MSG_VERBOSE" so that framework messages
     * are not displayed during the normal build.
     */
    protected void append(LoggingEvent event) {
        Object message = event.getMessage();
        if (message == null) {
            return;
        }

        // downgrade framework messages to VERBOSE
        task.log(message.toString(), Project.MSG_VERBOSE);
    }

    public void close() {

    }

    public boolean requiresLayout() {
        return false;
    }
}
