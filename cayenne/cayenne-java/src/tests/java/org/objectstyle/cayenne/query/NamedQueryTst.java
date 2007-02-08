package org.objectstyle.cayenne.query;

import junit.framework.TestCase;

import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.util.Util;

public class NamedQueryTst extends TestCase {

    public void testName() {
        NamedQuery query = new NamedQuery("abc");

        assertEquals("abc", query.getName());
        query.setName("123");
        assertEquals("123", query.getName());
    }

    public void testQueryName() {
        NamedQuery query = new NamedQuery("abc");
        assertEquals("abc", query.getName());
    }

    public void testSerializability() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = Util.cloneViaSerialization(o);

        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;

        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
    }

    public void testSerializabilityWithHessian() throws Exception {
        NamedQuery o = new NamedQuery("abc");
        Object clone = HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());

        assertTrue(clone instanceof NamedQuery);
        NamedQuery c1 = (NamedQuery) clone;

        assertNotSame(o, c1);
        assertEquals(o.getName(), c1.getName());
    }

    /**
     * Proper 'equals' and 'hashCode' implementations are important when mapping results
     * obtained in a QueryChain back to the query.
     */
    public void testEquals() throws Exception {
        NamedQuery q1 = new NamedQuery("abc", new String[] {
                "a", "b"
        }, new Object[] {
                "1", "2"
        });

        NamedQuery q2 = new NamedQuery("abc", new String[] {
                "a", "b"
        }, new Object[] {
                "1", "2"
        });

        NamedQuery q3 = new NamedQuery("abc", new String[] {
                "a", "b"
        }, new Object[] {
                "1", "3"
        });

        NamedQuery q4 = new NamedQuery("123", new String[] {
                "a", "b"
        }, new Object[] {
                "1", "2"
        });

        assertTrue(q1.equals(q2));
        assertEquals(q1.hashCode(), q2.hashCode());

        assertFalse(q1.equals(q3));
        assertFalse(q1.hashCode() == q3.hashCode());

        assertFalse(q1.equals(q4));
        assertFalse(q1.hashCode() == q4.hashCode());
    }
}
