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

package org.apache.cayenne.map;

import org.apache.cayenne.property.ArcProperty;
import org.apache.cayenne.property.ClassDescriptor;
import org.apache.cayenne.property.MockClassDescriptor;
import org.apache.cayenne.property.MockClassDescriptorFactory;
import org.apache.cayenne.property.Property;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneTestCase;
import org.apache.cayenne.unit.CayenneTestResources;

public class EntityResolverClassDescriptorTst extends CayenneTestCase {

    /**
     * Configures multi-tier stack as we want to access descriptors in different tiers...
     */
    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testServerDescriptorCaching() {
        EntityResolver resolver = getDomain().getEntityResolver();
        resolver.setClassDescriptorFactory(null);

        ClassDescriptor descriptor = resolver.getClassDescriptor("MtTable1");
        assertNotNull(descriptor);
        assertSame(descriptor, resolver.getClassDescriptor("MtTable1"));
        resolver.setClassDescriptorFactory(null);

        ClassDescriptor descriptor1 = resolver.getClassDescriptor("MtTable1");
        assertNotNull(descriptor1);
        assertNotSame(descriptor, descriptor1);
    }

    public void testServerDescriptorFactory() {
        EntityResolver resolver = getDomain().getEntityResolver();
        resolver.setClassDescriptorFactory(null);

        MockClassDescriptor mockDescriptor = new MockClassDescriptor();
        resolver
                .setClassDescriptorFactory(new MockClassDescriptorFactory(mockDescriptor));
        try {
            ClassDescriptor descriptor = resolver.getClassDescriptor("MtTable1");
            assertNotNull(descriptor);
            assertSame(mockDescriptor, descriptor);
        }
        finally {
            resolver.setClassDescriptorFactory(null);
        }
    }

    public void testArcProperties() {
        EntityResolver resolver = getDomain().getEntityResolver();
        resolver.setClassDescriptorFactory(null);

        ClassDescriptor descriptor = resolver.getClassDescriptor("MtTable1");
        assertNotNull(descriptor);

        Property p = descriptor.getProperty(MtTable1.TABLE2ARRAY_PROPERTY);
        assertTrue(p instanceof ArcProperty);

        ClassDescriptor target = ((ArcProperty) p).getTargetDescriptor();
        assertNotNull(target);
        assertSame(resolver.getClassDescriptor("MtTable2"), target);
        assertNotNull(((ArcProperty) p).getComplimentaryReverseArc());
        assertEquals(MtTable2.TABLE1_PROPERTY, ((ArcProperty) p)
                .getComplimentaryReverseArc()
                .getName());
    }
}
