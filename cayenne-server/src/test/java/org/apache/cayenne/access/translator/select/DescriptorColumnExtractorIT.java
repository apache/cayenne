package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.testmap.CompoundPaintingLongNames;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DescriptorColumnExtractorIT extends ServerCase {
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