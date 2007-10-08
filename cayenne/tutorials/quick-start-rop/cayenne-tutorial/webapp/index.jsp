<%@ page language="java" contentType="text/html" %>
<%@ page import="cayenne.tutorial.*" %>
<%@ page import="org.objectstyle.cayenne.*" %>
<%@ page import="org.objectstyle.cayenne.query.*" %>
<%@ page import="org.objectstyle.cayenne.exp.*" %>
<%@ page import="org.objectstyle.cayenne.access.*" %>
<%@ page import="java.util.*" %>

<% 
    SelectQuery query = new SelectQuery(Artist.class);
    query.addOrdering(Artist.NAME_PROPERTY, true);

    DataContext context = DataContext.getThreadDataContext();
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