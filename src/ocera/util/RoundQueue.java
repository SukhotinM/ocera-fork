// based on Copyright 2002 Adam Megacz, see the COPYING file for licensing [GPL]
package ocera.util;

/** A simple synchronized queue, implemented as an array  */
public class RoundQueue
{
    /** The store */
    private Object[] vec;
    /** The index of the first node in the queue */
    private int first = 0;
    /** The number of elements in the queue; INVARAINT: size <= vec.length */
    private int size = 0;

    public final static boolean BLOCKING = true;
    public final static boolean NO_BLOCKING = false;

    public RoundQueue(int initiallength)
    {
        vec = new Object[initiallength];
    }

    /** Grow the queue, if needed */
    private void grow(int newlength) {
        Object[] newvec = new Object[newlength];
        if (first + size > vec.length) {
            System.arraycopy(vec, first, newvec, 0, vec.length - first);
            System.arraycopy(vec, 0, newvec, vec.length - first, size - (vec.length - first));
        } else {
            System.arraycopy(vec, first, newvec, 0, size);
        }
        first = 0;
        vec = newvec;
    }

    /** The number of elements in the queue */
    public int size() { return size; }

    /** Empties the queue */
    public synchronized void flush() {
        first = 0;
        size = 0;
        for(int i=0; i<vec.length; i++) vec[i] = null;
    }

    /** Add an element to the queue */
    public synchronized void append(Object o) {
        if (size == vec.length) grow(vec.length * 2);
        if (first + size >= vec.length) vec[first + size - vec.length] = o;
        else vec[first + size] = o;
        size++;
        if (size == 1) notify(); // wake up waiting threads
    }

    /** Remove and return and element from the queue, blocking if empty. */
    public Object remove() { return remove(true); }

    /** Remove and return an element from the queue, blocking if
        <tt>block</tt> is true and the queue is empty. */
    public synchronized Object remove(boolean block) {

        while (size == 0) {
            if (!block) return null;
            try {
                wait();
            } catch (InterruptedException e) {
            } catch (Exception e) {
                System.err.println("exception in RoundQueue.wait(); this should never happen");
                System.err.println(e);
            }
        }

        Object ret = vec[first];
        first++;
        size--;
        if (first >= vec.length) first = 0;
//        System.out.println("remove() returned, size=" + size());
        return ret;
    }

    /**
     * @return top element in the queue without removing it, blocking if
        the queue is empty.*/
    public synchronized Object peek() {
        return peek(true);
    }

    /**
     * @return the top element in the queue without removing it, blocking if
        <tt>block</tt> is true and the queue is empty. */
    public synchronized Object peek(boolean block) {
        while (size == 0) {
            if (!block) return null;
            try {
                wait();
            } catch (InterruptedException e) {
//                System.err.println("RoundQueue");
            } catch (Exception e) {
                System.err.println("exception in RoundQueue.wait(); this should never happen");
                System.err.println(e);
            }
        }
        return vec[first];
    }
}

