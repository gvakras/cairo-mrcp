/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005 Onomatopia, Inc. - http://www.onomatopia.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
 *
 */
package com.onomatopia.cairo.util;

import java.util.LinkedList;
import java.util.List;

/**
 * A FIFO queue that blocks on the remove method until an element is available to remove.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
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
