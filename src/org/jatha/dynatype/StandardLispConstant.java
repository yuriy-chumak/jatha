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

import org.jatha.exception.LispConstantRedefinedException;

// See LispValue.java for documentation



//------------------------------  LispConstant  ------------------------------

// The value of a constant can't be changed.
// We allow the value to be set only if it is unbound.

public class StandardLispConstant extends StandardLispSymbol
	implements LispConstant
{
	// CONSTRUCTORS
	public StandardLispConstant(String symbolName)
	{
		super(symbolName);
	}
	
	public StandardLispConstant(LispString symbolNameString)
	{
		super(symbolNameString);
	}

	public StandardLispConstant(String symbolName, LispValue value)
	{
		super(symbolName);
		f_value = value;
	}

	public StandardLispConstant(LispString symbolNameString, LispValue value)
	{
		super(symbolNameString);
		f_value = value;
	}

	// Used for turning a symbol into a constant.
	public StandardLispConstant(LispSymbol oldSymbol)
	{
		super(oldSymbol.symbol_name());

		if (oldSymbol.boundp())
			f_value    = oldSymbol.symbol_value();

		if (oldSymbol.fboundp())
			f_function = oldSymbol.symbol_function();
	}


  // ------  BASIC (non-LISP) methods  --------

  public boolean constantp() { return true; }


 
  public LispValue setf_symbol_value(LispValue newValue)
  {
    if (f_value == null)
      return f_value = newValue;
    else
      throw new LispConstantRedefinedException(f_name.getValue());
  }

};

