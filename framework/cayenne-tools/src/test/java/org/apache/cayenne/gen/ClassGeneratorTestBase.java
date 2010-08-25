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

import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.cayenne.map.ObjEntity;

/**
 * Superclass of ClassGenerator tests.
 * @deprecated since 3.0
 */
public abstract class ClassGeneratorTestBase extends TestCase {

    protected ClassGenerator cgen;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cgen = createGenerator();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        cgen = null;
    }

    protected abstract ClassGenerator createGenerator() throws Exception;

    protected String generatedString(ObjEntity entity) throws Exception {
        StringWriter writer = new StringWriter();
        cgen.generateClass(writer, entity);
        return writer.toString();
    }

}
