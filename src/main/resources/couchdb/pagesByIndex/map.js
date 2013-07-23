/**
 * Keys pages by when they were marked to be the index
 */
function (doc) {
    if ( doc.markedIndex )
        emit( [doc.siteID, doc.markedIndex], doc );
}
