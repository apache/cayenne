/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.unit;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.ParameterizedQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryChain;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * DataSetFactory that loads DataSets from XML using Spring.
 * 
 * @author Andrei Adamchik
 */
public class XMLDataSetFactory implements DataSetFactory {

    private static Logger logObj = Logger.getLogger(XMLDataSetFactory.class);

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
            logObj.error("DataSet resource not found: " + testCase.getName());
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
            // strip "org.objectstyle.cayenne"
            if (name.startsWith("org.objectstyle.cayenne.")) {
                name = name.substring("org.objectstyle.cayenne.".length());
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
