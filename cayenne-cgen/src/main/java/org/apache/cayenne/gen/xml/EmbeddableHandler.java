package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EmbeddableHandler extends NamespaceAwareNestedTagHandler {

    private static final String EMBEDDABLE_TAG = "embeddable";
    private static final String EMBEDDABLE_NAME_TAG = "name";

    private ClassGenerationAction configuration;

    EmbeddableHandler(NamespaceAwareNestedTagHandler parentHandler, ClassGenerationAction configuration) {
        super(parentHandler);
        this.configuration = configuration;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case EMBEDDABLE_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case EMBEDDABLE_NAME_TAG:
                createEmbeddableEntity(data);
                break;
        }
    }

    private void createEmbeddableEntity(String data) {
        if(data.trim().length() == 0) {
            return;
        }

        if(configuration != null) {
            configuration.loadEmbeddable(data);
        }
    }
}

