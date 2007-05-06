package org.objectstyle.cayenne.access;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextRefetchTst extends CayenneTestCase {

    public void testRefetchTempId() {
        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());

        try {
            DataContext context = getDomain().createDataContext();
            ObjectId tempID = new ObjectId("Artist");

            try {
                context.refetchObject(tempID);
                fail("Refetching temp ID must have generated an error.");
            }
            catch (CayenneRuntimeException ex) {
                // expected ... but check that no queries were run
                assertEquals("Refetching temp id correctly failed, "
                        + "but DataContext shouldn't have run a query", 0, engine
                        .getRunCount());
            }
        }
        finally {
            engine.stopInterceptNode();
        }
    }

}