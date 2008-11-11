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

package org.apache.cayenne.conn;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;


/**
 * <p>Basic JNDI object factory that creates an instance of
 * <code>PoolManager</code> that has been configured based on the
 * <code>RefAddr</code> values of the specified <code>Reference</code>.</p>
 *
 * <p>Here is a sample Tomcat 4.x configuration that sets this class
 * as a default factory for javax.sql.DataSource objects:</p>
<code><pre>
&lt;ResourceParams name="jdbc/mydb"&gt;
    &lt;parameter&gt;
        &lt;name&gt;factory&lt;/name&gt;
        &lt;value>org.apache.cayenne.conn.ContainerPoolFactory&lt;/value&gt;
    &lt;/parameter&gt;

    &lt;parameter>
        &lt;name>username&lt;/name>
        &lt;value>andrei&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>password&lt;/name>
        &lt;value>bla-bla&lt;/value>
    &lt;/parameter>
                
    &lt;parameter>
        &lt;name>driver&lt;/name>
        &lt;value>org.gjt.mm.mysql.Driver&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>url&lt;/name>
        &lt;value>jdbc:mysql://noise/cayenne&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>min&lt;/name>
        &lt;value>1&lt;/value>
    &lt;/parameter>
            
    &lt;parameter>
        &lt;name>max&lt;/name>
        &lt;value>3&lt;/value>
    &lt;/parameter>
&lt;/ResourceParams>
</pre></code>
 *
 * <p>After ContainerPoolFactory was configured to be used within the container 
 * (see above for Tomcat example), you can reference your "jdbc/mydb" DataSource in
 * web application deployment descriptor like that (per Servlet Specification): </p>
 *<code><pre>
&lt;resource-ref>
    &lt;es-ref-name>jdbc/mydb&lt;/res-ref-name>
    &lt;res-type>javax.sql.DataSource&lt;/res-type>
    &lt;res-auth>Container&lt;/res-auth>
&lt;/resource-ref>
</pre></code> 
 *
 */
public class ContainerPoolFactory implements ObjectFactory {

    /**
     * <p>Creates and returns a new <code>PoolManager</code> instance.  If no
     * instance can be created, returns <code>null</code> instead.</p>
     *
     * @param obj The possibly null object containing location or
     *  reference information that can be used in creating an object
     * @param name The name of this object relative to <code>nameCtx</code>
     * @param nameCtx The context relative to which the <code>name</code>
     *  parameter is specified, or <code>null</code> if <code>name</code>
     *  is relative to the default initial context
     * @param environment The possibly null environment that is used in
     *  creating this object
     *
     * @exception Exception if an exception occurs creating the instance
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment)
    throws Exception {
        // We only know how to deal with <code>javax.naming.Reference</code>s
        // that specify a class name of "javax.sql.DataSource"
        if ((obj == null) || !(obj instanceof Reference)) {
            return null;
        }

        Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName())) {
            return null;
        }

        // Create and configure a PoolManager instance based on the
        // RefAddr values associated with this Reference
        RefAddr ra = null;
        String driver = null;
        String url = null;
        int min = 1;
        int max = 1;
        String username = null;
        String password = null;
        
        ra = ref.get("min");
        if (ra != null) {
            min = Integer.parseInt(ra.getContent().toString());
        }
        
        ra = ref.get("max");
        if (ra != null) {
            max = Integer.parseInt(ra.getContent().toString());
        }


        ra = ref.get("driver");
        if (ra != null) {
            driver = ra.getContent().toString();
        }


        ra = ref.get("password");
        if (ra != null) {
            password = ra.getContent().toString();
        }

        ra = ref.get("url");
        if (ra != null) {
            url = ra.getContent().toString();
        }

        ra = ref.get("username");
        if (ra != null) {
            username = ra.getContent().toString();
        }

        return new PoolManager(driver, url, min, max, username, password);
    }
}

