/**
 * Keys pages by their slug
 */
function (doc) {
    if ( doc.slug )
        emit( [doc.siteID, doc.slug], doc );
}
