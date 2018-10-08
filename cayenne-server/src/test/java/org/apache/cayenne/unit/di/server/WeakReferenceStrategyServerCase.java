package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultScope;

public class WeakReferenceStrategyServerCase extends ServerCase {
    private static final Injector injector;

    static {
        DefaultScope testScope = new DefaultScope();
        injector = DIBootstrap.createInjector(new WeakReferenceStrategyServerCaseModule(testScope));
        injector.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    @Override
    protected Injector getUnitTestInjector() {
        return injector;
    }
}
