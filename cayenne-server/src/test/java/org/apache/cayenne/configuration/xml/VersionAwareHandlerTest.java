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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @since 4.1
 */
public class VersionAwareHandlerTest {

    private static String[] VERSION_SET_1 = {"10", "11", "9"}; // sorted as strings
    private static String[] VERSION_SET_2 = {"10"};

    VersionAwareHandler handler;

    @Before
    public void createHandler() {
        handler = new VersionAwareHandler(new LoaderContext(null, null), "test"){
        };
    }

    private Attributes createAttributesWithVersion(String version) {
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "project-version", "project-version", "", version);
        return attributes;
    }

    @Test
    public void validateCorrectVersion() {
        handler.validateVersion(createAttributesWithVersion("9"), VERSION_SET_1);
        handler.validateVersion(createAttributesWithVersion("10"), VERSION_SET_1);
        handler.validateVersion(createAttributesWithVersion("11"), VERSION_SET_1);
        handler.validateVersion(createAttributesWithVersion("10"), VERSION_SET_2);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void validateIncorrectVersion1() {
        handler.validateVersion(createAttributesWithVersion("8"), VERSION_SET_1);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void validateIncorrectVersion2() {
        handler.validateVersion(createAttributesWithVersion("11"), VERSION_SET_2);
    }
}