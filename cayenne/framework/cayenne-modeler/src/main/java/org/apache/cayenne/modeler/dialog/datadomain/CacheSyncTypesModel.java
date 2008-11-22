
/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.dialog.datadomain;

import org.apache.cayenne.util.Util;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 */
public class CacheSyncTypesModel extends BasicModel {
    public static final String JGROUPS_FACTORY_LABEL = "JavaGroups Multicast (Default)";
    public static final String JMS_FACTORY_LABEL = "JMS Transport";
    public static final String CUSTOM_FACTORY_LABEL = "Custom Transport";

    public static final Object[] NOTIFICATION_TYPES =
        new Object[] { JGROUPS_FACTORY_LABEL, JMS_FACTORY_LABEL, CUSTOM_FACTORY_LABEL };

    public static final Selector NOTIFICATION_TYPES_SELECTOR =
        Selector.fromString("notificationTypes");
    public static final Selector FACTORY_LABEL_SELECTOR =
        Selector.fromString("factoryLabel");

    protected String factoryLabel;

    public Object[] getNotificationTypes() {
        return NOTIFICATION_TYPES;
    }

    public String getFactoryLabel() {
        return factoryLabel;
    }

    public void setFactoryLabel(String factoryLabel) {
        if (!Util.nullSafeEquals(this.factoryLabel, factoryLabel)) {
            this.factoryLabel = factoryLabel;
            fireModelChange(VALUE_CHANGED, FACTORY_LABEL_SELECTOR);
        }
    }
}
