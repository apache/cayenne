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

package org.apache.cayenne.project.validator;

import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ValidationInfoTest extends CayenneCase {

    public void testSeverity() throws Exception {
        int severity = ValidationInfo.WARNING;
        ValidationInfo info = new ValidationInfo(severity, null, null);
        assertEquals(severity, info.getSeverity());
    }

    public void testMessage() throws Exception {
        String message = "abcde";
        ValidationInfo info = new ValidationInfo(-1, message, null);
        assertEquals(message, info.getMessage());
    }
    
    public void testPath() throws Exception {
        ProjectPath path = new ProjectPath(new Object());
        ValidationInfo info = new ValidationInfo(-1, null, path);
        assertSame(path, info.getPath());
    }
}
