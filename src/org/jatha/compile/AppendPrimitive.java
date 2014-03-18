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


public class AppendPrimitive extends LispPrimitive
{
  public AppendPrimitive(Lisp lisp)
  {
    super(lisp, "APPEND", 0, Long.MAX_VALUE);
  }

  public void Execute(SECDMachine machine)
  {
    LispValue args = machine.S.pop();

    machine.S.push(appendArgs(args));
    machine.C.pop();
  }

  // This is right-recursive so it only copies each arg once.
  // The last arg is not copied, of course.
  LispValue appendArgs(LispValue args)
  {
    if (f_lisp.cdr(args) == f_lisp.NIL)
      return f_lisp.car(args);
    else
      return f_lisp.car(args).append(appendArgs(f_lisp.cdr(args)));
  }

  // Unlimited number of evaluated args.
  public LispValue CompileArgs (LispCompiler compiler, SECDMachine machine, LispValue args,
				LispValue valueList, LispValue code)
    throws CompilerException
  {
    return
      compiler.compileArgsLeftToRight(args, valueList,
				      f_lisp.makeCons(machine.LIS,
                              f_lisp.makeCons(args.length(), code)));
   }
}
