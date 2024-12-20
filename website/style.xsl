<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8"/>
    
    <xsl:template match="/">
        <html>
            <head>
                <title>Clickable Links Example</title>
                <link rel="stylesheet" type="text/css" href="style.css"/> <!-- Link to the CSS file -->
            </head>
            <body>
                <h2>Files:</h2>
                <ul>
                    <xsl:for-each select="files/file">
                        <li>
                            <a href="{link}">
                                <xsl:value-of select="name"/>
                            </a>
                        </li>
                    </xsl:for-each>
                </ul>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
