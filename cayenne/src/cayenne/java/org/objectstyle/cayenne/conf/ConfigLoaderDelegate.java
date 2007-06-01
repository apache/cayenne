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

import java.util.List;
import java.util.Map;

/**
 * Interface that defines callback API used by ConfigLoader to process loaded
 * configuration. Main responsibility of ConfigLoaderDelegate is to create
 * objects, while ConfigLoader is mainly concerned with XML parsing. 
 * 
 * @author Andrei Adamchik
 */
public interface ConfigLoaderDelegate {
    /**
     * Callback methods invoked in the beginning of the configuration
     * processing.
     */
    public void startedLoading();

    /**
     * Callback methods invoked at the end of the configuration processing.
     */
    public void finishedLoading();

    /**
     * Callback method invoked when a project version is read.
     * @since 1.1
     */
    public void shouldLoadProjectVersion(String version);
        
    /**
     * Callback method invoked when a domain is encountered in the configuration
     * file.
     * @param name domain name.
     */
    public void shouldLoadDataDomain(String name);
    
    /**
     * Callback method invoked when a DataView reference is encountered in the configuration
     * file.
     * 
     * @since 1.1
     */
    public void shouldRegisterDataView(String name, String location);

    /**
     * @deprecated Since 1.1 this method is no longer called during project loading.
     * {@link #shouldLoadDataMaps(String,Map)} is used instead.
     */
    public void shouldLoadDataMap(
        String domainName,
        String mapName,
        String location,
        List depMapNames);
    
    /**
     * @deprecated Since 1.1 this method is no longer called during project loading.
     * {@link #shouldLoadDataMaps(String,Map)} is used instead.
     */
    public void shouldLoadDataMaps(String domainName, Map locations, Map dependencies);
    
    /**
     * @since 1.1
     */
    public void shouldLoadDataMaps(String domainName, Map locations);
    
    /**
     * @since 1.1
     */
    public void shouldLoadDataDomainProperties(String domainName, Map properties);

    public void shouldLoadDataNode(
        String domainName,
        String nodeName,
        String dataSource,
        String adapter,
        String factory);

    public void shouldLinkDataMap(
        String domainName,
        String nodeName,
        String mapName);

    /**
     * Gives delegate an opportunity to process the error.
     * 
     * @param th
     * @return boolean indicating whether ConfigLoader should proceed with
     * further processing. Ultimately it is up to the ConfigLoader to make this
     * decision.
     */
    public boolean loadError(Throwable th);

    /**
     * @return status object indicating the state of the configuration loading.
     */
    public ConfigStatus getStatus();
}
