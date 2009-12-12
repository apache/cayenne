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
package org.apache.cayenne.project2;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.Configurable;
import org.apache.cayenne.configuration.ConfigurationVisitor;

public class ProjectTest extends TestCase {

    public void testRootNode() {

        Configurable object = new Configurable() {

            public <T> T acceptVisitor(ConfigurationVisitor<T> visitor) {
                return null;
            }
        };

        Project<Configurable> project = new Project<Configurable>(object);
        assertSame(object, project.getRootNode());
    }

    public void testVersion() {

        Configurable object = new Configurable() {

            public <T> T acceptVisitor(ConfigurationVisitor<T> visitor) {
                return null;
            }
        };

        Project<Configurable> project = new Project<Configurable>(object);

        assertNull(project.getVersion());
        project.setVersion("1.1");
        assertEquals("1.1", project.getVersion());
    }
}
