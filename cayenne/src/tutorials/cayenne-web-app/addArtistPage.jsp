<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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

<html:form action="/saveArtist">
<table width="640" class="standardTable" border="1">
<tr>
	<td colspan="2" bgcolor="silver"><span class="titleTextStrong">Add an Artist</span></td>
</tr>
<tr>
	<td align="right">Artist Name:&nbsp;</td>
	<td><html:text property="artistName" size="50"/></td>
</tr>
<tr>
	<td align="right">Date of Birth:&nbsp;</td>
	<td><html:text property="dateOfBirth" size="10" maxlength="10" /></td>
</tr>
<tr>
	<td colspan="2" align="center"><html:submit><bean:message key="button.addartist"/></html:submit></td>
</tr>
</table>
</html:form>

</body>
</html:html>