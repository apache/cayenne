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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Base task for all Cayenne ant tasks, providing support for common configuration items.
 *
 * @author Kevin Menard
 * @since 1.2
 */
public class CayenneTask extends Task
{
    protected Path classpath;

    /**
     * Sets the classpath used by the task.
     *
     * @param path The classpath to set.
     */
    public void setClasspath(Path path) {
        createClasspath().append(path);
    }

    /**
     * Sets the classpath reference used by the task.
     *
     * @param reference The classpath reference to set.
     */
    public void setClasspathRef(Reference reference) {
        createClasspath().setRefid(reference);
    }

    /**
     * Convenience method for creating a classpath instance to be used for the task.
     *
     * @return The new classpath.
     */
    private Path createClasspath() {
        if (null == classpath) {
            classpath = new Path(getProject());
        }

        return classpath.createPath();
    }
}
