/**
 * Indexes links and pages for a site
 */
function (doc) {
    if ( doc.type !== 'page' && doc.type !== 'link' ) return;

    var sort = {};
    if ( doc.navSort ) {
        sort = ('' + doc.navSort).split('.').map(function (value) {
            return (+value);
        });
    }

    emit( [ doc.siteID, sort ], doc );
}

