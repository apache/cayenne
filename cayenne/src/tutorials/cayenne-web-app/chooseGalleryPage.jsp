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

<br><br>

<table width="640" class="standardTable" border="1">
<tr>
	<td bgcolor="silver"><span class="titleTextStrong">Choose Gallery for '<b><bean:write name="painting" property="paintingTitle" scope="request" /></b>'</span></td>
</tr>

<tr>
	<td><br>Galaries currently taking submissions: (choose by clicking the gallery name)<br><br>
	<logic:iterate id="aGallery" name="galleries" scope="request">
		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="choosePaintingForGallery.do?title=<bean:write name="painting" property="paintingTitle" scope="request" />&galleryName=<bean:write name="aGallery" property="galleryName"/>"><bean:write name="aGallery" property="galleryName"/></a><br>
	</logic:iterate>
	<br>
	</td>
</tr>

</table>

</body>

</html:html>
