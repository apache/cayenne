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
package org.apache.cayenne.project.validation;

import java.lang.reflect.Field;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLParserFactory;
import org.apache.cayenne.map.EJBQLQueryDescriptor;

public class EJBQLStatementValidator {

    public PositionException validateEJBQL(EJBQLQueryDescriptor query) {
        if (query.getEjbql() != null) {
            PositionException message = null;

            try {
                // Only parse query and validate it's syntax.
                // It still may be invalid e.g. invalid entities used.
                // We can't call compile() for the full check as it will try to get
                // Class objects for ObjEntities used in query that are not available
                // in the modeler
                EJBQLParserFactory.getParser().parse(query.getEjbql());
            } catch (CayenneRuntimeException e) {
                message = new PositionException();
                message.setE(e);
                if (e.getCause() != null) {

                    message.setMessage(e.getCause().getMessage());

                    if (e instanceof EJBQLException) {
                        EJBQLException ejbqlException = (EJBQLException) e;
                        Throwable cause = ejbqlException.getCause();

                        if (cause != null) {
                            try {
                                Field tokenField = cause.getClass().getField("currentToken");
                                Object token = tokenField.get(cause);
                                Field nextTokenField = token.getClass().getField("next");
                                Object nextToken = nextTokenField.get(token);
                                Field beginColumnField = nextToken.getClass().getField("beginColumn");
                                Field beginLineField = nextToken.getClass().getField("beginLine");
                                Field endColumnField = nextToken.getClass().getField("endColumn");
                                Field endLineField = nextToken.getClass().getField("endLine");
                                Field imageField = nextToken.getClass().getField("image");

                                message.setBeginColumn((Integer) beginColumnField.get(nextToken));
                                message.setBeginLine((Integer) beginLineField.get(nextToken));
                                message.setEndColumn((Integer) endColumnField.get(nextToken));
                                message.setEndLine((Integer) endLineField.get(nextToken));
                                message.setImage((String) imageField.get(nextToken));
                                message.setLength(message.getImage().length());
                            } catch (Exception e1) {
                                throw new CayenneRuntimeException(e1);
                            }
                        }
                    }
                } else {
                    message.setE(e);
                    message.setMessage(e.getUnlabeledMessage());
                }
            } catch (Throwable e) {
                message = new PositionException();
                if(e instanceof Exception) {
                    message.setE((Exception)e);
                }
                message.setMessage(e.getMessage());
            }

            return message;
        }
        else {
            return null;
        }
    }

    public class PositionException {

        private Integer beginColumn;
        private Integer beginLine;
        private Integer endColumn;
        private Integer endLine;
        private Integer length;
        private String image;
        private String message;
        private Exception e;

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public Exception getE() {
            return e;
        }

        public void setE(Exception e) {
            this.e = e;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Integer getBeginColumn() {
            return beginColumn;
        }

        public void setBeginColumn(Integer beginColumn) {
            this.beginColumn = beginColumn;
        }

        public Integer getBeginLine() {
            return beginLine;
        }

        public void setBeginLine(Integer beginLine) {
            this.beginLine = beginLine;
        }

        public Integer getEndColumn() {
            return endColumn;
        }

        public void setEndColumn(Integer endColumn) {
            this.endColumn = endColumn;
        }

        public Integer getEndLine() {
            return endLine;
        }

        public void setEndLine(Integer endLine) {
            this.endLine = endLine;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
