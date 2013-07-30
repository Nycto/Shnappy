/**
 * A list of all the sites
 */
function (doc) {
    if ( doc.type === "siteinfo" )
        emit( doc.title, doc );
}
