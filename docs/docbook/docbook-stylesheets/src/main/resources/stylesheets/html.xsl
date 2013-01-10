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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="1.0" xmlns:d="http://docbook.org/ns/docbook">


    <xsl:import href="urn:docbkx:stylesheet" />
    <xsl:import href="highlight.xsl" />
    <xsl:include href="common-customizations.xsl"/>

    <!--<xsl:param name="highlight.source" select="1"/>-->
    <xsl:param name="html.stylesheet" select="'css/cayenne-doc.css'"/>
    <xsl:param name="chunker.output.encoding">UTF-8</xsl:param>

    <!-- Only chapters start a new page -->
    <xsl:param name="chunk.section.depth">0</xsl:param>

    <!-- Don't add any embedded styles -->
    <xsl:param name="css.decoration">0</xsl:param>

    <xsl:param name="ignore.image.scaling">1</xsl:param>

    <xsl:param name="use.id.as.filename">1</xsl:param>

    <xsl:template name="user.head.content">
        <div class="buildversion">Apache Cayenne ${pom.version}</div>
    </xsl:template>

    <!--<xsl:template name="head.content.generator">-->
        <!--<xsl:param name="node" select="."/>-->
        <!--<meta name="generator" content="DocBook {$DistroTitle} V{$VERSION}"/>-->
        <!--<meta name="keywords" content="Cayenne ${pom.version} documentation" />-->
        <!--<meta name="description" content="User documentation for Apache Cayenne version ${pom.version}" />-->
    <!--</xsl:template>-->

    <xsl:template name="head.content">
        <xsl:param name="node" select="."/>
        <xsl:param name="title">
            <xsl:apply-templates select="$node" mode="object.title.markup.textonly"/>
        </xsl:param>

        <title>
            <xsl:copy-of select="$title"/>
        </title>

        <xsl:if test="$html.base != ''">
            <base href="{$html.base}"/>
        </xsl:if>

        <!-- Insert links to CSS files or insert literal style elements -->
        <xsl:call-template name="generate.css"/>

        <xsl:if test="$html.stylesheet != ''">
            <xsl:call-template name="output.html.stylesheets">
                <xsl:with-param name="stylesheets" select="normalize-space($html.stylesheet)"/>
            </xsl:call-template>
        </xsl:if>

        <xsl:if test="$link.mailto.url != ''">
            <link rev="made" href="{$link.mailto.url}"/>
        </xsl:if>

        <meta name="generator" content="DocBook {$DistroTitle} V{$VERSION}"/>
        <meta name="keywords" content="Cayenne ${pom.version} documentation" />
        <meta name="description" content="User documentation for Apache Cayenne version ${pom.version}" />


        <xsl:if test="$generate.meta.abstract != 0">
            <xsl:variable name="info" select="(d:articleinfo
                                      |d:bookinfo
                                      |d:prefaceinfo
                                      |d:chapterinfo
                                      |d:appendixinfo
                                      |d:sectioninfo
                                      |d:sect1info
                                      |d:sect2info
                                      |d:sect3info
                                      |d:sect4info
                                      |d:sect5info
                                      |d:referenceinfo
                                      |d:refentryinfo
                                      |d:partinfo
                                      |d:info
                                      |d:docinfo)[1]"/>
            <xsl:if test="$info and $info/d:abstract">
                <meta name="description">
                    <xsl:attribute name="content">
                        <xsl:for-each select="$info/d:abstract[1]/*">
                            <xsl:value-of select="normalize-space(.)"/>
                            <xsl:if test="position() &lt; last()">
                                <xsl:text> </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:attribute>
                </meta>
            </xsl:if>
        </xsl:if>

        <xsl:if test="($draft.mode = 'yes' or
                ($draft.mode = 'maybe' and
                ancestor-or-self::*[@status][1]/@status = 'draft'))
                and $draft.watermark.image != ''">
            <style type="text/css"><xsl:text>
body { background-image: url('</xsl:text>
                <xsl:value-of select="$draft.watermark.image"/><xsl:text>');
       background-repeat: no-repeat;
       background-position: top left;
       /* The following properties make the watermark "fixed" on the page. */
       /* I think that's just a bit too distracting for the reader... */
       /* background-attachment: fixed; */
       /* background-position: center center; */
     }</xsl:text>
            </style>
        </xsl:if>
        <xsl:apply-templates select="." mode="head.keywords.content"/>
    </xsl:template>

</xsl:stylesheet>
