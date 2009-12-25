<!-- 
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
 -->
<%@ page language="java" contentType="text/html" %>
<%@ page import="org.apache.cayenne.tutorial.persistent.*" %>
<%@ page import="org.apache.cayenne.*" %>
<%@ page import="org.apache.cayenne.query.*" %>
<%@ page import="org.apache.cayenne.exp.*" %>
<%@ page import="java.util.*" %>

<% 
    SelectQuery query = new SelectQuery(Artist.class);
    query.addOrdering(Artist.NAME_PROPERTY, SortOrder.ASCENDING);

    ObjectContext context = BaseContext.getThreadObjectContext();
    List<Artist> artists = context.performQuery(query);
%>
<html>
    <head>
        <title>Main</title>
    </head>
    <body>
        <h2>Artists:</h2>
        
        <% if(artists.isEmpty()) {%>
        <p>No artists found</p>
        <% } else { 
        	for(Artist a : artists) {
        %>
        <p><a href="detail.jsp?id=<%=DataObjectUtils.intPKForObject(a)%>"> <%=a.getName()%> </a></p>
        <%
            }
            } %>
        <hr>
        <p><a href="detail.jsp">Create new artist...</a></p>
    </body>	
</html>