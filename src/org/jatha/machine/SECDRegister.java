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

package org.jatha.machine;

import org.jatha.dynatype.*;
import org.jatha.exception.LispException;
import org.jatha.exception.LispUnboundVariableException;
import org.jatha.exception.LispValueNotAListException;
import org.jatha.Lisp;
import org.jatha.LispProcessor;

import
static org.jatha.dynatype.LispValue.*;

// @date    Mon Feb  3 19:02:37 1997
/**
 * SECDRegister is essentially a Stack, with
 * an additional <tt>assign()</tt> method
 * which resets the value, and a <tt>value()</tt>
 * method which retrieves the whole stack.
 * 
 * Due to the problems with LispValue.pop(), this
 * is not the stack itself, but contains a Symbol
 * whose value is the stack.
 *
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public class SECDRegister extends LispProcessor
{
	private static int count = 1000;
	public SECDRegister(String name)
	{
		f_name = "*REGISTER-" + ++count + "*";

		assign(NIL);
	}

	protected String f_name;           // Print name
	protected LispList f_value;        // Assigned value
	
	public LispValue push(LispValue newValue)
	{
		f_value = cons(newValue, f_value);
		return newValue;
	}
	
	public LispValue pop()
	{
		LispValue returns = car(f_value);
		f_value = (LispList)cdr(f_value);
		return returns;
	}
	
	
	public void assign(LispValue newValue)
	{
		f_value = (LispList)newValue;
//		this.setf_symbol_value(newValue);
	}

	public LispValue value()
	{
		return f_value;
	}
	
/*	protected LispValue setf_symbol_value(LispValue newValue)
	{
		f_value = (LispList)newValue;
		return f_value;
	}
	protected LispValue symbol_value() throws LispException
	{
		return f_value;
	}*/
	
	@Override
	public String toString()
	{
		if (value() != null)
			return value().toString();
		return f_name;
	}
}