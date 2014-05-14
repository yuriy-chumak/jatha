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
 *�
 * For further information, please contact Micheal Hewett at
 *   hewett@cs.stanford.edu
 *
 */
package org.jatha.dynatype;

import java.io.PrintStream;

import org.jatha.Lisp;
import org.jatha.exception.LispConstantRedefinedException;


// See LispValue.java for documentation



//--------------------------------  LispNil  --------------------------------
//
// This is used only for NIL.  Since NIL is both a degenerate list and
// a symbol, it causes some programming difficulties.  Using this resolves
// those problems.

public class StandardLispNIL extends StandardLispList
{
	protected StandardLispNIL() {
		// do nothing
	}
	
	public LispValue car() { return NIL; }
	public LispValue cdr() { return NIL; }
	
	
	
	// =-( unsorted )-=========================================	
	
  private  LispString name;
  private  LispValue  value;
  private  LispValue  plist;
  private  LispValue  function = null;

  public void internal_princ(PrintStream os)        { os.print("NIL"); }
  public void internal_princ_as_cdr(PrintStream os) { /* Do Nothing */ }

  public void internal_prin1(PrintStream os)        { os.print("NIL"); }
  public void internal_prin1_as_cdr(PrintStream os) { /* Do Nothing */ }

  public void internal_print(PrintStream os)        { os.print("NIL"); }
  public void internal_print_as_cdr(PrintStream os) { /* Do Nothing */ }

  // contributed by Jean-Pierre Gaillardon, April 2005
  public String toString()      { return "NIL"; }
  public String toStringAsCdr() { return ""; }


  public boolean constantp() { return true; }
  public int     basic_length()    { return 0;     }

/* ------------------  LISP functions   ------------------------------ */

  public LispValue     assoc(LispValue index)      { return NIL; }
  public boolean boundp() { return true; }
  public LispValue     butlast()                   { return NIL; }
  public LispValue     copy_list()                 { return NIL; }
  public LispValue     elt(LispValue index)        { return NIL; }

  public LispValue     first        ()  { return NIL; }
  public LispValue     second       ()  { return NIL; }
  public LispValue     third        ()  { return NIL; }
  public LispValue     fourth       ()  { return NIL; }
  public LispValue     fifth        ()  { return NIL; }
  public LispValue     sixth        ()  { return NIL; }
  public LispValue     seventh      ()  { return NIL; }
  public LispValue     eighth       ()  { return NIL; }
  public LispValue     ninth        ()  { return NIL; }
  public LispValue     tenth        ()  { return NIL; }

  public LispValue     last()                    { return NIL; }
  public int length() { return 0; }
  public LispValue     member(LispValue elt)     { return NIL; }
  public LispValue     nreverse(LispValue index) { return NIL; }

  public LispValue     rassoc(LispValue index)   { return NIL; }
  public LispValue     remove(LispValue elt)     { return NIL; }
  public LispValue     rest()                    { return NIL; }
  public LispValue     reverse(LispValue elt)    { return NIL; }
  public LispValue     rplaca(LispValue  newCar) { return NIL; }
  public LispValue     rplacd(LispValue  newCdr) { return NIL; }

  public LispValue     setf_symbol_function(LispValue newFunction) {
    throw new LispConstantRedefinedException("NIL");
  }
  public LispValue     setf_symbol_value(LispValue newValue) {
    throw new LispConstantRedefinedException("NIL");
  }

  public LispString    symbol_name()    { return null;  } // ?
  public LispValue     symbol_package() { return null;  }
  public LispValue     symbol_value()   { return this; }

  public LispValue     subst(LispValue oldValue, LispValue newValue)
  { return this; }

	public LispValue append(LispValue otherList)
	{
		return otherList;
	}
};


