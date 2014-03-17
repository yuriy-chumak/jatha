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
// ===================================================================
// Copyright (c) 1997-1998, All rights reserved, by Micheal Hewett
//
// This software is free for educational and non-profit use.
// Any for-profit use must be governed by a license available
// from the author, who can be contacted via email at
// "hewett@cs.stanford.edu"
//
// ===================================================================
//
//  SECDop.java  - SECD and LISP primitive operations
//
//  24 Jan 1997
//  22 Aug 1998 - Moved individual opcodes to separate files
//                to satisfy the Java 1.1 compiler.
//
// -------------------------------------------------------------------

package org.jatha.machine;

import org.jatha.Jatha;
import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispCons;
import org.jatha.dynatype.LispList;
import org.jatha.dynatype.LispInteger;
import org.jatha.dynatype.LispValue;


// @date    Sat Feb  1 21:05:03 1997
/**
 * SECDop is the abstract class that encompasses all SECD
 * machine ops.
 *
 * @see org.jatha.compile.LispPrimitive
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public abstract class SECDop extends LispPrimitive
{
	/**
	 * @see SECDMachine
	 */
	public SECDop(final Jatha lisp, String opName)
	{
		super(lisp, opName);
	}

	/**
	 * The output of this function is printed when the
	 * instruction needs to be printed.
	 */
	public String toString()
	{
		return "SECD." + functionName;
	}


	/* --- Utility routines --- */
	public LispValue loc(long y, LispValue z)
	{
		assert z instanceof LispCons; // todo: move LispCons type to variable declaration
		
		if (y == 1)
			return(f_lisp.car(z));
		else
			return loc(y-1, f_lisp.cdr(z));
	}


  /**
   * Sets the nth position in the list to the new value.
   * @param y  an index into the list
   * @param values a list of values
   * @param newValue the new value for the index.
   */
  public void setLoc(long y, LispValue values, LispValue newValue)
  {
    if (y == 1)
      values.rplaca(newValue);
    else
      setLoc(y-1, f_lisp.cdr(values), newValue);
  }


	public LispValue getComponentAt(LispValue ij_indexes, LispValue valueList)
	{
		assert ij_indexes instanceof LispCons;	// ?
	  
		long i = ((LispInteger)(f_lisp.car(ij_indexes))).getLongValue();
		long j = ((LispInteger)(f_lisp.cdr(ij_indexes))).getLongValue();

		return loc(j, loc(i, valueList));
	}

	public void setComponentAt(LispValue ij_indexes, LispValue valueList, LispValue newValue)
	{
		assert ij_indexes instanceof LispCons;	// ?

		long i = ((LispInteger)(f_lisp.car(ij_indexes))).getLongValue();
		long j = ((LispInteger)(f_lisp.cdr(ij_indexes))).getLongValue();

		setLoc(j, loc(i, valueList), newValue);
	}
}

// The individual opcodes are in separate files.
