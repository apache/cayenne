package org.apache.cayenne.access;

import junit.framework.TestCase;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.CayenneException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class AbstractDbLoaderDelegateTest extends TestCase {

    final class TestDbLoaderDelegate extends AbstractDbLoaderDelegate {}

    private AbstractDbLoaderDelegate delegate;
    private DataMap dataMap;
    private DbEntity dbEntity;
    private ObjEntity objEntity;

    public void setUp() {
        delegate = new TestDbLoaderDelegate();
        dataMap = new DataMap();

        dbEntity = new DbEntity("TestDbEntity");
        dbEntity.setDataMap(dataMap);
        
        objEntity = new ObjEntity("TestObjEntity");
        objEntity.setDataMap(dataMap);
    }

    public void testOverwriteDbEntity() throws CayenneException {
        assertFalse(delegate.overwriteDbEntity(dbEntity));
    }

    public void testDbEntityAdded() {
        delegate.dbEntityAdded(dbEntity);

        final List<DbEntity> entities = Arrays.asList(dbEntity);

        assertEquals(1, dataMap.getDbEntities().size());
        assertTrue(dataMap.getDbEntities().containsAll(entities));
        
        assertEquals(entities, delegate.getAddedDbEntities());
    }

    public void testDbEntityRemoved() {
        // Make sure the entity is in the datamap to start.
        dataMap.addDbEntity(dbEntity);

        delegate.dbEntityRemoved(dbEntity);

        // The entity should no longer be in the map.
        assertEquals(0, dataMap.getDbEntities().size());

        assertEquals(Arrays.asList(dbEntity), delegate.getRemovedDbEntities());
    }

    public void testObjEntityAdded() {
        delegate.objEntityAdded(objEntity);

        final List<ObjEntity> entities = Arrays.asList(objEntity);

        assertEquals(1, dataMap.getObjEntities().size());
        assertTrue(dataMap.getObjEntities().containsAll(entities));

        assertEquals(entities, delegate.getAddedObjEntities());
    }

    public void testObjEntityRemoved() {
        // Make sure the entity is in the datamap to start.
        dataMap.addObjEntity(objEntity);

        delegate.objEntityRemoved(objEntity);

        // The entity should no longer be in the map.
        assertEquals(0, dataMap.getObjEntities().size());

        assertEquals(Arrays.asList(objEntity), delegate.getRemovedObjEntities());
    }
}
