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

package org.apache.cayenne.configuration.event;

import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.map.event.MapEvent;;

/**
 * @since 4.0
 */
public class TemplateEvent extends MapEvent {
    protected ClassTemplate template;

    /** Creates a Template change event. */
    public TemplateEvent(Object src, ClassTemplate template) {
        super(src);
        setTemplate(template);
    }

    /** Creates a Template event of a specified type. */
    public TemplateEvent(Object src, ClassTemplate template, int id) {
        this(src, template);
        setId(id);
    }

    /** Creates a Template name change event.*/
    public TemplateEvent(Object src, ClassTemplate template, String oldName) {
        this(src, template);
        setOldName(oldName);
    }

    /** Returns template object associated with this event. */
    public ClassTemplate getTemplate() {
        return template;
    }

    /**
     * Sets the template.
     *
     * @param template The template to set
     */
    public void setTemplate(ClassTemplate template) {
        this.template = template;
    }

    @Override
    public String getNewName() {
        return (template != null) ? template.getName() : null;
    }
}
