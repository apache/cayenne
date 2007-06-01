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
package org.objectstyle.cayenne.query;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.objectstyle.cayenne.util.XMLEncoder;
import org.objectstyle.cayenne.util.XMLSerializable;

/**
 * Helper class that holds parameters for classes implementing 
 * {@link GenericSelectQuery} interface.
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
final class SelectExecutionProperties implements XMLSerializable, Serializable {
    int fetchLimit = GenericSelectQuery.FETCH_LIMIT_DEFAULT;
    int pageSize = GenericSelectQuery.PAGE_SIZE_DEFAULT;
    boolean fetchingDataRows = GenericSelectQuery.FETCHING_DATA_ROWS_DEFAULT;
    boolean refreshingObjects = GenericSelectQuery.REFRESHING_OBJECTS_DEFAULT;
    boolean resolvingInherited = GenericSelectQuery.RESOLVING_INHERITED_DEFAULT;
    String cachePolicy = GenericSelectQuery.CACHE_POLICY_DEFAULT;

    /**
     * Copies values of this object to another SelectQueryProperties object.
     */
    void copyToProperties(SelectExecutionProperties anotherProperties) {
        anotherProperties.fetchingDataRows = this.fetchingDataRows;
        anotherProperties.fetchLimit = this.fetchLimit;
        anotherProperties.pageSize = this.pageSize;
        anotherProperties.refreshingObjects = this.refreshingObjects;
        anotherProperties.resolvingInherited = this.resolvingInherited;
        anotherProperties.cachePolicy = this.cachePolicy;
    }

    void initWithProperties(Map properties) {
        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object fetchLimit = properties.get(GenericSelectQuery.FETCH_LIMIT_PROPERTY);
        Object pageSize = properties.get(GenericSelectQuery.PAGE_SIZE_PROPERTY);
        Object refreshingObjects =
            properties.get(GenericSelectQuery.REFRESHING_OBJECTS_PROPERTY);
        Object fetchingDataRows =
            properties.get(GenericSelectQuery.FETCHING_DATA_ROWS_PROPERTY);

        Object resolvingInherited =
            properties.get(GenericSelectQuery.RESOLVING_INHERITED_PROPERTY);
        
        Object cachePolicy = properties.get(GenericSelectQuery.CACHE_POLICY_PROPERTY);

        // init ivars from properties
        this.fetchLimit =
            (fetchLimit != null)
                ? Integer.parseInt(fetchLimit.toString())
                : GenericSelectQuery.FETCH_LIMIT_DEFAULT;

        this.pageSize =
            (pageSize != null)
                ? Integer.parseInt(pageSize.toString())
                : GenericSelectQuery.PAGE_SIZE_DEFAULT;

        this.refreshingObjects =
            (refreshingObjects != null)
                ? "true".equalsIgnoreCase(refreshingObjects.toString())
                : GenericSelectQuery.REFRESHING_OBJECTS_DEFAULT;

        this.fetchingDataRows =
            (fetchingDataRows != null)
                ? "true".equalsIgnoreCase(fetchingDataRows.toString())
                : GenericSelectQuery.FETCHING_DATA_ROWS_DEFAULT;

        this.resolvingInherited = (resolvingInherited != null)
                ? "true".equalsIgnoreCase(resolvingInherited.toString())
                : GenericSelectQuery.RESOLVING_INHERITED_DEFAULT;

        this.cachePolicy = (cachePolicy != null)
                ? cachePolicy.toString()
                : GenericSelectQuery.CACHE_POLICY_DEFAULT;
    }

    public void encodeAsXML(XMLEncoder encoder) {
        if (refreshingObjects != GenericSelectQuery.REFRESHING_OBJECTS_DEFAULT) {
            encoder.printProperty(
                GenericSelectQuery.REFRESHING_OBJECTS_PROPERTY,
                refreshingObjects);
        }

        if (fetchingDataRows != GenericSelectQuery.FETCHING_DATA_ROWS_DEFAULT) {
            encoder.printProperty(
                GenericSelectQuery.FETCHING_DATA_ROWS_PROPERTY,
                fetchingDataRows);
        }

        if (resolvingInherited != GenericSelectQuery.RESOLVING_INHERITED_DEFAULT) {
            encoder.printProperty(
                GenericSelectQuery.RESOLVING_INHERITED_PROPERTY,
                resolvingInherited);
        }

        if (fetchLimit != GenericSelectQuery.FETCH_LIMIT_DEFAULT) {
            encoder.printProperty(GenericSelectQuery.FETCH_LIMIT_PROPERTY, fetchLimit);
        }

        if (pageSize != GenericSelectQuery.PAGE_SIZE_DEFAULT) {
            encoder.printProperty(GenericSelectQuery.PAGE_SIZE_PROPERTY, pageSize);
        }
        
        if (cachePolicy != null && !GenericSelectQuery.CACHE_POLICY_DEFAULT.equals(cachePolicy)) {
            encoder.printProperty(GenericSelectQuery.CACHE_POLICY_PROPERTY, cachePolicy);
        }
    }
    
    String getCachePolicy() {
        return cachePolicy;
    }
    
    void setCachePolicy(String policy) {
        this.cachePolicy = policy;
    }

    boolean isFetchingDataRows() {
        return fetchingDataRows;
    }

    int getFetchLimit() {
        return fetchLimit;
    }

    int getPageSize() {
        return pageSize;
    }

    boolean isRefreshingObjects() {
        return refreshingObjects;
    }

    boolean isResolvingInherited() {
        return resolvingInherited;
    }

    void setFetchingDataRows(boolean b) {
        fetchingDataRows = b;
    }

    void setFetchLimit(int i) {
        fetchLimit = i;
    }

    void setPageSize(int i) {
        pageSize = i;
    }

    void setRefreshingObjects(boolean b) {
        refreshingObjects = b;
    }

    void setResolvingInherited(boolean b) {
        resolvingInherited = b;
    }
}
