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


package org.apache.cayenne.modeler.dialog.validator;

import javax.swing.JFrame;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.project.validator.ValidationInfo;

/**
 * JUnit tests for ValidationDisplayHandler class.
 * 
 */
public class ValidationDisplayHandlerTest extends TestCase {

    public void testValidationInfo() throws Exception {
        ValidationInfo info = new ValidationInfo(-1, "123", null);
        ValidationDisplayHandler handler = new ConcreteErrorMsg(info);

        assertSame(info, handler.getValidationInfo());
    }

    public void testMessage() throws Exception {
        String msg = "abc";
        ValidationInfo info = new ValidationInfo(-1, msg, null);
        ValidationDisplayHandler handler = new ConcreteErrorMsg(info);

        assertSame(msg, handler.getMessage());
    }

    public void testSeverity() throws Exception {
        ValidationInfo info =
            new ValidationInfo(ValidationDisplayHandler.ERROR, null, null);
        ValidationDisplayHandler handler = new ConcreteErrorMsg(info);

        assertEquals(ValidationDisplayHandler.ERROR, handler.getSeverity());
    }

    public void testDomain() throws Exception {
        ValidationInfo info =
            new ValidationInfo(ValidationDisplayHandler.ERROR, null, null);
        ValidationDisplayHandler handler = new ConcreteErrorMsg(info);

        DataDomain dom = new DataDomain("test");
        assertNull(handler.getDomain());
        handler.setDomain(dom);
        assertSame(dom, handler.getDomain());
    }

    class ConcreteErrorMsg extends ValidationDisplayHandler {

        /**
         * Constructor for ConcreteErrorMsg.
         * @param validation
         */
        public ConcreteErrorMsg(ValidationInfo validation) {
            super(validation);
        }

        public void displayField(ProjectController mediator, JFrame frame) {}
    }
}
