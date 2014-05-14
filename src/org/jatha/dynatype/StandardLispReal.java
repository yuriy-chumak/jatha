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
import java.math.BigInteger;

import org.jatha.Lisp;



//-------------------------------  LispReal  ---------------------------------

public class StandardLispReal extends StandardLispNumber implements LispReal
{
  private  double f_value;

  public void internal_print(PrintStream os) { os.print(f_value); }
  public void internal_prin1(PrintStream os) { os.print(f_value); }
  public void internal_princ(PrintStream os) { os.print(f_value); }

  public StandardLispReal(double theValue)
  {
    f_value = theValue;
  }

  public StandardLispReal()
  {
    f_value = 0.0;
  }

  public double getDoubleValue()  { return f_value; }

  public BigInteger getBigIntegerValue()
  {
    return BigInteger.valueOf((long)f_value);
  }

  public long getLongValue()
  {
    return (long)f_value;
  }



  public String toString() { return String.valueOf(f_value); }

  public LispValue eql(LispValue val)
  {
    if (val instanceof LispReal)
    {
      //System.err.println("StandardLispReal.eql: comparing " + this.f_value + " to " + ((LispReal)val).getDoubleValue());
      if (this.f_value == ((LispReal)val).getDoubleValue())
        return T;
      else
        return NIL;
    }
    else
      return super.eql(val);
  }

  public LispValue equal(LispValue val)
  {
    return eql(val);
  }

  public LispValue zerop    ()
  {
    if (f_value == 0.0)
      return T;
    else
      return NIL;
  }

};

