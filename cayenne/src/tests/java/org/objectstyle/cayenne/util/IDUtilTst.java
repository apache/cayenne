package org.objectstyle.cayenne.util;

import org.objectstyle.cayenne.access.types.ByteArrayTypeTst;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class IDUtilTst extends CayenneTestCase {

    public void testPseudoUniqueByteSequence1() throws Exception {
        try {
            IDUtil.pseudoUniqueByteSequence(10);
            fail("must throw an exception on short sequences");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPseudoUniqueByteSequence2() throws Exception {
        byte[] byte16 = IDUtil.pseudoUniqueByteSequence(16);
        assertNotNull(byte16);
        assertEquals(16, byte16.length);

        try {
            ByteArrayTypeTst.assertByteArraysEqual(
                byte16,
                IDUtil.pseudoUniqueByteSequence(16));
            fail("Same byte array..");
        } catch (Throwable th) {

        }
    }

    public void testPseudoUniqueByteSequence3() throws Exception {
        byte[] byte17 = IDUtil.pseudoUniqueByteSequence(17);
        assertNotNull(byte17);
        assertEquals(17, byte17.length);

        byte[] byte123 = IDUtil.pseudoUniqueByteSequence(123);
        assertNotNull(byte123);
        assertEquals(123, byte123.length);
    }

}
