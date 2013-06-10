package com.roundeights.shnappy

/** Represents the sort position of an element in the Nav */
case class SortKey ( val keys: IndexedSeq[Int] ) {

    /** The parts of this sort key */
    def this ( keys: String ) = this(
        keys.split(".").map( _.toInt ).seq match {
            case parsed if parsed.length > 0 => parsed
            case _ => IndexedSeq(1)
        }
    )

    /** Generates a sort key that will put another element after this one */
    def after = new SortKey( IndexedSeq( keys(0) + 1 ) )

}

/** Nav objects have the ability to produce a link and be sorted */
trait Nav {

    /** The URL for this link */
    def linkUrl: String

    /** The text for this link */
    def linkText: String

    /** The sort order of this link */
    def sortKey: SortKey

    /** Returns this nav instance is a map of data */
    def toMap: Map[String, String] = Map("url" -> linkUrl, "text" -> linkText)
}

