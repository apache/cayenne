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
package org.apache.cayenne.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CayenneMcpOptionsTest {

    @Test
    public void emptyArgs() {
        CayenneMcpOptions opts = CayenneMcpOptions.parse(new String[0]);
        assertFalse(opts.isHelp());
        assertFalse(opts.isVersion());
    }

    @Test
    public void shortHelp() {
        CayenneMcpOptions opts = CayenneMcpOptions.parse(new String[]{"-h"});
        assertTrue(opts.isHelp());
        assertFalse(opts.isVersion());
    }

    @Test
    public void longHelp() {
        CayenneMcpOptions opts = CayenneMcpOptions.parse(new String[]{"--help"});
        assertTrue(opts.isHelp());
    }

    @Test
    public void shortVersion() {
        CayenneMcpOptions opts = CayenneMcpOptions.parse(new String[]{"-V"});
        assertTrue(opts.isVersion());
        assertFalse(opts.isHelp());
    }

    @Test
    public void longVersion() {
        CayenneMcpOptions opts = CayenneMcpOptions.parse(new String[]{"--version"});
        assertTrue(opts.isVersion());
    }
}
