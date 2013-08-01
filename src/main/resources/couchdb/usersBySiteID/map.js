/**
 * Indexes users by what site they can access
 */
function ( doc ) {
    if ( doc.type !== 'user' ) return;

    if ( !doc.sites || !doc.sites.forEach ) return;

    doc.sites.forEach(function (siteID) {
        emit([siteID, doc.name], doc);
    });
}
