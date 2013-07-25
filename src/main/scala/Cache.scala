package com.roundeights.shnappy

import scala.concurrent._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * A reference to a value produced by a Future
 */
private[shnappy] class LazyRef[T] (
    private val build: () => Future[T]
)(
    implicit val ctx: ExecutionContext
){

    /** The cached value */
    private val value = new AtomicReference[Future[T]]

    /** Returns this value */
    def apply(): Future[T] = {

        def pull: Future[T] = {
            val pulled = build()
            value.set( pulled )
            pulled
        }

        value.get match {
            case null => pull
            case saved => saved.recoverWith { case _ => pull }
        }
    }
}

/**
 * A lazy hash map
 */
private[shnappy] class LazyMap[K,V] (
    private val build: (K) => Future[V]
)(
    implicit val ctx: ExecutionContext
) {

    /** A cache of page data by slug */
    private val cache = new ConcurrentHashMap[K, Future[V]]

    /** Returns a future reference for the given key */
    def get ( key: K ): Future[V] = {

        def pull: Future[V] = {
            val pulled = build(key)
            cache.put(key, pulled)
            pulled
        }

        if ( cache.contains(key) )
            cache.get(key).recoverWith { case _ => pull }
        else
            pull
    }
}

