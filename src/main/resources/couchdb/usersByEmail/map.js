/**
 * Indexes users by their email address
 */
function (doc) {
    if ( doc.email )
        emit( doc.email, doc );
}

