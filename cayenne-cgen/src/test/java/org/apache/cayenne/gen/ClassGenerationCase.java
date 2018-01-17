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

package org.apache.cayenne.gen;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.Before;

import java.io.StringWriter;
import java.util.Properties;

public class ClassGenerationCase {

    private VelocityEngine velocityEngine;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();

        props.put("resource.loader", "cayenne");
        props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class.getName());
        props.put("cayenne.resource.loader.cache", "false");

        this.velocityEngine = new VelocityEngine();
        this.velocityEngine.init(props);
    }

    protected String renderTemplate(String templateName, Context context) throws Exception {
        StringWriter writer = new StringWriter();

        Template template = velocityEngine.getTemplate(templateName);
        template.merge(context, writer);

        return writer.toString();
    }

}
