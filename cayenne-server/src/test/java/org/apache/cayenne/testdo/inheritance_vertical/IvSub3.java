package org.apache.cayenne.testdo.inheritance_vertical;

import org.apache.cayenne.testdo.inheritance_vertical.auto._IvSub3;

public class IvSub3 extends _IvSub3 {

    private static final long serialVersionUID = 1L;

    @Override
    protected void onPrePersist() {
//        if(getIvRoot() == null) {
//            throw new IllegalStateException("IvRoot must be set");
//        }
    }
}
