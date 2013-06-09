/**
 * Keys pages by their slug
 */
function (doc) {
    if ( doc.slug )
        emit( doc.slug, doc );
}
