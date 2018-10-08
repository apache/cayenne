package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

public class WeakReferenceStrategyModule implements Module {
    @Override
    public void configure(Binder binder) {
        ServerModule.contributeProperties(binder)
                //Use in ObjectStoreGCIT
                .put(Constants.SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY, "weak");
    }
}
