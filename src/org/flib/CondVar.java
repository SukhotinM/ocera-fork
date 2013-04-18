package org.flib;

/**
 * org.flib.CondVar
 * <p/>
 * (C) Copyright 3:28:27 PM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class CondVar
{
    private boolean _is_true;

    /** Create a new condition variable in a known state.
     */
    public CondVar(boolean is_true) {_is_true = is_true;}

    /** See if the condition variable is true (without releasing).
     */
    public synchronized boolean is_true()  {return _is_true;}

    /** Set the condition to false. Waiting threads are not affected.
     */
    public synchronized void setFalse() { _is_true = false; }

    /** Set the condition to true. Waiting threads are not released.
     */
    public synchronized void setTrue() { _is_true = true; notifyAll(); }

    /** Release all waiting threads without setting the condition true
     */
    public synchronized void releaseAll(){ notifyAll(); }

    /** Release one waiting thread without setting the condition true
     */
    public synchronized void releaseOne(){ notify(); }

    /** Wait for the condition to become true.
     *  @param timeout Timeout in milliseconds
     */
    public synchronized void waitForTrue( long timeout ) throws InterruptedException
    {
        if (!_is_true ) wait(timeout);
    }

    /** Wait (potentially forever) for the condition to become true.
     */
    public synchronized void waitForTrue() throws InterruptedException
    {
        if(!_is_true ) wait();
    }
}