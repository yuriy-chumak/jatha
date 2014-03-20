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

import org.jatha.Lisp;
import org.jatha.dynatype.LispValue;


// @date    Sun Apr 27 1997
/**
 * opSP_BIND binds a special variable to the topmost
 * value on the stack.  The variable name is the
 * next value on the C register.
 * @see SECDMachine
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
class opSP_BIND extends SECDop
{
  public opSP_BIND()
  {
    super("SP_BIND");
  }


  public void Execute(SECDMachine machine)
  {
    LispValue val = machine.S.pop();

    machine.C.pop();  // Pop the SP_BIND instruction
    LispValue sym = machine.C.pop();   // Pop the symbol name
    machine.special_bind(sym, val);
  }


  public LispValue grindef(LispValue code, int indentAmount)
  {
    indent(indentAmount);
    System.out.print(functionName);
    indent(1);
    code.second().internal_princ(System.out);

    NEWLINE.internal_princ(System.out);

    return cdr(cdr(code));
  }
}
