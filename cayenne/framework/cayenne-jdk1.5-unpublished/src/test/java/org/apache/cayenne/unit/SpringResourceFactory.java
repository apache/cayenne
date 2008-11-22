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

package org.apache.cayenne.unit;

import java.io.InputStream;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * A bean that loads other Spring beans from the specified location.
 * 
 */
public class SpringResourceFactory implements FactoryBean {

    protected String location;
    protected BeanFactory factory;

    public SpringResourceFactory(String location) {
        this.location = location;
    }

    public Object getObject() throws Exception {
        if (factory == null) {
            InputStream in = Thread
                    .currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(location);
            this.factory = new XmlBeanFactory(new InputStreamResource(in));
        }
        return factory;
    }

    public Class getObjectType() {
        return BeanFactory.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
