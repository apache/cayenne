package de.jexp.jequel.java;

import junit.framework.TestCase;

public class GenericCallTest extends TestCase {
    public void testGenericCall() {
        check(new B());
        check(new C());
    }

    private <T> void check(final A<T> b) {
        assertEquals("10", b.get().toString());
    }

    private void check(final C c) {
        assertEquals("abc", c.get());
    }


    interface A<T> {
        T get();
    }

    class B implements A<Integer> {
        public Integer get() {
            return 10;
        }
    }

    class C implements A<String> {
        public String get() {
            return "abc";
        }
    }
}