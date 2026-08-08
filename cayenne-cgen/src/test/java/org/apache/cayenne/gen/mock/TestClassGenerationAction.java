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
package org.apache.cayenne.gen.mock;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.MetadataUtils;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.gen.ToolsUtilsFactory;
import org.slf4j.Logger;

public class TestClassGenerationAction extends ClassGenerationAction {

    private final Collection<StringWriter> writers;

    public TestClassGenerationAction(CgenConfiguration configuration, ToolsUtilsFactory utilsFactory,
                                     MetadataUtils metadataUtils, Logger logger, Collection<StringWriter> writers) {
        super(configuration, utilsFactory, metadataUtils, logger);
        this.writers = writers;
    }

    @Override
    protected Writer openWriter(TemplateType templateType) throws Exception {
        StringWriter writer = new StringWriter();
        writers.add(writer);
        return writer;
    }
}
