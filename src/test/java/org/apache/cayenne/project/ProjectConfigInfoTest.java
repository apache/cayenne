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


package org.apache.cayenne.project;

import java.io.File;

import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class ProjectConfigInfoTest extends CayenneCase {
    protected ProjectConfigInfo config;

    protected void setUp() throws Exception {
        super.setUp();
        config = new ProjectConfigInfo();
    }

    public void testSourceJar() throws Exception {
        File f = new File("xyz");
        config.setSourceJar(f);
        assertSame(f, config.getSourceJar());
    }

    public void testDestJar() throws Exception {
        File f = new File("xyz");
        config.setDestJar(f);
        assertSame(f, config.getDestJar());
    }

    public void testAltProjectFile() throws Exception {
        File f = new File("xyz");
        config.setAltProjectFile(f);
        assertSame(f, config.getAltProjectFile());
    }
}
