package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.extension.LoaderDelegate;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.extension.SaverDelegate;

public class CgenExtension implements ProjectExtension {

    public static final String NAMESPACE = "http://cayenne.apache.org/schema/" + Project.VERSION + "/cgen";

    @Inject
    private DataChannelMetaData metaData;

    @Override
    public LoaderDelegate createLoaderDelegate() {
        return null;
    }

    @Override
    public SaverDelegate createSaverDelegate() {
        return new CgenSaverDelegate(metaData);
    }

    @Override
    public ConfigurationNodeVisitor<String> createNamingDelegate() {
        return null;
    }
}
