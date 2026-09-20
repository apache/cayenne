/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.docs;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.query.ProcedureCall;

import java.util.List;

/**
 * Examples that are only compiled, but not run, as there are no stored procedures in the docs test schema.
 */
public class ProcedureCallExamples {

    private ObjectContext context;

    public void select() {
        // tag::select[]
        List<Artist> result = ProcedureCall.query("my_procedure", Artist.class)
                .param("p1", "abc")
                .param("p2", 3000)
                .select(context);
        // end::select[]
    }

    public void call() {
        // tag::call[]
        // here we do not bother with root class.
        // Procedure name gives us needed routing information
        List<QueryResult> result = ProcedureCall.query("my_procedure")
                .param("p1", "abc")
                .param("p2", 3000)
                .call(context);
        // end::call[]
    }

    public void outParameters() {
        // tag::outParameters[]
        for (QueryResult item : ProcedureCall.query("my_procedure").call(context)) {
            switch (item) {
                case QueryResult.Select<?> select -> process(select.objects());
                case QueryResult.Update update -> process(update.counts());
                case QueryResult.OutParameters out -> process(out.values().get("out_param"));
                default -> {
                }
            }
        }
        // end::outParameters[]
    }

    private void process(Object result) {
    }
}
