package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ObjEntityHandler extends NamespaceAwareNestedTagHandler {

    private static final String OBJENTITY_TAG = "objEntity";
    private static final String OBJENTITY_NAME_TAG = "name";

    private ClassGenerationAction configuration;

    ObjEntityHandler(NamespaceAwareNestedTagHandler parentHandler, ClassGenerationAction configuration) {
        super(parentHandler);
        this.configuration = configuration;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case OBJENTITY_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case OBJENTITY_NAME_TAG:
                createObjEntity(data);
                break;
        }
    }

    private void createObjEntity(String data) {
        if(data.trim().length() == 0) {
            return;
        }

        if(configuration != null) {
            configuration.loadEntity(data);
        }
    }
}
