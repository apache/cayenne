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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.IncrementalListResponse;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.ObjectDetachOperation;

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
        this.serverResolver = channel.getEntityResolver();
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

            List cachedList = channel.getQueryCache().get(serverMetadata);
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
        this.response = channel.getParentChannel().onQuery(null, serverQuery);
    }

    private boolean interceptIncrementalListConversion() {
        int pageSize = serverMetadata.getPageSize();
        if (pageSize > 0 && serverMetadata.getCacheKey() != null) {

            List list = response.firstList();
            if (list.size() > pageSize && list instanceof IncrementalFaultList) {

                // cache
                channel.getQueryCache().put(serverMetadata, list);

                // extract and convert first page

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
