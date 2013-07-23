/**
 * Orders keys by the nav sort key
 */
function (doc) {
    if ( !doc.navSort ) return;

    var sort = doc.navSort.split(/[\-\.\s]/).map(function (value) {
        return (+value);
    });

    sort.unshift( doc.siteID );

    emit( sort, doc );
}
