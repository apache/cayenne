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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.ParameterizedQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryChain;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * DataSetFactory that loads DataSets from XML using Spring.
 * 
 */
public class XMLDataSetFactory implements DataSetFactory {

   

    protected String location;
    protected Map dataSets;

    public XMLDataSetFactory() {
        this.dataSets = new HashMap();
    }

    /**
     * Returns a Collection of Cayenne queries for a given test.
     */
    public Query getDataSetQuery(Class testCase, String testName, Map parameters) {
        // use test case class name as a key to locate BeanFactory
        // use test name to locate DataSet
        BeanFactory factory = getFactory(testCase);

        if (factory == null) {
            return null;
        }

        // name is either a Collection or an individual query
        Object object = factory.getBean(testName);
        if (object == null) {
            throw new RuntimeException("No query exists for test name:" + testName);
        }

        QueryChain chain = new QueryChain();

        if (object instanceof Collection) {
            Iterator it = ((Collection) object).iterator();
            while (it.hasNext()) {
                chain.addQuery(processQuery((Query) it.next(), parameters));
            }
        }
        else if (object instanceof Query) {
            chain.addQuery(processQuery((Query) object, parameters));
        }
        else {
            throw new RuntimeException("Invalid object type for name '"
                    + testName
                    + "': "
                    + object.getClass().getName());
        }

        return chain;
    }

    protected Query processQuery(Query query, Map parameters) {
        if (parameters == null) {
            return query;
        }

        return (query instanceof ParameterizedQuery) ? ((ParameterizedQuery) query)
                .createQuery(parameters) : query;
    }

    protected BeanFactory getFactory(Class testCase) {

        if (testCase == null) {
            throw new CayenneRuntimeException("Null test case");
        }

        // lookup BeanFactory in the class hierarchy...
        BeanFactory factory = null;
        Class aClass = testCase;
        while (factory == null && aClass != null) {
            factory = loadForClass(aClass);
            aClass = aClass.getSuperclass();
        }

        if (factory == null) {
            throw new CayenneRuntimeException("DataSet resource not found: "
                    + testCase.getName());
        }

        return factory;
    }

    protected BeanFactory loadForClass(Class testCase) {
        BeanFactory factory = (BeanFactory) dataSets.get(testCase);

        if (factory == null) {
            StringBuffer resourceName = new StringBuffer();

            if (location != null) {
                resourceName.append(location);
                if (!location.endsWith("/")) {
                    resourceName.append("/");
                }
            }

            String name = testCase.getName();
            // strip "org.apache.cayenne"
            if (name.startsWith("org.apache.cayenne.")) {
                name = name.substring("org.apache.cayenne.".length());
            }

            resourceName.append(name).append(".xml");

            InputStream in = Thread
                    .currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(resourceName.toString());

            if (in == null) {
                return null;
            }

            factory = new XmlBeanFactory(new InputStreamResource(in));
            dataSets.put(testCase, factory);
        }

        return factory;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
