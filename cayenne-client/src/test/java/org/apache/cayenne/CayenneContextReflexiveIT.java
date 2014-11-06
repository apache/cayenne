package org.apache.cayenne;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.testdo.reflexive.ClientReflexive;
import org.apache.cayenne.testdo.reflexive.Reflexive;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

@UseServerRuntime(ClientCase.REFLEXIVE_PROJECT)
public class CayenneContextReflexiveIT extends ClientCase {

    @Inject
    private CayenneContext clientContext;

    @Inject
    private ClientServerChannel clientServerChannel;

    @Test
    public void testCAY830() throws Exception {

        // an exception was triggered within POST_LOAD callback
        LifecycleCallbackRegistry callbackRegistry = clientServerChannel
                .getEntityResolver()
                .getCallbackRegistry();

        try {
            callbackRegistry.addListener(Reflexive.class, new LifecycleListener() {

                public void postLoad(Object entity) {
                }

                public void postPersist(Object entity) {
                }

                public void postRemove(Object entity) {
                }

                public void postUpdate(Object entity) {
                }

                public void postAdd(Object entity) {
                }

                public void preRemove(Object entity) {
                }

                public void preUpdate(Object entity) {
                }

                public void prePersist(Object entity) {
                }
            });

            ClientReflexive o1 = clientContext.newObject(ClientReflexive.class);
            o1.setName("parent");

            ClientReflexive o2 = clientContext.newObject(ClientReflexive.class);
            o2.setName("child");
            o2.setToParent(o1);
            clientContext.commitChanges();

            clientContext.deleteObjects(o1);
            clientContext.deleteObjects(o2);
            clientContext.commitChanges();
            // per CAY-830 an exception is thrown here
        }
        finally {
            callbackRegistry.clear();
        }
    }
}
