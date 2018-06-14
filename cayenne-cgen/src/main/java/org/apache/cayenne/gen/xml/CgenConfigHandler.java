package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.dbsync.xml.DbImportExtension;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CgenConfigHandler extends NamespaceAwareNestedTagHandler{

    private DataChannelMetaData metaData;

    CgenConfigHandler(NamespaceAwareNestedTagHandler parentHandler, DataChannelMetaData metaData) {
        super(parentHandler);
        this.metaData = metaData;
        this.targetNamespace = DbImportExtension.NAMESPACE;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return false;
    }
}
