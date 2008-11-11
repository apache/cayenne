/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.modeler.util;

/**
 * A circular array is an array of fixed size as objects are added it will push objects
 * off of the end to allow space for new objects to be added. This is useful for things
 * like a fixed history size for a navigation tool.
 * 
 * @since 1.2
 */
public class CircularArray extends Object {

    private Object array[] = null;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private int capacity = 0;

    /**
     * Creates an array of capacity size.
     * 
     * @param capacity - size of the new array
     */
    public CircularArray(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }
        array = new Object[capacity];
        this.capacity = capacity;
    }

    /**
     * Clears out the contents of the array.
     */
    public synchronized void clear() {
        array = new Object[capacity];
        head = 0;
        tail = 0;
        count = 0;
    }

    /**
     * Returns true if the array has no elements;
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Adds a new object to the array. If the array is full it will push the oldest item
     * out of the array.
     * 
     * @param obj - the object to be added
     */
    public synchronized void add(Object obj) {
        // we have wrapped and we have to move the head pointer
        if (count == capacity && tail == head) {
            head = (head + 1) % capacity;
        }

        array[tail] = obj;

        tail = (tail + 1) % capacity;

        count++;
        if (count > capacity)
            count = capacity;
    }

    /**
     * Returns the number of objects stored in the array.
     */
    public int capacity() {
        return capacity;
    }

    /*
     * Converts the logical index into a physical index.
     */
    int convert(int index) {
        return (index + head) % capacity;
    }

    /*
     * Makes sure that the index is within range.
     */
    private void rangeCheck(int index) {
        if (index >= capacity || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + capacity);
        }
    }

    /**
     * Returns true if the array contains the specified object.
     * 
     * @param obj the object to be checked
     */
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    /**
     * Gets the object at the specified index.
     * 
     * @param index the index of the object to be retrieved
     */
    public synchronized Object get(int index) {
        rangeCheck(index);
        if (count == 0) {
            return null;
        }
        return array[convert(index)];
    }

    /**
     * Returns the index of the specified object
     * 
     * @param obj the object that is being searched for
     */
    public synchronized int indexOf(Object obj) {
        for (int i = 0; i < capacity; i++) {
            int index = convert(i);
            if (array[index] == obj) {
                return i;
            }
        }
        // not found
        return -1;
    }

    /**
     * Removes the specified object from the array
     * 
     * @param i the index of the object to be removed
     */
    public synchronized void remove(Object obj) {
        if (count == 0) {
            return;
        }
        
        int i = indexOf(obj);

        while (i >= 0) {
            // convert from logical to physical location
            int pos = convert(i);

            if (pos == head) {
                // move the head up one
                head = (head + 1) % capacity;
                array[pos] = null;
                count--;
            }
            else if (pos == tail) {
                // move the tail back one
                tail = (tail - 1 + capacity) % capacity;
                array[pos] = null;
                count--;
            }
            else {
                // create a brand new array and start it back out at zero
                Object[] a = new Object[capacity];
                int destPos = 0;
                int len = 0;

                if (head == tail) {
                    // most likeley scenario when it is full
                    if (head < pos) {
                        // copy from head to position
                        len = pos - head;
                        System.arraycopy(array, head, a, destPos, len);
                        destPos += len;

                        // copy from pos +1 to end
                        len = (capacity - 1) - pos;
                        if (len > 0) {
                            System.arraycopy(array, pos + 1, a, destPos, len);
                            destPos += len;
                        }

                        // copy from zero to head
                        len = head;
                        if (len > 0) {
                            System.arraycopy(array, 0, a, destPos, len);
                        }
                    }
                    else if (head > pos) {
                        // copy from head to end of array
                        len = capacity - head;
                        if (len > 0) {
                            System.arraycopy(array, head, a, destPos, len);
                            destPos += len;
                        }

                        // copy from zero to pos -1
                        len = pos;
                        if (len > 0) {
                            System.arraycopy(array, 0, a, destPos, len);
                            destPos += len;
                        }

                        // copy from pos + 1 to tail
                        len = tail - pos - 1;
                        if (len > 0) {
                            System.arraycopy(array, pos + 1, a, destPos, len);
                        }
                    }
                }
                else if (head < tail) {
                    // copy from head to position -1
                    len = pos - head;
                    if (len > 0) {
                        System.arraycopy(array, head, a, destPos, len);
                        destPos += len;
                    }

                    // copy from position + 1 to tail
                    len = tail - pos;
                    if (len > 0) {
                        System.arraycopy(array, pos + 1, a, destPos, len);
                        destPos += len;
                    }
                }
                else if (head > tail) {
                    if (head < pos) {
                        // copy from head to position
                        len = pos - head;
                        System.arraycopy(array, head, a, destPos, len);
                        destPos += len;

                        // copy from pos +1 to end
                        len = capacity - 1 - pos;
                        if (len > 0) {
                            System.arraycopy(array, pos + 1, a, destPos, len);
                            destPos += len;
                        }

                        // copy from beginning to tail
                        len = tail;
                        if (len > 0) {
                            System.arraycopy(array, 0, a, destPos, len);
                        }
                    }
                    else if (head > pos) {
                        // copy from head to end of array
                        len = capacity - head;
                        if (len > 0) {
                            System.arraycopy(array, head, a, destPos, len);
                            destPos += len;
                        }

                        // copy from zero to pos -1
                        len = pos - 1;
                        if (len > 0) {
                            System.arraycopy(array, 0, a, destPos, len);
                            destPos += len;
                        }

                        // copy from pos+1 to tail
                        len = tail - pos;
                        if (len > 0) {
                            System.arraycopy(array, pos + 1, a, destPos, len);
                        }
                    }
                }
                count--;
                array = a;
                head = 0;
                tail = count;
            }
            i = indexOf(obj);
        }
    }

    /**
     * Resizes the array to the specified new size. If the new capacity is smaller than
     * the current object count in the array, it will keep the newCapacity most recent
     * objects.
     * 
     * @param newCapacity the new capacity of the array
     */
    public synchronized void resize(int newCapacity) {
        int i = 0;
        int offset = 0;
        if (newCapacity < count) {
            // making it smaller so we want to readjust the first object
            // to be copied into the new array
            i = count - newCapacity;
            offset = count - newCapacity;
        }
        Object newArray[] = new Object[newCapacity];
        for (; i < count; i++) {
            newArray[i - offset] = array[convert(i)];
        }
        head = 0;
        tail = 0;
        capacity = newCapacity;

        // adjust the count if it is more than the new capacity
        if (capacity < count)
            count = capacity;
        array = newArray;
    }

    /**
     * Returns the number of objects stored in the array.
     */
    public int size() {
        return count;
    }

    /**
     * Converts the array to an Object array.
     */
    public synchronized Object[] toArray() {
        Object[] o = new Object[capacity];
        for (int i = 0; i < capacity; i++) {
            o[i] = array[convert(i)];
        }
        return o;
    }

    public synchronized String internalRep() {
        Object o = null;

        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        for (int i = 0; i < array.length; i++) {
            sb.append('(').append(i).append(")  ");

            o = array[i];
            if (o == null) {
                sb.append("null");
            }
            else {
                sb.append(o.toString());
            }
            if (i == head || i == tail) {
                sb.append('<');
                if (i == head)
                    sb.append("h");
                if (i == tail)
                    sb.append("t");
            }
            sb.append("\n");
        }

        sb.append("count = [");
        sb.append(count);
        sb.append("]");

        sb.append("\nhead  = [");
        sb.append(head);
        sb.append("]");

        sb.append("\ntail  = [");
        sb.append(tail);
        sb.append("]");

        return sb.toString();
    }

    public String toString() {
        Object[] oa = toArray();
        Object o = null;

        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < oa.length; i++) {
            o = oa[i];
            if (i > 0) {
                sb.append(", ");
            }
            if (o == null) {
                sb.append("null");
            }
            else {
                sb.append(o.toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
