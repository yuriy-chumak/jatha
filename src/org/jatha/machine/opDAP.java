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
import org.jatha.dynatype.*;



// @date    Sat Feb  1 22:18:53 1997
/**
 * opDAP optimizes the   (... AP RTN) sequence in (... DAP).
 * From Section 7.8.3 of Kogge.
 * @see SECDMachine
 * @see opAP
 * @see opRTN
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
class opDAP extends SECDop
{
  /**
   * It calls <tt>SECDop()</tt> with the machine argument
   * and the label of this instruction.
   * @see SECDMachine
   */
  //@author  Micheal S. Hewett    hewett@cs.stanford.edu
  public opDAP(Lisp lisp)
  {
    super(lisp, "DAP");
  }


  public void Execute(SECDMachine machine)
  {

    LispValue fe = machine.S.pop();   /* (f . e) */
    LispValue v  = machine.S.pop();

    /*
      printf("\nAP:   fe = "); print(fe);
      printf("\nAP:   v  = "); print(v);
      */

    machine.C.pop();  // Pop DAD instruction

    machine.C.assign(f_lisp.car(fe));
    machine.E.assign(f_lisp.makeCons(v, f_lisp.cdr(fe)));
    machine.S.assign(NIL);
  }
}
