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

import org.apache.commons.logging.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @since 3.0
 */
class AntLogger implements Log {

    private Task parentTask;

    public AntLogger(Task parentTask) {
        this.parentTask = parentTask;
    }

    public void debug(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_DEBUG);
    }

    public void debug(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_DEBUG);
    }

    public void error(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_ERR);
    }

    public void error(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_ERR);
    }

    public void fatal(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_ERR);
    }

    public void fatal(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_ERR);
    }

    public void info(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_INFO);
    }

    public void info(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_INFO);
    }

    public void trace(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_VERBOSE);
    }

    public void trace(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_VERBOSE);
    }

    public void warn(Object message, Throwable th) {
        parentTask.log(String.valueOf(message), Project.MSG_WARN);
    }

    public void warn(Object message) {
        parentTask.log(String.valueOf(message), Project.MSG_WARN);
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public boolean isFatalEnabled() {
        return true;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public boolean isTraceEnabled() {
        return true;
    }
}
