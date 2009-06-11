package org.apache.cayenne.testdo.locking;

import org.apache.cayenne.testdo.locking.auto._SoftTest;

public class SoftTest extends _SoftTest {

    @Override
    protected void onPrePersist() {
        setDeleted(false);
    }

}
