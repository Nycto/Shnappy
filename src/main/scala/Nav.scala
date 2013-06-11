package com.roundeights.shnappy

/** Represents the sort position of an element in the Nav */
case class SortKey ( val keys: IndexedSeq[Int] ) {

    /** The parts of this sort key */
    def this ( keys: String ) = this(
        keys.split('.').map( _.toInt ).seq match {
            case parsed if parsed.length > 0 => parsed
            case _ => IndexedSeq(1)
        }
    )

    /** Serializes this component down to a JSON instance */
    override def toString: String = keys.mkString(".")

    /** Generates a sort key that will put another element after this one */
    def after = new SortKey( IndexedSeq( keys(0) + 1 ) )

}

/** A link in the navigation */
case class NavLink ( val url: String, val text: String, sort: SortKey ) {

    /** Returns this nav instance is a map of data */
    def toMap: Map[String, String] = Map("url" -> url, "text" -> text)
}

