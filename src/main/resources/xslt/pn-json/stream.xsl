<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:j="http://www.w3.org/2005/xpath-functions"
    exclude-result-prefixes="j"
    expand-text="yes" version="3.0">

    <xsl:import href="classpath:xslt/pn-json/pn2eum.xsl"/>

    <xsl:variable name="metadata" select="json-to-xml(unparsed-text(('json')))"/>

    <xsl:template name="xsl:initial-template">
        <xsl:apply-templates select="$metadata/j:map" mode="metadata"/>
    </xsl:template>
</xsl:stylesheet>
