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
package org.apache.cayenne.access.jdbc;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;


public class EJBQLIdColumnsTranslator extends EJBQLIdentifierColumnsTranslator {

    private EJBQLTranslationContext context;
    private Set<String> columns;
    
    EJBQLIdColumnsTranslator(EJBQLTranslationContext context) {
        super(context);
        this.context = context;
    }
    
    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {

        Map<String, String> xfields = null;
        if (context.isAppendingResultColumns()) {
            xfields = context.nextEntityResult().getFields();
        }

        // assign whatever we have to a final ivar so that it can be accessed within
        // the inner class
        final Map<String, String> fields = xfields;
        final String idVar = expression.getText();
        
        ClassDescriptor descriptor = context.getEntityDescriptor(idVar);
        ObjEntity oe = descriptor.getEntity();

        
        Iterator<ObjAttribute> ObjIterator = oe.getPrimaryKeys().iterator();
        try{
            while(ObjIterator.hasNext()){
                
                ObjAttribute temp = ObjIterator.next();
                DbAttribute t = (DbAttribute) oe.getDbEntity().getAttribute(temp.getDbAttributeName());
                appendColumn(idVar, temp,t, fields ,temp.getType());
            }
        }catch (Exception e) {
            // TODO: handle exception
        }

        return false;
    }

}
