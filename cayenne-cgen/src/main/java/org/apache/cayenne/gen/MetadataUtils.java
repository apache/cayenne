package org.apache.cayenne.gen;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.project.extension.info.ObjectInfo;

public class MetadataUtils {

    @Inject
    private DataChannelMetaData metaData;

    public String getComment(ConfigurationNode node) {
        return getInfo(node, ObjectInfo.COMMENT);
    }

    public String getInfo(ConfigurationNode node, String key) {
        return ObjectInfo.getFromMetaData(metaData, node, key);
    }
}
