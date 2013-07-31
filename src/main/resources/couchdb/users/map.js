/**
 * Sorts the users by their name
 */
function ( doc ) {
    if ( doc.type === 'user' )
        emit(doc.name, doc);
}
