/**
 * Site info indexed by host name
 */
function (doc) {
    if ( doc.type !== 'siteinfo' ) return;
    if ( !doc.hosts ) return;

    doc.hosts.forEach(function (hostname) {
        hostname = hostname.toLowerCase().trim();

        if ( hostname.slice(0, 4) === 'www.' )
            hostname = hostname.slice(4);

        if ( hostname !== '' )
            emit( hostname, doc );
    });
}

