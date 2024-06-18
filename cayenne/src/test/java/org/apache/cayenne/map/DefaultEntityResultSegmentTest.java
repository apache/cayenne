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
package org.apache.cayenne.map;

import org.apache.cayenne.reflect.PersistentDescriptor;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class DefaultEntityResultSegmentTest {
    private final List<String> expectedColumnPath = List.of("key1", "key2");

    private final Map<String, String> fields =
            new ConcurrentHashMap<>(Map.of("key1", "value1", "key2", "value2"));

    private final DefaultEntityResultSegment resultSegment =
            new DefaultEntityResultSegment(new PersistentDescriptor(), fields, fields.size());

    @Test
    public void testGetColumnPath() {
        List<String> actualColumnPath = fields.values()
                .stream()
                .map(resultSegment::getColumnPath)
                .collect(Collectors.toList());

        assertEquals(expectedColumnPath.size(), actualColumnPath.size());

        IntStream.range(0, actualColumnPath.size())
                .forEach(i -> assertEquals(expectedColumnPath.get(i), actualColumnPath.get(i)));
    }

}