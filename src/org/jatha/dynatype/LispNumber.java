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

import java.math.BigInteger;

/**
 * LispNumber is an abstract class that implements
 * the Common LISP NUMBER type.  It contains the
 * definitions of add, subtract, multiply and divide.
 *
 * @see http://en.wikipedia.org/wiki/Natural_number
 *
 * @see LispValue
 * @see LispAtom
 * @see LispInteger
 * @see LispReal
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public interface LispNumber extends LispAtom
{

	/**
	 * Returns a double value corresponding to this value.
	 */
	public double getDoubleValue();
	public BigInteger getBigIntegerValue();
	long getLongValue();

	/**
	 * Returns the sum of the object and the object(s) in the argument list.
	 * This is the <code>+</code> function in LISP.
	 */
	public LispNumber add(LispValue args);
	
	/**
	 * Returns the quotient of the object and the object(s) in the argument list.
	 * This is the <code>/</code> function in LISP.
	 */
	public LispNumber div(LispValue args);
	
	/**
	 * Returns the product of the object and the object(s) in the argument list.
	 * This is the <code>*</code> function in LISP.
	 */
	public LispNumber mul(LispValue args);
	
	/**
	 * Returns the difference of the object and the object(s) in the argument list.
	 * This is the <code>-</code> function in LISP.
	 */
	public LispNumber sub(LispValue args);
}