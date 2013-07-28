/**
 * Indexes the 'updated' dates by site ID
 */
function (doc) {
    function emitUpdated ( id ) {
        emit( id, { siteID: id, updated: doc.updated });
    }

    if ( doc.siteID && doc.updated )
        emitUpdated( doc.siteID );
    else if ( doc.type == 'siteinfo' && doc.updated )
        emitUpdated( doc._id );
}
