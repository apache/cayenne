package org.apache.cayenne.configuration.web;

import org.apache.cayenne.configuration.server.CayenneServerModuleProvider;
import org.apache.cayenne.di.Module;

import java.util.Collection;
import java.util.Collections;

public class WebServerModuleProvider implements CayenneServerModuleProvider{

    @Override
    public Module module() {
        return new WebModule();
    }

    @Override
    public Class<? extends Module> moduleType() {
        return WebModule.class;
    }

    @Override
    public Collection<Class<? extends Module>> overrides() {
        return Collections.emptyList();
    }
}