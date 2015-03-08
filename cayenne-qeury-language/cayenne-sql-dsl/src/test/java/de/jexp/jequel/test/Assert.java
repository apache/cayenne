package de.jexp.jequel.test;

import java.util.Collection;

/**
 * @author mh14 @ jexp.de
 * @since 04.11.2007 23:38:00 (c) 2007 jexp.de
 */
public class Assert {
    public static <T> void assertEquals(String msg, final Collection<? extends T> expected, final Collection<? extends T> got) {
        msg += " expected " + expected + " got " + got + " ";
        junit.framework.Assert.assertEquals(msg + " size", expected.size(), got.size());
        junit.framework.Assert.assertTrue(msg + " all in expected", expected.containsAll(got));
        junit.framework.Assert.assertTrue(msg + " all in got", got.containsAll(expected));
    }
}
