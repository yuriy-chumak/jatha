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

package org.jatha.compile;

import org.jatha.dynatype.*;
import org.jatha.exception.CompilerException;
import org.jatha.machine.*;

// @date    Fri Jan 31 17:31:40 1997
/**
 * The LispPrimitive class makes the
 * transition from LISP code to Java code.  There
 * is a LispPrimitive for each builtin LISP function.
 *
 *  1) Create the new LISP primitive as an instance of
 *     this class.  It must have several methods as
 *     described below.
 *  2) Register the new primitive with the compiler.
 *
 * Each primitive must implement one method:
 *
 *   public void Execute(SECDMachine machine)
 *
 * @see org.jatha.compile.LispCompiler
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public abstract class LispPrimitive1 extends LispPrimitive
{
/* ------------------ CONSTRUCTORS    ------------------------------ */
	/**
	 * The constructor for the LispPrimitive class.
	 * @see org.jatha.compile.LispCompiler
	 * @param fnName The LISP function name being implemented.
	 */
	public LispPrimitive1(String fnName)
	{
		super(fnName);
	}
	
	public void Execute(SECDMachine machine)
			throws CompilerException
	{
	    LispValue arg1 = machine.S.pop();
	    LispValue result = this.Execute(arg1);
	    
		machine.S.push(result);
		machine.C.pop();
	}
	

  /**
   * This method returns <code>true</code> if
   * the list of arguments satisfies the length restrictions
   * posed by the function, and <code>false</code> otherwise.
   * @see LispPrimitive
   * @param numberOfArguments  usually the result of args.length()
   * @return boolean
   */
  boolean validArgumentLength(int numberOfArguments)
  {
	  return numberOfArguments == 1;
  }


  /**
   * This method returns a Java string denoting the length of
   * the expected argument list in some readable form.
   * <p>
   * This method is called by the compiler when an argument count
   * exception is generated.
   *
   * @see LispPrimitive
   * @see LispCompiler
   * @return a Java string denoting the length of the expected argument list.
   */
  public String parameterCountString()
  {
    return "1";
  }


	protected abstract LispValue Execute(LispValue arg)
			throws CompilerException;
}