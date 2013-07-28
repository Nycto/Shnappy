/**
 * Compares update dates to find the most recent for each site
 */
function (key, vals, rereduce) {
    var recent;

    for ( var i = 0, len = vals.length; i < len; i++ ) {
        if ( !recent )
            recent = vals[i];
        else if ( recent.siteID != vals[i].siteID )
            return null;
        else if ( recent.updated < vals[i].updated )
            recent = vals[i];
    }

    return recent;
}

