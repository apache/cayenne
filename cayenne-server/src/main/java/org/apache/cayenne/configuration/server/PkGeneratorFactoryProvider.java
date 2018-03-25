package org.apache.cayenne.configuration.server;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.dba.PerAdapterProvider;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Inject;

import java.util.Map;

public class PkGeneratorFactoryProvider extends PerAdapterProvider<PkGenerator> {

    public PkGeneratorFactoryProvider(
            @Inject(Constants.SERVER_PK_GENERATORS_MAP) Map<String, PkGenerator> perAdapterValues,
            @Inject PkGenerator defaultValue) {
        super(perAdapterValues, defaultValue);
    }
}
