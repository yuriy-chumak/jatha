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

import org.jatha.Lisp;
import org.jatha.dynatype.*;
import org.jatha.exception.CompilerException;
import org.jatha.machine.*;


/**
 * if the function effectively evaluates itself
 * simply by compiling its argument list.  This is true for
 * functions like LIST, LIST*, and QUOTE.  This inhibits putting
 * the function call on the stack, thus saving a millisecond of time.
 *
 * @see org.jatha.compile.LispPrimitive
 */
public abstract class InlineLispPrimitive extends LispPrimitive
{
	public InlineLispPrimitive(Lisp lisp, String fnName, long minArgs) {
		super(lisp, fnName, minArgs);
	}
	public InlineLispPrimitive(Lisp lisp, String fnName, long minArgs, long maxArgs) {
		super(lisp, fnName, minArgs, maxArgs);
	}

	@Override
	public void Execute(SECDMachine machine)
			throws CompilerException
	{
		System.err.println(LispFunctionNameString() + " was compiled - shouldn't have been.");
		machine.C.pop();
	}
	
	@Override
	public LispValue CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue function,
					LispValue args, LispValue valueList, LispValue code)
			throws CompilerException
	{
		return CompileArgs(compiler, machine, args, valueList, code);
	}
}