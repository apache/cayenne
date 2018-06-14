package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

public class CgenSaverDelegate extends BaseSaverDelegate{

    private DataChannelMetaData metaData;

    CgenSaverDelegate(DataChannelMetaData metaData){
        this.metaData = metaData;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {

        return null;
    }
}
