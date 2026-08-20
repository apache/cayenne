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
package org.apache.cayenne.configuration;

import org.apache.cayenne.map.DataMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultDataChannelDescriptorMergerTest {

    @Test
    public void singleDescriptor() {
        DataChannelDescriptor descriptor = new DataChannelDescriptor();
        descriptor.setName("Zx");

        DefaultDataChannelDescriptorMerger merger = new DefaultDataChannelDescriptorMerger();

        DataChannelDescriptor merged = merger.merge(descriptor);
        assertSame(descriptor, merged);
        assertEquals("Zx", merged.getName());
    }

    @Test
    public void merged_Name() {
        DataChannelDescriptor d1 = new DataChannelDescriptor();
        d1.setName("Zx");

        DataChannelDescriptor d2 = new DataChannelDescriptor();
        d2.setName("Ym");

        DefaultDataChannelDescriptorMerger merger = new DefaultDataChannelDescriptorMerger();

        DataChannelDescriptor merged = merger.merge(d1, d2);
        assertNotSame(d1, merged);
        assertNotSame(d2, merged);
        assertEquals("Ym", merged.getName());
    }

    @Test
    public void merged_Properties() {
        DataChannelDescriptor d1 = new DataChannelDescriptor();
        d1.getProperties().put("X", "1");
        d1.getProperties().put("Y", "2");

        DataChannelDescriptor d2 = new DataChannelDescriptor();
        d2.getProperties().put("X", "3");
        d2.getProperties().put("Z", "4");

        DefaultDataChannelDescriptorMerger merger = new DefaultDataChannelDescriptorMerger();

        DataChannelDescriptor merged = merger.merge(d1, d2);
        assertEquals(2, merged.getProperties().size());
        assertEquals("3", merged.getProperties().get("X"));
        assertEquals("4", merged.getProperties().get("Z"));
    }

    @Test
    public void merged_DataMaps() {
        DataChannelDescriptor d1 = new DataChannelDescriptor();
        d1.setName("Zx");
        DataMap m11 = new DataMap("A");
        DataMap m12 = new DataMap("B");
        d1.getDataMaps().add(m11);
        d1.getDataMaps().add(m12);

        DataChannelDescriptor d2 = new DataChannelDescriptor();
        d2.setName("Ym");
        DataMap m21 = new DataMap("C");
        DataMap m22 = new DataMap("A");
        d2.getDataMaps().add(m21);
        d2.getDataMaps().add(m22);

        DefaultDataChannelDescriptorMerger merger = new DefaultDataChannelDescriptorMerger();

        DataChannelDescriptor merged = merger.merge(d1, d2);

        assertEquals(3, merged.getDataMaps().size());
        assertSame(m22, merged.getDataMap("A"));
        assertSame(m12, merged.getDataMap("B"));
        assertSame(m21, merged.getDataMap("C"));
    }
}
