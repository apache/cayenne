<%@ page language="java" contentType="text/html" %>
<%@ page import="org.example.cayenne.persistent.*" %>
<%@ page import="org.apache.cayenne.*" %>
<%@ page import="org.apache.cayenne.query.*" %>
<%@ page import="org.apache.cayenne.exp.*" %>
<%@ page import="java.util.*" %>

<% 
    SelectQuery query = new SelectQuery(Artist.class);
    query.addOrdering(Artist.NAME_PROPERTY, true);

    ObjectContext context = BaseContext.getThreadObjectContext();
    List artists = context.performQuery(query);
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
            Iterator it = artists.iterator();
            while(it.hasNext()) {
                Artist a = (Artist) it.next();
        %>
        <p><a href="detail.jsp?id=<%=DataObjectUtils.intPKForObject(a)%>"> <%=a.getName()%> </a></p>
        <%
            }
            } %>
        <hr>
        <p><a href="detail.jsp">Create new artist...</a></p>
    </body>	
</html>