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

package org.apache.cayenne.gen;

import java.io.File;

import org.apache.tools.ant.Task;

/**
 * Ant-specific extension of DefaultClassGenerator that provides logging functions.
 * 
 * @deprecated since 3.0 this class is no longer relevant.
 */
public class AntClassGenerator extends DefaultClassGenerator {

    protected Task parentTask;

    @Override
    protected File fileForSuperclass(String pkgName, String className) throws Exception {

        File outFile = super.fileForSuperclass(pkgName, className);
        if (outFile != null) {
            parentTask.log("Generating superclass file: " + outFile.getCanonicalPath());
        }

        return outFile;
    }

    @Override
    protected File fileForClass(String pkgName, String className) throws Exception {

        File outFile = super.fileForClass(pkgName, className);
        if (outFile != null) {
            parentTask.log("Generating class file: " + outFile.getCanonicalPath());
        }
        return outFile;
    }

    public Task getParentTask() {
        return parentTask;
    }

    /**
     * Sets the Ant task that uses this generator.
     * 
     * @param parentTask An Ant task that provides Ant context to this generator.
     */
    public void setParentTask(Task parentTask) {
        this.parentTask = parentTask;
    }
}
