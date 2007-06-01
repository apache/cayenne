<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-nested.tld" prefix="nested" %>

<html:html locale="true">

<head>

<title><bean:message key="index.title"/></title>

<html:base/>

<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">

</head>

<body bgcolor="white">
<jsp:include page="navi.html" flush="true"/>

<html:form action="/saveGallery">
<table width="640" class="standardTable" border="1">
<tr>
	<td colspan="2" bgcolor="silver"><span class="titleTextStrong">Add a Gallery</span></td>
</tr>
<tr>
	<td align="right">Gallery Name:&nbsp;</td>
	<td><html:text property="galleryName" size="40" maxlength="40"/></td>
</tr>
<tr>
	<td colspan="2" align="center"><html:submit>Add Gallery</html:submit></td>
</tr>
</table>
</html:form>

</body>

</html:html>
