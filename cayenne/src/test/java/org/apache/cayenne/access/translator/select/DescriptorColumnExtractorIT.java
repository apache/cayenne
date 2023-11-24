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
package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.testmap.CompoundPaintingLongNames;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DescriptorColumnExtractorIT extends RuntimeCase {
    @Inject
    private ObjectContext context;

    private final List<String> expectedFlattenedDbFields = List.of(
            "toArtist.ARTIST_ID",
            "toArtist.ARTIST_NAME",
            "toGallery.GALLERY_ID",
            "toGallery.GALLERY_NAME",
            "toPaintingInfo.PAINTING_ID",
            "toPaintingInfo.TEXT_REVIEW");

    @Test
    public void testEntityResultAddDbFieldsForFlattenedAttributes() {

        EntityResolver resolver = context.getEntityResolver();
        ClassDescriptor classDescriptor = resolver.getClassDescriptor("CompoundPaintingLongNames");

        TranslatorContext translatorContext = new TranslatorContext(
                new FluentSelectWrapper(ObjectSelect.query(CompoundPaintingLongNames.class)
                        .column(CompoundPaintingLongNames.SELF)),
                Mockito.mock(DbAdapter.class),
                resolver,
                null);

        DescriptorColumnExtractor descriptor = new DescriptorColumnExtractor(translatorContext, classDescriptor);
        descriptor.extract();

        List<String> actualFlattenedDbFields = translatorContext.getRootEntityResult()
                .getDbFields(resolver)
                .keySet()
                .stream()
                .filter(key -> key.startsWith("to"))
                .sorted()
                .collect(Collectors.toList());

        assertEquals(expectedFlattenedDbFields.get(0), actualFlattenedDbFields.get(0));
        assertEquals(expectedFlattenedDbFields.get(1), actualFlattenedDbFields.get(1));
        assertEquals(expectedFlattenedDbFields.get(2), actualFlattenedDbFields.get(2));
        assertEquals(expectedFlattenedDbFields.get(3), actualFlattenedDbFields.get(3));
        assertEquals(expectedFlattenedDbFields.get(4), actualFlattenedDbFields.get(4));
        assertEquals(expectedFlattenedDbFields.get(5), actualFlattenedDbFields.get(5));
    }
}