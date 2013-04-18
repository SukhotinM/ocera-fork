package org.flib.net;

/**
 * org.flib.net.NotCPickleDataException
 * <p/>
 * (C) Copyright 11:58:44 AM by Frantisek Vacek - Originator
 * <p/>
 * The software is distributed under the Gnu General Public License.
 * See file COPYING for details.
 * <p/>
 * Originator reserve the right to use and publish sources
 * under different conditions too. If third party contributors
 * do not accept this condition, they can delete this statement
 * and only GNU license will apply.
 */
public class NotCPickleDataException extends CPickleException
{
    public NotCPickleDataException(String message)
    {
        super(message);
    }
}
