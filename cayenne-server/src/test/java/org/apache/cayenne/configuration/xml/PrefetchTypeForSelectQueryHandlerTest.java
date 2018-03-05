package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.1
 */
public class PrefetchTypeForSelectQueryHandlerTest extends BaseHandlerTest{

    @Test
    public void testLoad() throws Exception {
        final DataMap map = new DataMap();

        parse("query", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new QueryDescriptorHandler(parent, map);
            }
        });

        SelectQueryDescriptor selectQueryDescriptor = (SelectQueryDescriptor) map.getQueryDescriptor("query");
        assertEquals(3, selectQueryDescriptor.getPrefetchesMap().size());

        assertEquals(1, (int) selectQueryDescriptor.getPrefetchesMap().get("paintings"));
        assertEquals(2, (int) selectQueryDescriptor.getPrefetchesMap().get("paintings.artist"));
        assertEquals(3, (int) selectQueryDescriptor.getPrefetchesMap().get("paintings.gallery"));
    }

}
