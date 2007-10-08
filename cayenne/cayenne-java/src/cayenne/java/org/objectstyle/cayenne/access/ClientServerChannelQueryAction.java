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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.util.GenericResponse;
import org.objectstyle.cayenne.util.IncrementalListResponse;
import org.objectstyle.cayenne.util.ListResponse;
import org.objectstyle.cayenne.util.ObjectDetachOperation;

/**
 * A query handler used by ClientServerChannel.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ClientServerChannelQueryAction {

    static final boolean DONE = true;

    private final ClientServerChannel channel;
    private Query serverQuery;
    private QueryResponse response;
    private final QueryMetadata serverMetadata;
    private final EntityResolver serverResolver;

    ClientServerChannelQueryAction(ClientServerChannel channel, Query query) {
        this.channel = channel;
        this.serverResolver = channel.getServerContext().getEntityResolver();
        this.serverQuery = query;
        this.serverMetadata = serverQuery.getMetaData(serverResolver);
    }

    QueryResponse execute() {

        if (interceptSinglePageQuery() != DONE) {
            runQuery();
        }

        if (interceptIncrementalListConversion() != DONE) {
            interceptObjectConversion();
        }

        return response;
    }

    private boolean interceptSinglePageQuery() {

        // retrieve range from the previously cached list
        if (serverMetadata.getFetchStartIndex() >= 0
                && serverMetadata.getFetchLimit() > 0
                && serverMetadata.getCacheKey() != null) {

            IncrementalFaultList cachedList = channel.getPaginatedResult(serverMetadata
                    .getCacheKey());
            if (cachedList == null) {
                throw new CayenneRuntimeException("No cached list for "
                        + serverMetadata.getCacheKey());
            }

            int startIndex = serverMetadata.getFetchStartIndex();
            int endIndex = startIndex + serverMetadata.getFetchLimit();

            // send back just one page... query sender will figure out where it fits in
            // the incremental list
            this.response = new ListResponse(new ArrayList(cachedList.subList(
                    startIndex,
                    endIndex)));

            return DONE;
        }

        return !DONE;
    }

    private void runQuery() {
        this.response = channel.getServerContext().onQuery(null, serverQuery);
    }

    private boolean interceptIncrementalListConversion() {
        int pageSize = serverMetadata.getPageSize();
        if (pageSize > 0 && serverMetadata.getCacheKey() != null) {

            List list = response.firstList();
            if (list.size() > pageSize && list instanceof IncrementalFaultList) {

                // cache
                channel.addPaginatedResult(
                        serverMetadata.getCacheKey(),
                        ((IncrementalFaultList) list));

                // extract and convert firts page

                List sublist = list.subList(0, pageSize);

                List firstPage = (serverMetadata.isFetchingDataRows()) ? new ArrayList(
                        sublist) : toClientObjects(sublist);

                this.response = new IncrementalListResponse(firstPage, list.size());
                return DONE;
            }
        }

        return !DONE;
    }

    private void interceptObjectConversion() {

        if (!serverMetadata.isFetchingDataRows()) {

            GenericResponse clientResponse = new GenericResponse();

            for (response.reset(); response.next();) {
                if (response.isList()) {
                    List serverObjects = response.currentList();
                    clientResponse.addResultList(toClientObjects(serverObjects));

                }
                else {
                    clientResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }

            this.response = clientResponse;
        }
    }

    private List toClientObjects(List serverObjects) {

        // create a list copy even if it is empty to ensure that we have a
        // clean serializable list...
        List clientObjects = new ArrayList(serverObjects.size());

        if (!serverObjects.isEmpty()) {
            ObjectDetachOperation op = new ObjectDetachOperation(serverResolver
                    .getClientEntityResolver());
            Iterator it = serverObjects.iterator();
            PrefetchTreeNode prefetchTree = serverMetadata.getPrefetchTree();

            while (it.hasNext()) {
                Persistent object = (Persistent) it.next();
                ObjectId id = object.getObjectId();

                // sanity check
                if (id == null) {
                    throw new CayenneRuntimeException(
                            "Server returned an object without an id: " + object);
                }

                // have to resolve descriptor here for every object, as
                // often a query will not have any info indicating the
                // entity type
                ClassDescriptor serverDescriptor = serverResolver.getClassDescriptor(id
                        .getEntityName());

                clientObjects.add(op.detach(object, serverDescriptor, prefetchTree));
            }
        }

        return clientObjects;
    }

}
