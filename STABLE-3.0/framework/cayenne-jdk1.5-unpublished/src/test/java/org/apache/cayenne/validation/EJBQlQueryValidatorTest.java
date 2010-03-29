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
package org.apache.cayenne.validation;

import org.apache.cayenne.project.validator.EJBQLQueryValidator;
import org.apache.cayenne.project.validator.EJBQLQueryValidator.PositionException;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;


public class EJBQlQueryValidatorTest  extends CayenneCase{

    public void testValidateEJBQL(){
        String[] ejbqlError = new String[] {
                "select g from G g",
                "select Artist FROM Artist",
                "select f from g", 
                "dddd",
                "select g.f from g",
                "select Artist.f from Artist", 
                "select Artist.f FROM Artist a",
                "select a FROM Artist a WHERE a.artistName = ?1 OR ",
                "select b FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :nam",
                "select artistName FROM Artist a WHERE a.artistName = ?1 OR a.artistName = :name",
                "select a FROM Artist a WHERE a.artistName = ?",
                "select a FROM Artist a WHERE  OR a.artistName = :name",
                "select b FROM Artist b WHERE  a.artistName = :name",
                "a FROM Artist a",
                "ubdate a FROM Artist a",
                "update a FROM Artist"
        };
        Object[] typeError = new Object[] {
                new org.apache.cayenne.CayenneRuntimeException(),
                new java.lang.NullPointerException(),
                new java.lang.NullPointerException(),
                new org.apache.cayenne.ejbql.EJBQLException(), 
                new org.apache.cayenne.CayenneRuntimeException(),
                new org.apache.cayenne.ejbql.EJBQLException(), 
                new org.apache.cayenne.ejbql.EJBQLException(),
                new org.apache.cayenne.ejbql.EJBQLException(),
                new java.lang.NullPointerException(),
                new java.lang.NullPointerException(),
                new org.apache.cayenne.ejbql.EJBQLException(),
                new org.apache.cayenne.ejbql.EJBQLException(),
                new org.apache.cayenne.ejbql.EJBQLException(), 
                new org.apache.cayenne.ejbql.EJBQLException(),
                new org.apache.cayenne.ejbql.EJBQLException(),
                new org.apache.cayenne.ejbql.EJBQLException()
        };
        
        

        for (int i=0; i<ejbqlError.length;i++) {            
            EJBQLQuery queryError = new EJBQLQuery(ejbqlError[i]);
            EJBQLQueryValidator ejbqlQueryValidator = new EJBQLQueryValidator();
            PositionException s = ejbqlQueryValidator.validateEJBQL(queryError, getDomain());
            assertEquals(typeError[i].getClass(),s.getE().getClass());
          }
    }
}
