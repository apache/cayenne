package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;

import org.apache.cayenne.project.extension.LoaderDelegate;

public class CgenLoaderDelegate implements LoaderDelegate {

    private DataChannelMetaData metaData;

    CgenLoaderDelegate(DataChannelMetaData metaData){
        this.metaData = metaData;
    }

    @Override
    public String getTargetNamespace() {
        return CgenExtension.NAMESPACE;
    }

    @Override
    public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent, String tag) {
//        if(CgenConfigHandler.CONFIG_TAG.equals(tag)) {
//            return new CgenConfigHandler(parent, metaData);
//        }
        return null;
    }
}
