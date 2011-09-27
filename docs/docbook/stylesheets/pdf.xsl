<?xml version="1.0" encoding="UTF-8"?>
<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:include href="common-customizations.xsl"/>

	<xsl:param name="paper.type">A4</xsl:param>

	<!-- don't indent the body text -->
	<xsl:param name="body.start.indent">0pt</xsl:param>

	<!-- print headers and footers mirrored so double sided printing works -->
	<xsl:param name="fop1.extensions" select="1"/>
	<xsl:param name="double.sided" select="1"/>

	<xsl:param name="admon.graphics" select="0"/>
	<xsl:param name="admon.graphics.extension">.png</xsl:param>
	<xsl:param name="admon.graphics.path">stylesheets/docbook-xsl-ns/images/</xsl:param>
	<xsl:param name="admon.textlabel" select="1"/>

	<xsl:param name="callout.graphics" select="1"/>
	<xsl:param name="callout.graphics.extension">.png</xsl:param>
	<xsl:param name="callout.graphics.path">stylesheets/docbook-xsl-ns/images/callouts/</xsl:param>

	<xsl:param name="footer.rule">0</xsl:param>

	<!-- Separation between glossary terms and descriptions when glossaries are presented using lists. -->
	<xsl:param name="glossterm.separation">2em</xsl:param>

	<!-- This parameter specifies the width reserved for glossary terms when a list presentation is used.  -->
	<xsl:param name="glossterm.width">10em</xsl:param>

	<!--  Specifies the longest term in variablelists. In variablelists, the listitem is indented to leave room for the term elements. The indent may be computed if it is not specified with a termlength attribute on the variablelist element. The computation counts characters in the term elements in the list to find the longest term. However, some terms are very long and would produce extreme indents. This parameter lets you set a maximum character count. Any terms longer than the maximum would line wrap. The default value is 24. The character counts are converted to physical widths by multiplying by 0.50em. There is some variability in how many characters fit in the space since some characters are wider than others. -->
	<xsl:param name="variablelist.max.termlength">18</xsl:param>

	<!-- Custom font settings - preferred truetype font -->
	<xsl:param name="title.font.family">Lucinda Grande,sans-serif</xsl:param>
	<xsl:param name="body.font.family">Times,serif</xsl:param>
	<xsl:param name="sans.font.family">Lucinda Grande,sans-serif</xsl:param>
	<xsl:param name="dingbat.font.family">Lucinda Grande,serif</xsl:param>
	<xsl:param name="monospace.font.family">monospace</xsl:param>

	<!-- Specify the default text alignment. The default text alignment is used for most body text. -->
	<xsl:param name="alignment">left</xsl:param>


	<!--  Specifies the default point size for body text. The body font size is specified in two parameters (body.font.master and body.font.size) so that math can be performed on the font size by XSLT. -->
	<xsl:param name="body.font.master">11</xsl:param>

	<xsl:param name="hyphenate">false</xsl:param>

	<!-- "normal" is 1.6. This value is multiplied by the font size -->
	<xsl:param name="line-height">1.5</xsl:param>

	<!--  Specify the spacing required between normal paragraphs. -->
	<xsl:attribute-set name="normal.para.spacing">
		<xsl:attribute name="space-before.optimum">0.8em</xsl:attribute>
		<xsl:attribute name="space-before.minimum">0.6em</xsl:attribute>
		<xsl:attribute name="space-before.maximum">1.0em</xsl:attribute>
	</xsl:attribute-set>

	<!--  Tables, examples, figures, and equations don't need to be forced onto onto one page without page breaks. -->
	<xsl:attribute-set name="formal.object.properties">
		<xsl:attribute name="keep-together.within-column">auto</xsl:attribute>
	</xsl:attribute-set>

	<!-- The body bottom margin is the distance from the last line of text in the page body to the bottom of the region-after. -->
	<xsl:param name="body.margin.bottom">20mm</xsl:param>

	<!-- The body top margin is the distance from the top of the region-before to the first line of text in the page body. -->
	<xsl:param name="body.margin.top">20mm</xsl:param>

	<!-- The top page margin is the distance from the physical top of the page to the top of the region-before. -->
	<xsl:param name="page.margin.top">10mm</xsl:param>

	<!-- The bottom page margin is the distance from the bottom of the region-after to the physical bottom of the page. -->
	<xsl:param name="page.margin.bottom">10mm</xsl:param>

	<!-- The inner page margin. The inner page margin is the distance from binding edge of the page to the first column of text. In the left-to-right, top-to-bottom writing direction, this is the left margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
	<xsl:param name="page.margin.inner">25mm</xsl:param>

	<!-- The outer page margin. The outer page margin is the distance from non-binding edge of the page to the last column of text. In the left-to-right, top-to-bottom writing direction, this is the right margin of recto pages. The inner and outer margins are usually the same unless the output is double-sided. -->
	<xsl:param name="page.margin.outer">15mm</xsl:param>

	<!-- Make hyperlinks blue and don't display the underlying URL -->
	<xsl:param name="ulink.show" select="0" />

	<xsl:attribute-set name="xref.properties">
		<xsl:attribute name="color">blue</xsl:attribute>
	</xsl:attribute-set>

	<xsl:attribute-set name="monospace.verbatim.properties">
		<xsl:attribute name="font-size">7pt</xsl:attribute>
	</xsl:attribute-set>

</xsl:stylesheet>
