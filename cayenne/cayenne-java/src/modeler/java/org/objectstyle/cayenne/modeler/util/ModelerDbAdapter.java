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
package org.objectstyle.cayenne.modeler.util;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.AutoAdapter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.dba.DbAdapterFactory;
import org.objectstyle.cayenne.validation.BeanValidationFailure;
import org.objectstyle.cayenne.validation.ValidationException;
import org.objectstyle.cayenne.validation.ValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * A DbAdapter wrapper used in CayenneModeler.
 * 
 * @author Andrus Adamchik
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
                Class adapterClass = Class.forName(adapterClassName, true, loader);
                return (DbAdapter) adapterClass.newInstance();
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Can't load DbAdapter class: "
                        + adapterClassName);
            }
        }
    }
}
