//package org.apache.cayenne.rop.server;
//
//import org.apache.cayenne.configuration.server.CayenneServerModuleProvider;
//import org.apache.cayenne.configuration.server.ServerModule;
//import org.apache.cayenne.di.Module;
//
//import java.util.Collection;
//import java.util.Collections;
//
///**
// * Created by Arseni on 01.11.17.
// */
//public class ROPServerModuleProvider implements CayenneServerModuleProvider {
//    @Override
//    public Module module() {
//        return new ROPServerModule();
//    }
//
//    @Override
//    public Class<? extends Module> moduleType() {
//        return ROPServerModule.class;
//    }
//
//    @Override
//    public Collection<Class<? extends Module>> overrides() {
//        Collection modules = Collections.singletonList(ServerModule.class);
//        return modules;
//    }
//}
