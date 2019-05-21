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
DROP PROCEDURE IF EXISTS cayenne_tst_upd_proc;
DROP PROCEDURE IF EXISTS cayenne_tst_select_proc;
CREATE PROCEDURE cayenne_tst_upd_proc( IN paintingPrice INT ) LANGUAGE JAVA EXTERNAL NAME 'CLASSPATH:org.apache.cayenne.unit.HSQLProcedures.cayenne_tst_upd_proc';
CREATE PROCEDURE cayenne_tst_select_proc( IN name VARCHAR(50), IN paintingPrice INT ) LANGUAGE JAVA EXTERNAL NAME 'CLASSPATH:org.apache.cayenne.unit.HSQLProcedures.cayenne_tst_select_proc';