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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CaseDataFactory {

    private static void createArtist(Connection conn, String artistName) throws Exception {
        String insertArtist = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
        PreparedStatement stmt = conn.prepareStatement(insertArtist);
        long dateBase = System.currentTimeMillis();

        stmt.setInt(1, 1);
        stmt.setString(2, artistName);
        stmt.setDate(3, new java.sql.Date(dateBase - 1000 * 60 * 60 * 24 * 365 * 30));
        stmt.executeUpdate();

        stmt.close();
        conn.commit();
    }

    public static void createArtistWithPainting(
            String artistName,
            String[] paintingNames,
            boolean paintingInfo) throws Exception {

        Connection conn = CayenneResources
                .getResources()
                .getDataSource()
                .getConnection();

        try {
            conn.setAutoCommit(false);
            createArtist(conn, artistName);

            String insertPt = "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, ESTIMATED_PRICE, PAINTING_TITLE) VALUES (?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(insertPt);

            int len = paintingNames.length;
            if (len > 0) {
                for (int i = 0; i < len; i++) {
                    stmt.setInt(1, i + 1);
                    stmt.setInt(2, 1);
                    stmt.setFloat(3, 1000 * i);
                    stmt.setString(4, paintingNames[i]);
                    stmt.executeUpdate();
                }
                stmt.close();
                conn.commit();

                if (paintingInfo) {
                    String insertPtI = "INSERT INTO PAINTING_INFO (PAINTING_ID, TEXT_REVIEW) VALUES (?,?)";
                    stmt = conn.prepareStatement(insertPtI);
                    for (int i = 0; i < len; i++) {
                        stmt.setInt(1, i + 1);
                        stmt.setString(2, "text: " + paintingNames[i]);
                        stmt.executeUpdate();
                    }

                    stmt.close();
                    conn.commit();
                }

            }
        }
        finally {
            conn.close();
        }
    }
}
