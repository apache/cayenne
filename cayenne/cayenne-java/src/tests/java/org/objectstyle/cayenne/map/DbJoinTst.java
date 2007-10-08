package org.objectstyle.cayenne.map;

import junit.framework.TestCase;

/**
 * @author Andrei Adamchik
 */
public class DbJoinTst extends TestCase {

    public void testRelationship() throws Exception {
        DbJoin join = new DbJoin(null);
        assertNull(join.getRelationship());

        DbRelationship relationship = new DbRelationship("abc");
        join.setRelationship(relationship);
        assertSame(relationship, join.getRelationship());
    }

}