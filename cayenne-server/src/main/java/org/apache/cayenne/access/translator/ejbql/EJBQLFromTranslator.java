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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLFromItem;
import org.apache.cayenne.ejbql.parser.EJBQLInnerFetchJoin;
import org.apache.cayenne.ejbql.parser.EJBQLJoin;
import org.apache.cayenne.ejbql.parser.EJBQLOuterFetchJoin;

/**
 * @since 3.0
 */
public class EJBQLFromTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;
    private String lastId;
    private EJBQLJoinAppender joinAppender;

    public EJBQLFromTranslator(EJBQLTranslationContext context) {
        super(true);
        this.context = context;
        this.joinAppender = context.getTranslatorFactory().getJoinAppender(context);
    }

    @Override
    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            if (lastId != null) {
                context.markCurrentPosition(EJBQLJoinAppender.makeJoinTailMarker(lastId));
            }
        }

        return true;
    }

    @Override
    public boolean visitFromItem(EJBQLFromItem expression, int finishedChildIndex) {

        String id = expression.getId();
        
        if (lastId != null) {
            context.append(',');
            context.markCurrentPosition(EJBQLJoinAppender.makeJoinTailMarker(lastId));
        }

        this.lastId = id;
        joinAppender.appendTable(new EJBQLTableId(id));
        return false;
    }

    @Override
    public boolean visitInnerFetchJoin(EJBQLJoin join) {
        joinAppender.appendInnerJoin(
                null,
                new EJBQLTableId(join.getLeftHandSideId()),
                new EJBQLTableId(((EJBQLInnerFetchJoin) join).getRightHandSideId()));

        context.markCurrentPosition(EJBQLJoinAppender
                .makeJoinTailMarker(((EJBQLInnerFetchJoin) join).getRightHandSideId()));
        return false;
    }

    @Override
    public boolean visitInnerJoin(EJBQLJoin join) {
        joinAppender.appendInnerJoin(
                null,
                new EJBQLTableId(join.getLeftHandSideId()),
                new EJBQLTableId(join.getRightHandSideId()));
        
        //fix 1341-mark current join position for probable future joins to this join
        context.markCurrentPosition(EJBQLJoinAppender.makeJoinTailMarker(join.getRightHandSideId()));
        return false;
    }

    @Override
    public boolean visitOuterFetchJoin(EJBQLJoin join) {
        joinAppender.appendOuterJoin(
                null,
                new EJBQLTableId(join.getLeftHandSideId()),
                new EJBQLTableId(((EJBQLOuterFetchJoin) join).getRightHandSideId()));

        context.markCurrentPosition(EJBQLJoinAppender
                .makeJoinTailMarker(((EJBQLOuterFetchJoin) join).getRightHandSideId()));
        return false;
    }

    @Override
    public boolean visitOuterJoin(EJBQLJoin join) {
        joinAppender.appendOuterJoin(
                null,
                new EJBQLTableId(join.getLeftHandSideId()),
                new EJBQLTableId(join.getRightHandSideId()));
        context.markCurrentPosition(EJBQLJoinAppender.makeJoinTailMarker(join.getRightHandSideId()));
        return false;
    }
}
