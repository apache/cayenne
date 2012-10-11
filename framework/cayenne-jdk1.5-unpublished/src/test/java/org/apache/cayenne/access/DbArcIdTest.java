package org.apache.cayenne.access;

import junit.framework.TestCase;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbEntity;

public class DbArcIdTest extends TestCase {

    public void testHashCode() {
        
        DbEntity e = new DbEntity("X");

        DbArcId id1 = new DbArcId(e, new ObjectId("x", "k", "v"), "r1");
        int h1 = id1.hashCode();
        assertEquals(h1, id1.hashCode());
        assertEquals(h1, id1.hashCode());

        DbArcId id1_eq = new DbArcId(e, new ObjectId("x", "k", "v"), "r1");
        assertEquals(h1, id1_eq.hashCode());

        DbArcId id2 = new DbArcId(e, new ObjectId("x", "k", "v"), "r2");
        assertFalse(h1 == id2.hashCode());

        DbArcId id3 = new DbArcId(e, new ObjectId("y", "k", "v"), "r1");
        assertFalse(h1 == id3.hashCode());
    }

    public void testEquals() {

        DbEntity e = new DbEntity("X");
        
        DbArcId id1 = new DbArcId(e, new ObjectId("x", "k", "v"), "r1");
        assertTrue(id1.equals(id1));

        DbArcId id1_eq = new DbArcId(e, new ObjectId("x", "k", "v"), "r1");
        assertTrue(id1.equals(id1_eq));
        assertTrue(id1_eq.equals(id1));

        DbArcId id2 = new DbArcId(e, new ObjectId("x", "k", "v"), "r2");
        assertFalse(id1.equals(id2));

        DbArcId id3 = new DbArcId(e, new ObjectId("y", "k", "v"), "r1");
        assertFalse(id1.equals(id3));

        assertFalse(id1.equals(new Object()));
    }
}
