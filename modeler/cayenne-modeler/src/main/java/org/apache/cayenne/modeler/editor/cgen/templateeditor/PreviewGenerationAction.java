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

package org.apache.cayenne.modeler.editor.cgen.templateeditor;


import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.TemplateType;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Used for generating class preview in template editor
 * @since 5.0
 */
public class PreviewGenerationAction extends ClassGenerationAction {

    private StringWriter writer;

    public PreviewGenerationAction(CgenConfiguration cgenConfig) {
        super(cgenConfig);
    }

    @Override
    protected void validateAttributes() {
        //Mock
    }

    public void setWriter(StringWriter writer) {
        this.writer = writer;
    }

    @Override
    protected Writer openWriter(TemplateType templateType) {
        // clear and return
        writer.getBuffer().setLength(0);
        return writer;
    }
}
