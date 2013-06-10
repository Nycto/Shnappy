/**
 * Orders keys by the nav sort key
 */
function (doc) {
    if ( !doc.navSort ) return;

    var sort = doc.navSort.split(/[\-\.\s]/).map(function (value) {
        return (+value);
    });

    emit( sort, doc );
}
