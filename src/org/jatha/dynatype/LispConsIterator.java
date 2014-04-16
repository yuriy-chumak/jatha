/*
 * Jatha - a Common LISP-compatible LISP library in Java.
 * Copyright (C) 1997-2005 Micheal Scott Hewett
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * For further information, please contact Micheal Hewett at
 *   hewett@cs.stanford.edu
 *
 */

package org.jatha.dynatype;

import java.util.Iterator;

import org.jatha.Lisp;

import
static org.jatha.dynatype.LispValue.*;


// @date    Thu Mar 27 13:35:07 1997
/**
 * An abstract class for the CONS and NIL data types.
 *
 * @see LispValue
 * @see LispInteger
 * @see LispReal
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public class LispConsIterator// extends StandardLispValue
		implements Iterator<LispValue>
{
	protected LispValue m_list = null;

	public LispConsIterator(LispValue list)
	{
		m_list = list;
	}

	public boolean hasNext()
	{
		return (m_list != NIL);
	}

	public LispValue next()
	{
		LispValue result = car(m_list);
		m_list = cdr(m_list);
		return result;
	}

	public void remove()
	{
		throw new UnsupportedOperationException("LispCons iterator does not support remove().");
	}
	
	static final LispValue car(LispValue arg)
	{
		return ((LispList)arg).car();
	}
	static final LispValue cdr(LispValue arg) 
	{
		return ((LispList)arg).cdr();
	}
}