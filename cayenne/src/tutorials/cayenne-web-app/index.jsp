<%@ page language="java" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<html:html locale="true">

<head>

<title><bean:message key="index.title"/></title>

<html:base/>

<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">

</head>

<body bgcolor="white">
<jsp:include page="navi.html" flush="true"/>

<logic:notPresent name="org.apache.struts.action.MESSAGE" scope="application">

  <font color="red">

    ERROR:  Application resources not loaded -- check servlet container

    logs for error messages.

  </font>

</logic:notPresent>



<h3><bean:message key="index.heading"/></h3>

<p><bean:message key="index.message"/></p>



</body>

</html:html>

