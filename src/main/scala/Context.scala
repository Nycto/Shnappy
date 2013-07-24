package com.roundeights.shnappy

/**
 * Represents contextual information for the current page load
 */
class Context ( val data: SiteData, val renderer: Renderer ) {

    /** The site info instance */
    def siteInfo = data.siteInfo

    /** The theme folder for this site */
    def theme = siteInfo.theme

    /** The favicon to use for this site */
    def favicon = siteInfo.favicon
}

