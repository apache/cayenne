package org.objectstyle.cayenne;

import org.objectstyle.cayenne.CayenneContext;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class GeneratedMethodsInClientObjectContextTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testAddToList() throws Exception {

        EntityResolver resolver = getDomain()
                .getEntityResolver()
                .getClientEntityResolver();

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ClientMtTable1 t1 = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = (ClientMtTable2) context.newObject(ClientMtTable2.class);

        t1.addToTable2Array(t2);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());
        
        // do it again to make sure action can handle series of changes
        ClientMtTable1 t3 = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        ClientMtTable2 t4 = (ClientMtTable2) context.newObject(ClientMtTable2.class);

        t3.addToTable2Array(t4);
        assertEquals(1, t3.getTable2Array().size());
        assertSame(t3, t4.getTable1());
    }

    public void testSetValueHolder() throws Exception {

        EntityResolver resolver = getDomain()
                .getEntityResolver()
                .getClientEntityResolver();

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ClientMtTable1 t1 = (ClientMtTable1) context.newObject(ClientMtTable1.class);
        ClientMtTable2 t2 = (ClientMtTable2) context.newObject(ClientMtTable2.class);

        t2.setTable1(t1);
        assertEquals(1, t1.getTable2Array().size());
        assertSame(t1, t2.getTable1());
    }
}
