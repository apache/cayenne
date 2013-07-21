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
CREATE OR REPLACE FUNCTION cayenne_tst_select_proc (varchar, numeric) 
RETURNS SETOF ARTIST
AS '
     UPDATE PAINTING SET ESTIMATED_PRICE = ESTIMATED_PRICE * 2
     WHERE ESTIMATED_PRICE < $2;
 
     SELECT DISTINCT A.artist_id, A.artist_name, A.date_of_birth
     FROM ARTIST A, PAINTING P
     WHERE A.ARTIST_ID = P.ARTIST_ID AND
     RTRIM(A.ARTIST_NAME) = $1
     ORDER BY A.ARTIST_ID;
' LANGUAGE SQL;