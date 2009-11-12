// $Id: ConcurrentQueueMap.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.Queue;

/**
 * A mapping from a key, to queues of objects. Useful for mapping event keys to
 * consumers who are listening for those events, in cases where consumers are
 * triggered only once.
 */
public class ConcurrentQueueMap<K, V> extends Object {
    private ConcurrentMap<K, Queue<V>> internalMap
        = new ConcurrentHashMap<K, Queue<V>>();

    public      ConcurrentQueueMap() {
    }

    public void offer(K key, V consumer) {
        Queue<V> queue;

        // Synchronize access so two threads can't try creating new queues at
        // the same time
        synchronized(this) {
            queue = this.internalMap.get(key);
            if (null == queue) {
                queue = new ConcurrentLinkedQueue<V>();
                this.internalMap.put(key, queue);
            }       
        }
        queue.offer(consumer);
    }

    /**
     * Retrieves, but does not remove, the head of the queue specified by the
     * given key, or returns null if this queue is empty/absent.
     */
    public V peek(K key) {
        final Queue<V> queue = this.internalMap.get(key);

        if (null != queue) {
            return queue.peek();
        }

        return null;
    }

    /**
     * Retrieves and removes the head of the queue specified by the given key,
     * or returns null if this queue is empty/absent.
     */
    public V poll(K key) {
        final Queue<V> queue = this.internalMap.get(key);

        if (null != queue) {
            return queue.poll();
        }

        return null;
    }
}
