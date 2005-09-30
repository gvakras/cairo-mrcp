package com.onomatopia.cairo.util;

import java.util.LinkedList;
import java.util.List;

/**
 * A FIFO queue that blocks on the remove method until an element is available to remove.
 */
@Deprecated public class BlockingFifoQueue<E> {

    private List<E> list;

    /**
     * Creates a new data list
     */
    public BlockingFifoQueue() {
        list = new LinkedList<E>();
    }

    /**
     * Adds a data to the queue
     *
     * @param data the data to add
     */
    public synchronized void add(E data) {
        list.add(data);
        notify();
    }

    /**
     * Returns the current size of the queue
     *
     * @return the size of the queue
     */
    public synchronized int size() {
        return list.size();
    }

    /**
     * Removes the oldest item on the queue
     *
     * @return the oldest item
     */
    public synchronized E remove() throws InterruptedException {
        while (list.size() == 0) {
            wait();
        }
        E data = list.remove(0);
        if (data == null) {
            System.out.println("BlockingFifoQueue is returning null.");
        }
        return data;
    }
}
