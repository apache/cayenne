package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.testdo.generated.GeneratedColumnTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@UseServerRuntime(ServerCase.GENERATED_PROJECT)
public class BatchActionGeneratedIT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testHasGeneratedKeys1() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that supports keys
        JdbcAdapter adapter = buildAdapter(true);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        assertTrue(new BatchAction(batch1, node, false).hasGeneratedKeys());
    }

    @Test
    public void testHasGeneratedKeys2() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that does not support keys...
        JdbcAdapter adapter = buildAdapter(false);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        assertFalse(new BatchAction(batch1, node, false).hasGeneratedKeys());
    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
