/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;

import java.util.ArrayList;
import java.util.List;

class EmbeddableAttributeDisplayEventType extends EmbeddableDisplayEventType {

    public EmbeddableAttributeDisplayEventType(ProjectController controller) {
        super(controller);
    }

    @Override
    public void fireLastDisplayEvent() {
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
        if (!dataChannel.getName().equals(preferences.getDomain())) {
            return;
        }

        DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
        if (dataMap == null) {
            return;
        }

        Embeddable embeddable = dataMap.getEmbeddable(preferences.getEmbeddable());
        if (embeddable == null) {
            return;
        }

        EmbeddableDisplayEvent embeddableDisplayEvent = new EmbeddableDisplayEvent(this, embeddable, dataMap, dataChannel);
        controller.fireEmbeddableDisplayEvent(embeddableDisplayEvent);

        EmbeddableAttribute[] embeddableAttributes = getLastEmbeddableAttributes(embeddable);
        EmbeddableAttributeDisplayEvent attributeDisplayEvent = new EmbeddableAttributeDisplayEvent(this, embeddable, embeddableAttributes, dataMap, dataChannel);
        controller.fireEmbeddableAttributeDisplayEvent(attributeDisplayEvent);
    }

    @Override
    public void saveLastDisplayEvent() {
        preferences.setEvent(EmbeddableAttributeDisplayEvent.class.getSimpleName());
        preferences.setDomain(controller.getCurrentDataChanel().getName());
        preferences.setDataMap(controller.getCurrentDataMap().getName());
        preferences.setEmbeddable(controller.getCurrentEmbeddable().getClassName());

        EmbeddableAttribute[] currentEmbAttributes = controller.getCurrentEmbAttributes();
        if (currentEmbAttributes == null) {
            preferences.setEmbAttrs("");
        } else {
            StringBuilder sb = new StringBuilder();
            for (EmbeddableAttribute embeddableAttribute : currentEmbAttributes) {
                sb.append(embeddableAttribute.getName()).append(",");
            }
            preferences.setEmbAttrs(sb.toString());
        }
    }

    protected EmbeddableAttribute[] getLastEmbeddableAttributes(Embeddable embeddable) {
        List<EmbeddableAttribute> embeddableAttributeList = new ArrayList<EmbeddableAttribute>();
        EmbeddableAttribute[] attributes = new EmbeddableAttribute[0];

        String embAttrs = preferences.getEmbAttrs();
        if (embAttrs.isEmpty()) {
            return embeddableAttributeList.toArray(attributes);
        }

        for (String embAttrName : embAttrs.split(",")) {
            embeddableAttributeList.add(embeddable.getAttribute(embAttrName));
        }

        return embeddableAttributeList.toArray(attributes);
    }
}