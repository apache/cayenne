package org.apache.cayenne.gen;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.gen.xml.CgenExtension;
import org.apache.cayenne.project.ProjectModule;

public class CgenModule implements Module{
    @Override
    public void configure(Binder binder) {
        ProjectModule.contributeExtensions(binder).add(CgenExtension.class);
    }
}
