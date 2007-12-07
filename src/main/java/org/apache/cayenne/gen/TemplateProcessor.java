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

import java.io.Writer;
import java.util.Properties;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogSystem;

/**
 * Encapsulates a class generation template that can be used repeatedly to generate a set
 * of classes.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
// This class is used for version 1.2 of the class generation. Backwards compatibility (v
// 1.1) is implemented via subclasses.
public class TemplateProcessor {

    protected Template classTemplate;
    protected Context velocityContext;

    public TemplateProcessor(String template, Context velocityContext) throws Exception {

        if (velocityContext != null) {
            this.velocityContext = velocityContext;
        }
        else {
            this.velocityContext = new VelocityContext();
        }

        initializeClassTemplate(template);
    }

    /**
     * Sets up VelocityEngine properties, creates a VelocityEngine instance, and fetches a
     * template using the VelocityEngine instance.
     */
    protected void initializeClassTemplate(String template)
            throws CayenneRuntimeException {

        Properties props = new Properties();

        // null logger that will prevent velocity.log from being generated
        props.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogSystem.class
                .getName());
        props.put("resource.loader", "cayenne");
        props.put("cayenne.resource.loader.class", ClassGeneratorResourceLoader.class
                .getName());

        VelocityEngine velocityEngine = new VelocityEngine();
        try {
            velocityEngine.init(props);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't initialize Velocity", ex);
        }

        try {
            classTemplate = velocityEngine.getTemplate(template);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Can't create template: " + template, ex);
        }
    }

    /**
     * Generates code for provided context arguments, writing output to the provided
     * Writer.
     */
    public void generateClass(
            Writer out,
            DataMap dataMap,
            ObjEntity entity,
            String fqnBaseClass,
            String fqnSuperClass,
            String fqnSubClass) throws Exception {

        if (dataMap == null) {
            throw new IllegalStateException("DataMap is null.");
        }

        velocityContext.put("objEntity", entity);
        velocityContext.put("stringUtils", StringUtils.getInstance());
        velocityContext.put("entityUtils", new EntityUtils(
                dataMap,
                entity,
                fqnBaseClass,
                fqnSuperClass,
                fqnSubClass));
        velocityContext.put("importUtils", new ImportUtils());

        classTemplate.merge(velocityContext, out);
    }

    public void setClassTemplate(Template classTemplate) {
        this.classTemplate = classTemplate;
    }

    public void setVelocityContext(Context velocityContext) {
        this.velocityContext = velocityContext;
    }
}
