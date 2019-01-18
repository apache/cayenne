package org.apache.cayenne.unit.di.server;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.UnitDbAdapter;

public class WeakReferenceStrategyServerRuntimeProvider extends ServerRuntimeProvider {

    public WeakReferenceStrategyServerRuntimeProvider(@Inject ServerCaseDataSourceFactory dataSourceFactory,
                                                      @Inject ServerCaseProperties properties,
                                                      @Inject Provider<DbAdapter> dbAdapterProvider,
                                                      @Inject UnitDbAdapter unitDbAdapter) {
        super(dataSourceFactory, properties, dbAdapterProvider, unitDbAdapter);
    }

    @Override
    protected Collection<? extends Module> getExtraModules() {
        Collection<Module> modules = new ArrayList<>(super.getExtraModules());
        modules.add(new WeakReferenceStrategyModule());
        return modules;
    }
}
