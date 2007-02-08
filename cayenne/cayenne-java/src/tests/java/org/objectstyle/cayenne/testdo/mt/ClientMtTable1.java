package org.objectstyle.cayenne.testdo.mt;

import java.util.List;

import org.objectstyle.cayenne.testdo.mt.auto._ClientMtTable1;

public class ClientMtTable1 extends _ClientMtTable1 {

    // provide direct access to persistent properties for testing..

    public String getGlobalAttribute1Direct() {
        return globalAttribute1;
    }

    public String getServerAttribute1Direct() {
        return serverAttribute1;
    }

    public List getTable2ArrayDirect() {
        return table2Array;
    }

}
