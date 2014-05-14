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

import java.util.Map;
import java.util.HashMap;
import java.io.*;

import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispPrimitive;
import org.jatha.exception.*;
import org.jatha.machine.SECDMachine;
import org.jatha.read.*;
import org.jatha.util.SymbolTools;
import org.jatha.Lisp;




//--------------------------------  LispSymbol  ------------------------------

// date    Wed Mar 26 11:09:38 1997
/**
 * LispSymbol implements LISP Symbols, including provisions
 * for special bindings.
 *
 * @see LispValue
 * @see LispAtom
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 * @version 1.0
 *
 */
public class StandardLispSymbol extends StandardLispAtom implements LispSymbol
{
	static long randomname = 1100;
	public StandardLispSymbol() // constructs unbound symbol with unique name
	{
		this("SYM-" + Long.toString(++randomname));
	}
/* ------------------  PRIVATE vars   ------------------------------ */

	// Every LISP symbol has these three components
	protected  LispValue    f_function;     // Function value
	protected  LispString   f_name;         // Print name
	protected  LispValue    f_value;        // Assigned value

	protected  boolean    f_isExternalInPackage = false;
	protected  boolean    f_isSpecial = false;  // Special? (dynamically-bound)
	protected  int        f_specialCount = 0;   // Number of special binding nestings

	protected Map f_documentation;

/* ------------------  CONSTRUCTORS   ------------------------------ */
	public StandardLispSymbol(String symbolName)
	{
		this(string(symbolName));
	}

	// Only 'name' is required to create a symbol.
	public StandardLispSymbol(LispString symbolNameString)
	{
		f_name       = symbolNameString;
		f_value      = null;              // Default to UNBOUND
		f_function   = null;              // Default to UNBOUND

		// If the symbol contains lower-case letters, or anything other than
		// the following set of letters, we need to print OR-bars around it.

		f_documentation = new HashMap();
	}

/* ------------------  BASIC (non-LISP) methods   ------------------------------ */


  // 'equals' is required for HashTable.
  public boolean equals(LispSymbol otherSymbol)
  {

    return (this == otherSymbol);   // Same symbols have same address
  }

  /**
   * Returns the Java String representing the symbol's name, without any quoting.
   */
  public String getName()
  {
    return f_name.getValue();
  }

  public void internal_prin1(PrintStream os)
  {
    os.print(toString());
  }

  // PRINC doesn't print the package name.
  public void internal_princ(PrintStream os) { os.print(f_name.getValue()); }

  public void internal_print(PrintStream os)
  {
    os.print(toString());
  }

  /**
   * Returns this symbol as a string, including the package.
   */
  public String toString()
  {
	return f_name.getValue();
  }


  /**
   * Strips or-bars from a LispSymbol name.
   * Leaves a colon on the front of keyword symbols.
   */
  public String toStringSimple()
  {
    // if (this instanceof LispKeyword)    // would this work?
//    if (this instanceof LispKeyword)
//      return ":" + f_name.getValue();
//    else
      return f_name.getValue();
  }

  //  ****  Handling special (dynamically-bound) variables   *********

  public void set_special(boolean value) { f_isSpecial = value; }

  public boolean specialP() { return f_isSpecial; }

  public void adjustSpecialCount(int amount) { f_specialCount = f_specialCount + amount; }

  public int get_specialCount() { return f_specialCount; }


/* ------------------  LISP methods   ------------------------------ */

  public LispValue  apply(LispValue args)
  {
    if (f_function == null)
      return super.apply(args);
    else
    {
      System.err.println("\nSorry, APPLY is not yet implemented.");

      return NIL;
    }
  }

  public boolean boundp()
  {
    return (f_value != null);
  }

  public boolean fboundp()
  {
    return (f_function != null);
  }

  public LispValue funcall(SECDMachine machine, LispValue args)
  {
    if (f_function == null)
      throw new LispValueNotAFunctionException("The first argument to FUNCALL");
    else
    {
      // push the args back on the stack
      for (LispValue v = args; v != NIL; v = cdr(v))
    	  machine.S.push(car(v));

      // get the function, and push it on the code stack.
      // Note that we don't do error checking on the number
      // of arguments.  This is bad...
      machine.C.pop();                     // Pop the funcall function off.
      machine.C.push(((LispFunction)f_function).getCode());   // Push the new one on.
    }

    return NIL;
  }

  public LispValue     pop          ()
  {
    LispValue returnValue;

    if (f_value instanceof LispList)
    {
      returnValue     = car(f_value);
      setf_symbol_value(cdr(f_value));
      return returnValue;
    }
    else
      throw new LispValueNotAListException("The value of "
                                           + f_name.toStringSimple());
  }

  public LispValue     push         (LispValue newValue)
  {
    if (f_value instanceof LispList)
    {
      setf_symbol_value(new StandardLispCons(newValue, f_value));
      return newValue;
    }
    else
      throw new LispValueNotAListException("The value of "
                                           + f_name.toStringSimple());
  }

  // Modified by Jean-Pierre Gaillardon to handle macros, April 2005
  public LispValue setf_symbol_function(LispValue newCode)
  {
    // function or macro
    if (newCode instanceof LispFunction/* && !(newCode instanceof LispMacro)*/) // ??
    {
      f_function = newCode;
      return f_function;
    }

    if (newCode instanceof LispPrimitive) // ??
    {
      f_function = newCode;
      return f_function;
    }
    
    // A macro has the symbol :MACRO as the first element.
    if (isMacroCode(newCode))
    {
      f_function = new StandardLispMacro(this, cdr(newCode));
      return f_function;
    }

    // Else, create a new function.
    f_function = new StandardLispFunction(this, newCode);
    return f_function;
  }

  public LispValue setf_symbol_value(LispValue newValue)
  {
    f_value = newValue;
    return f_value;
  }

  public LispValue setq(LispValue newValue)
  {
    f_value = newValue;
    return f_value;
  }

  /**
   * Converts a String, Symbol or Character to a string.
   */
  public LispValue string()
  {
    return string(this.toString());
  }

  public LispValue symbol_function() throws LispException
  {
    if (f_function == null)
    {
      throw new LispUndefinedFunctionException(f_name.getValue());
    }
    return f_function;
  }

  public LispString symbol_name()
  {
    return f_name;
  }

  public LispValue symbol_value() throws LispException
  {
    if (f_value == null)
    {
      throw new LispUnboundVariableException(f_name.getValue());
    }
    return f_value;
  }

	/**
	 * @param code a Lisp list
	 * @return true if code is code for a macro (the first element is :MACRO)
	 */
	public static boolean isMacroCode(LispValue code)
	{
		return (code instanceof LispList) && (Lisp.car(code) == MACRO);
	}

};

