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

package org.apache.cayenne.project.upgrade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpgradeContextTest {

    @Test
    public void recordChange() {
        UpgradeContext context = new UpgradeContext(null, null);
        context.recordChange("info", List.of(), false);
        context.recordChange("destructive", List.of("a: b"), true);

        assertEquals(List.of(
                new UpgradeChange("info", List.of(), false),
                new UpgradeChange("destructive", List.of("a: b"), true)), context.getChanges());
        assertTrue(context.getObsoleteFiles().isEmpty());
    }
}
