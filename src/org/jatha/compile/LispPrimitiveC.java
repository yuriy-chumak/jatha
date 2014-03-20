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

// primitive with variable count of arguments
public abstract class LispPrimitiveC extends LispPrimitive
{
	// Fields
	protected long minNumberOfArgs;
	protected long maxNumberOfArgs;

	public LispPrimitiveC(String fnName, long minArgs) {
		super(fnName);
		minNumberOfArgs = minArgs;
		maxNumberOfArgs = Long.MAX_VALUE;
	}
	
	  boolean validArgumentLength(LispValue numberOfArguments)
	  {
	    long numArgs = ((LispInteger)numberOfArguments).getLongValue();

	    return ((minNumberOfArgs <= numArgs)
		    && (numArgs <= maxNumberOfArgs));
	  }
	  public String parameterCountString()
	  {
	    String result = Long.toString(minNumberOfArgs);

	    if (maxNumberOfArgs == Long.MAX_VALUE)
	      result += "...";
	    else if (maxNumberOfArgs != minNumberOfArgs)
	      result += " " + maxNumberOfArgs;

	    return result;
	  }
	  
	

	// Unlimited number of evaluated args.
	public LispValue CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue args,
			LispValue valueList, LispValue code)
			throws CompilerException
	{
		return
				compiler.compileArgsLeftToRight(args, valueList,
						cons(machine.LIS,
						     cons(args.length(), code)));
	}
	
	public void Execute(SECDMachine machine)
			throws CompilerException
	{
	    LispValue arg1 = machine.S.pop();
	    
	    LispValue result = this.Execute(arg1);
	    
		machine.S.push(result);
		machine.C.pop();
	}
	
	protected abstract LispValue Execute(LispValue arg)
			throws CompilerException;
}