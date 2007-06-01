/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;

/**
 * Save delegate used for saving Cayenne access stack.
 * 
 * @author Andrei Adamchik
 */
public class RuntimeSaveDelegate implements ConfigSaverDelegate {
    protected Configuration config;

    public RuntimeSaveDelegate(Configuration config) {
        this.config = config;
    }

    /**
     * Constructor for RuntimeSaveDelegate.
     */
    public RuntimeSaveDelegate() {
        super();
    }

    protected DataDomain findDomain(String domainName) {
        DataDomain domain = config.getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Can't find DataDomain: " + domainName);
        }

        return domain;
    }

    protected DataNode findNode(String domainName, String nodeName) {
        DataDomain domain = findDomain(domainName);
        DataNode node = domain.getNode(nodeName);
        if (node == null) {
            throw new IllegalArgumentException(
                "Can't find DataNode: " + domainName + "." + nodeName);
        }

        return node;
    }
    
    /**
     * @since 1.1
     */
    public String projectVersion() {
        return config.getProjectVersion();
    }

    /**
     * @deprecated since 1.1, since dependencies are no longer tracked explicitly.
     */
    public Iterator dependentMapNames(String domainName, String mapName) {
        Transformer tr = new Transformer() {
            public Object transform(Object input) {
                return ((DataMap) input).getName();
            }
        };
        List deps = findDomain(domainName).getMap(mapName).getDependencies();
        return new TransformIterator(deps.iterator(), tr);
    }

    public Iterator domainNames() {
        Transformer tr = new Transformer() {
            public Object transform(Object input) {
                return ((DataDomain) input).getName();
            }
        };
        return new TransformIterator(config.getDomains().iterator(), tr);
    }
    
    /**
     * @since 1.1
     */
    public Iterator viewNames() {
        return config.getDataViewLocations().keySet().iterator();
    }

    /**
     * @since 1.1
     */
    public String viewLocation(String dataViewName) {
        return (String) config.getDataViewLocations().get(dataViewName);
    }

    public Iterator propertyNames(String domainName) {
        return findDomain(domainName).getProperties().keySet().iterator();
    }

    public String propertyValue(String domainName, String propertyName) {
        return (String) findDomain(domainName).getProperties().get(propertyName);
    }

    public String mapLocation(String domainName, String mapName) {
        return findDomain(domainName).getMap(mapName).getLocation();
    }

    public Iterator mapNames(String domainName) {
        Transformer tr = new Transformer() {
            public Object transform(Object input) {
                return ((DataMap) input).getName();
            }
        };

        List maps = new ArrayList(findDomain(domainName).getDataMaps());
        return new TransformIterator(maps.iterator(), tr);
    }

    public String nodeAdapterName(String domainName, String nodeName) {
        DbAdapter adapter = findNode(domainName, nodeName).getAdapter();
        return (adapter != null) ? adapter.getClass().getName() : null;
    }

    public String nodeDataSourceName(String domainName, String nodeName) {
        return findNode(domainName, nodeName).getDataSourceLocation();
    }

    public String nodeFactoryName(String domainName, String nodeName) {
        return findNode(domainName, nodeName).getDataSourceFactory();
    }

    public Iterator nodeNames(String domainName) {
        Transformer tr = new Transformer() {
            public Object transform(Object input) {
                return ((DataNode) input).getName();
            }
        };

        Collection nodes = findDomain(domainName).getDataNodes();
        return new TransformIterator(nodes.iterator(), tr);
    }

    public Iterator linkedMapNames(String domainName, String nodeName) {
        Transformer tr = new Transformer() {
            public Object transform(Object input) {
                return ((DataMap) input).getName();
            }
        };

        Collection maps = findNode(domainName, nodeName).getDataMaps();
        return new TransformIterator(maps.iterator(), tr);
    }
}
