package org.apache.cayenne.project;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class DeletableNodesGetter extends
        BaseConfigurationNodeVisitor<Collection<ConfigurationNode>> {

    @Override
    public Collection<ConfigurationNode> visitDataChannelDescriptor(
            DataChannelDescriptor descriptor) {

        Collection<ConfigurationNode> nodes = new ArrayList<ConfigurationNode>();
        Collection<DataMap> deletedDataMaps = descriptor.getDeletedDataMaps();

        for (DataMap map : deletedDataMaps) {
                nodes.add(map);
        }
        deletedDataMaps.clear();

        return nodes;
    }

    @Override
    public Collection<ConfigurationNode> visitDataMap(DataMap dataMap) {
        return Collections.<ConfigurationNode> singletonList(dataMap);
    }
}
