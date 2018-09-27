package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.UnitDbAdapter;

import java.util.ArrayList;
import java.util.Collection;

public class WeakReferenceStrategyServerRuntimeProvider extends ServerRuntimeProvider {

    public WeakReferenceStrategyServerRuntimeProvider(@Inject ServerCaseDataSourceFactory dataSourceFactory,
                                                      @Inject ServerCaseProperties properties,
                                                      @Inject Provider<DbAdapter> dbAdapterProvider,
                                                      @Inject UnitDbAdapter unitDbAdapter) {
        super(dataSourceFactory, properties, dbAdapterProvider, unitDbAdapter);
    }

    @Override
    protected Collection<? extends Module> getExtraModules() {
        Collection<Module> modules = new ArrayList<>();
        modules.addAll(super.getExtraModules());
        modules.add(new WeakReferenceStrategyModule());
        return modules;
    }
}
