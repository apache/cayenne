<%@ page import="java.io.PrintWriter" %>
<%@ include file="../common/IncludeTop.jsp" %>

<logic:notPresent name="BeanActionException">
  <logic:notPresent name="message">
    <h3>Something happened...</h3>
    <b>But no further information was provided.</b>
  </logic:notPresent>
</logic:notPresent>
<p/>
<logic:present name="BeanActionException">
  <h3>Error!</h3>
  <b><bean:write name="BeanActionException" property="class.name"/></b>

  <p/>
  <bean:write name="BeanActionException" property="message"/>
</logic:present>
<p/>
<logic:present name="BeanActionException">
  <h4>Stack</h4>
  <pre>
    <%
      Exception e = (Exception) request.getAttribute("BeanActionException");
      e.printStackTrace(new PrintWriter(out));
    %>
  </pre>
</logic:present>

<%@ include file="../common/IncludeBottom.jsp" %>