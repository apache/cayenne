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

package org.apache.cayenne.modeler.util;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.DbAdapterFactory;
import org.apache.cayenne.validation.BeanValidationFailure;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A DbAdapter wrapper used in CayenneModeler.
 * 
 */
public class ModelerDbAdapter extends AutoAdapter {

    protected String adapterClassName;

    public ModelerDbAdapter(DataSource dataSource) {
        this(null, dataSource);
    }

    public ModelerDbAdapter(String adapterClassName, DataSource dataSource) {
        super(dataSource);
        this.adapterClassName = adapterClassName;
    }

    /**
     * Validates DbAdapter name, throwing an exception in case it is invalid.
     */
    public void validate() throws ValidationException {
        if (adapterClassName != null) {
            ValidationFailure failure = BeanValidationFailure.validateJavaClassName(
                    this,
                    "adapterClassName",
                    adapterClassName);

            if (failure != null) {
                ValidationResult result = new ValidationResult();
                result.addFailure(failure);
                throw new ValidationException(failure.getDescription(), result);
            }
        }
    }

    public String getAdapterClassName() {
        return adapterClassName;
    }

    protected DbAdapterFactory createDefaultFactory() {
        return new AdapterFactory();
    }

    class AdapterFactory implements DbAdapterFactory {

        public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {

            if (adapterClassName == null) {
                return AutoAdapter.getDefaultFactory().createAdapter(md);
            }

            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            try {
                return ((Class<DbAdapter>) Class.forName(adapterClassName, true, loader)).newInstance();
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Can't load DbAdapter class: "
                        + adapterClassName);
            }
        }
    }
}
