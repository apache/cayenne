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

package org.apache.cayenne.modeler.util;

import org.junit.Before;
import org.junit.Test;

import javax.swing.filechooser.FileFilter;
import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class DataMapFileFilterTest {

    protected FileFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = FileFilters.getDataMapFilter();
    }

    @Test
    public void testAcceptDir() throws Exception {
        assertTrue(filter.accept(new File(".")));
    }

    @Test
    public void testRejectCayenneXml() throws Exception {
        assertFalse(filter.accept(new File("cayenne.xml")));
    }

    @Test
    public void testRejectOther() throws Exception {
        assertFalse(filter.accept(new File("somefile.txt")));
    }

    @Test
    public void testRejectHiddenMapXml() throws Exception {
        assertFalse(filter.accept(new File(".map.xml")));
    }

    @Test
    public void testAcceptMapXml() throws Exception {
        assertTrue(filter.accept(new File("xyz.map.xml")));
    }

    @Test
    public void testRejectMixedCase() throws Exception {
        assertFalse(filter.accept(new File("xyz.MAP.xml")));
    }
}
