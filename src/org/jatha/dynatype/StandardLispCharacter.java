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

import java.io.PrintStream;

import org.jatha.Lisp;



//------------------------------  LispCharacter  -------------------------------

public class StandardLispCharacter extends StandardLispAtom implements LispCharacter
{
  // ----- fields  ------------

  private  char value;

  // ----- static initializer  -------------


  // ----- Constructors  ------------
  public StandardLispCharacter(char theChar)
  {
    value = theChar;
  }


  public StandardLispCharacter()
  {
    value = 'm';  // For Mike!
  }

  // ----- non-LISP methods  ------------
  public void internal_princ(PrintStream os) { os.print(value); }

  public void internal_prin1(PrintStream os) { os.print("#\\" + value); }

  public void internal_print(PrintStream os) { os.print("#\\" + value); }

  char getValue() { return value; }


  public String toString() { return String.valueOf(value); }

  public boolean constantp() { return true; }

  // ----- LISP methods  ------------

  // contributed by Jean-Pierre Gaillardon, April 2005
  public LispValue eql(LispValue val)
  {
    if (val instanceof LispCharacter)
      if (this.value == ((LispCharacter)val).getCharacterValue())
        return T;
      else
        return NIL;
    else
      return super.eql(val);
  }

  public LispValue equal(LispValue val)
  {
    return eql(val);
  }

  /**
   * Converts a String, Symbol or Character to a string.
   */
  public LispValue string()
  {
    return new StandardLispString(this.toString());
  }

  public char getCharacterValue()
  {
    return value;
  }


};
