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
package org.apache.cayenne.modeler.event;

import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;

/**
 * @since 4.0
 */
public class TemplateDisplayEvent extends DataMapDisplayEvent {
    protected ClassTemplate template;
    protected boolean templateChanged = true;

    /**
     * Creates a new TemplateDisplayEvent
     */
    public TemplateDisplayEvent(Object src, ClassTemplate template, DataMap map, DataChannelDescriptor domain) {
        super(src, map, domain);
        this.template = template;
    }

    public ClassTemplate getTemplate() {
        return template;
    }

    public void setTemplate(ClassTemplate template) {
        this.template = template;
    }

    public boolean isTemplateChanged() {
        return templateChanged;
    }

    public void setTemplateChanged(boolean b) {
        templateChanged = b;
    }

}
