package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.spi.DefaultScope;

public class WeakReferenceStrategyServerCaseModule extends ServerCaseModule {

    public WeakReferenceStrategyServerCaseModule(DefaultScope testScope) {
        super(testScope);
    }

    @Override
    public void configure(Binder binder){
        super.configure(binder);
        binder.bind(ServerRuntime.class).toProvider(WeakReferenceStrategyServerRuntimeProvider.class).in(testScope);
    }
}
