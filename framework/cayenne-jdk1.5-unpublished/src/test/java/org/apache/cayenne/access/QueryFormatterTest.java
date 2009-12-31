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
package org.apache.cayenne.access;

import junit.framework.TestCase;

public class QueryFormatterTest extends TestCase {

    /**
     * Check if we have lose some words after formatting.
     */
    public void testQueryFormattingNotLosingWords() {

        String notFormattedQuery = "declare   @Amount MONEY,  @BatchBrief BRIEFNAME, "
                + " @BatchID IDENTIFIER,  @BranchBrief ACCNUMBER,  @BranchID IDENTIFIER, "
                + " @Comment COMMENT,  @CurrencyID IDENTIFIER,  @CurrencyNumber BRIEFNAME, "
                + " @DateEnd OPERDAY,  @DateOpen OPERDAY,  @DateStart OPERDAY, "
                + " @DepositProductBrief BRIEFNAME,  @DepositProductID IDENTIFIER,"
                + "  @GiverBrief USERNAME,  @GiverID IDENTIFIER,  @InterestRateValue FLOAT, "
                + " @Number NUMBER20,  @OwnerAgentBrief USERNAME,  @OwnerAgentID IDENTIFIER,"
                + "  @OwnerBrief USERNAME,  @OwnerID IDENTIFIER,  @TermDay INT_KEY, "
                + " @TermDepositID IDENTIFIER,  @TermID IDENTIFIER,  @TermMonth INT_KEY, "
                + " @UserFIOBrief USERNAME,  @UserID IDENTIFIER,  "
                + "@ReturnCode IDENTIFIER select  @Amount = ?, "
                + "   @BatchBrief = ?,    @BatchID = ?,    @BranchBrief = ?,   "
                + " @BranchID = ?,    @Comment = ?,    @CurrencyID = ?,    @CurrencyNumber = ?, "
                + "   @DateEnd = ?,    @DateOpen = ?,    @DateStart = ?,    @DepositProductBrief = ?,  "
                + "  @DepositProductID = ?,    @GiverBrief = ?,    @GiverID = ?,    @InterestRateValue = ?, "
                + "   @Number = ?,    @OwnerAgentBrief = ?,    @OwnerAgentID = ?,    @OwnerBrief = ?, "
                + "   @OwnerID = ?,    @TermDay = ?,    @TermID = ?,    @TermMonth = ?,    @UserFIOBrief = ?,"
                + "    @UserID = ?,    @ReturnCode = -1   exec @ReturnCode = MY_STORED_PROCEDURE  @Amount "
                + "  =  @Amount  ,  @BatchBrief   =  @BatchBrief  ,  @BatchID   =  @BatchID  ,  @BranchBrief "
                + "  =  @BranchBrief  ,  @BranchID   =  @BranchID  ,  @Comment   =  @Comment  ,  @CurrencyID "
                + "  =  @CurrencyID  ,  @CurrencyNumber   =  @CurrencyNumber  ,  @DateEnd   =  @DateEnd  ,"
                + "  @DateOpen   =  @DateOpen  ,  @DateStart   =  @DateStart  ,  @DepositProductBrief   ="
                + "  @DepositProductBrief  ,  @DepositProductID   =  @DepositProductID  ,  @GiverBrief   = "
                + " @GiverBrief  ,  @GiverID   =  @GiverID  ,  @InterestRateValue   =  @InterestRateValue "
                + " ,  @Number   =  @Number  ,  @OwnerAgentBrief   =  @OwnerAgentBrief  ,  @OwnerAgentID "
                + "  =  @OwnerAgentID  ,  @OwnerBrief   =  @OwnerBrief  ,  @OwnerID   =  @OwnerID  , "
                + " @TermDay   =  @TermDay  ,  @TermDepositID   =  @TermDepositID out  ,  @TermID   = "
                + " @TermID  ,  @TermMonth   =  @TermMonth  ,  @UserFIOBrief   =  @UserFIOBrief  ,"
                + "  @UserID   =  @UserID  select  @TermDepositID AS TermDepositID, "
                + " @ReturnCode AS ReturnCode";
        String[] wordsNFQ = notFormattedQuery.split("\\s+");
        String formattedQuery = QueryFormatter.formatQuery(notFormattedQuery);
        String[] wordsFQ = formattedQuery.split("\\s+");
        assertEquals(wordsNFQ.length, wordsFQ.length);
        for (int i = 0; i < wordsNFQ.length; i++) {
            assertTrue(formattedQuery.contains(wordsNFQ[i])
                    || formattedQuery.contains(wordsNFQ[i].toUpperCase()));
        }
        System.out.println(formattedQuery);
    }

}
