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

package org.apache.cayenne.tools;

import org.slf4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.slf4j.Marker;

/**
 * @since 3.0
 */
class AntLogger implements Logger {

    private Task parentTask;

    public AntLogger(Task parentTask) {
        this.parentTask = parentTask;
    }

    @Override
    public void debug(String message) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(String message, Object object) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(String message, Object... objects) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_DEBUG);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String message) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(Marker marker, String message, Object object) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(Marker marker, String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(Marker marker, String message, Object... objects) {
        parentTask.log(message, Project.MSG_DEBUG);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void error(String message) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(String message, Object object) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(String message, Object... objects) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_ERR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String message) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(Marker marker, String message, Object object) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(Marker marker, String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(Marker marker, String message, Object... objects) {
        parentTask.log(message, Project.MSG_ERR);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_ERR);
    }

    @Override
    public void info(String message) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(String message, Object object) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(String message, Object... objects) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_INFO);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String message) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(Marker marker, String message, Object object) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(Marker marker, String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(Marker marker, String message, Object... objects) {
        parentTask.log(message, Project.MSG_INFO);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return parentTask.getTaskName();
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String message) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(String message, Object object) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(String message, Object... objects) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_VERBOSE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String message) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(Marker marker, String message, Object object) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(Marker marker, String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(Marker marker, String message, Object... objects) {
        parentTask.log(message, Project.MSG_VERBOSE);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_VERBOSE);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void warn(String message) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(String message, Object object) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(String message, Object... objects) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_WARN);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String message) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(Marker marker, String message, Object object) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(Marker marker, String message, Object object, Object secondObject) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(Marker marker, String message, Object... objects) {
        parentTask.log(message, Project.MSG_WARN);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        parentTask.log(message, throwable, Project.MSG_WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }
}
