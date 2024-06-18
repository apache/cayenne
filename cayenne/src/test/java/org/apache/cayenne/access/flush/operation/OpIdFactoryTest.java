package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;
import org.junit.Test;

import static org.junit.Assert.*;

public class OpIdFactoryTest {

    @Test
    public void testEqualsAndHashCode() {
        ObjectId idSource1 = ObjectId.of("db:test");
        idSource1.getReplacementIdMap().put("id", 1);

        ObjectId idSource2 = ObjectId.of("db:test");
        idSource2.getReplacementIdMap().put("id", 1);

        ObjectId idSource3 = ObjectId.of("db:test");
        idSource3.getReplacementIdMap().put("id2", 1);

        ObjectId idSource4 = ObjectId.of("db:test2");
        idSource4.getReplacementIdMap().put("id", 1);

        ObjectId idSource5 = ObjectId.of("db:test2");
        idSource5.getReplacementIdMap().put("id", 1);

        ObjectId idSource6 = ObjectId.of("db:test", "id", 1);

        ObjectId id1 = OpIdFactory.idForOperation(idSource1);
        ObjectId id2 = OpIdFactory.idForOperation(idSource2);
        ObjectId id3 = OpIdFactory.idForOperation(idSource3);
        ObjectId id4 = OpIdFactory.idForOperation(idSource4);
        ObjectId id5 = OpIdFactory.idForOperation(idSource5);
        ObjectId id6 = OpIdFactory.idForOperation(idSource6);

        assertEquals(id1, id1);
        assertEquals(id2, id2);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());

        assertEquals(id4, id4);
        assertEquals(id5, id5);
        assertEquals(id4, id5);
        assertEquals(id4.hashCode(), id5.hashCode());

        assertNotEquals(id1, id3);
        assertNotEquals(id1.hashCode(), id3.hashCode());
        assertNotEquals(id1, id4);
        assertNotEquals(id1.hashCode(), id4.hashCode());
        assertNotEquals(id2, id5);
        assertNotEquals(id2.hashCode(), id5.hashCode());
        assertNotEquals(id3, id4);
        assertNotEquals(id3.hashCode(), id4.hashCode());

        assertNotEquals(id1, id6);
        assertNotEquals(id1.hashCode(), id6.hashCode());

        assertNotSame(idSource1, id1);
        assertNotSame(idSource2, id2);
        assertNotSame(idSource3, id3);
        assertNotSame(idSource4, id4);
        assertNotSame(idSource5, id5);
        assertSame(idSource6, id6);
    }


}