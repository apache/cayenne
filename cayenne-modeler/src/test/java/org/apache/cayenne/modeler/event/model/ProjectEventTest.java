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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.project.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;


public class ProjectEventTest {

    @Test
    public void testConstructor1() throws Exception {
    	Object src = new Object();
    	Project d = new Project();
    	ProjectEvent e = ProjectEvent.ofChange(src, d);
    	
    	assertSame(src, e.getSource());
    	assertSame(d, e.getProject());
    }

    @Test
    public void testConstructor2() throws Exception  {
    	Object src = new Object();
    	Project d = new Project();
    	ProjectEvent e = ProjectEvent.ofChange(src, d, "oldname");
    	
    	assertSame(src, e.getSource());
    	assertSame(d, e.getProject());
    	assertEquals("oldname", e.getOldName());
    }

}

