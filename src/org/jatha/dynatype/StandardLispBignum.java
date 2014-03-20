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

//------------------------------  LispBignum  -------------------------------

/**
 * Implements BigNums - large integers.
 */
public class StandardLispBignum extends StandardLispInteger implements LispBignum
{
  private  BigInteger value;
  
  // ---  static initializer  ---

  // ---  Constructors  ---

  public StandardLispBignum(BigInteger  theValue)
  {
    value = theValue;
  }

  public StandardLispBignum(long   theValue)
  {
    value = BigInteger.valueOf(theValue);
  }


  public StandardLispBignum(double theValue)
  {
    value = BigInteger.valueOf((long)theValue);
  }


  public StandardLispBignum()
  {
    value = BIGZERO;
  }

  public double  getDoubleValue()
  {
    return value.doubleValue();
  }

  public BigInteger getBigIntegerValue()
  {
    return value;
  }

  public long getLongValue()
  {
    return value.longValue();
  }

  public void    internal_princ(PrintStream os) { os.print(value); }
  public void    internal_prin1(PrintStream os) { os.print(value); }
  public void    internal_print(PrintStream os) { os.print(value); }
  public boolean basic_bignump()   { return true; }
  public boolean basic_integerp()  { return true; }


  /**
   * Returns a Java Double, Float or Integer object,
   * depending on the typeHint.
   */
  public Object toJava(String typeHint)
  {
    if (typeHint == null)
      return toJava();

    else if (typeHint.equalsIgnoreCase("Double"))
      return new Double(getDoubleValue());

    else if (typeHint.equalsIgnoreCase("Float"))
      return new Float(getDoubleValue());

    else if (typeHint.equalsIgnoreCase("Integer"))
      return new Integer((int)(getLongValue()));

    else if (typeHint.equalsIgnoreCase("Long"))
      return new Long(getLongValue());

    else
      return value;
  }


  public String  toString() { return value.toString(); }


  // ---  LISP methods  ---

  /**
   * Bignum implementation of abs.
   */
  public LispValue abs()
  {
    if (this.value.signum() > 0)
      return this;
    else
      return bignum(this.value.negate());
  }


  public LispValue eql(LispValue val)
  {
    if (val instanceof LispBignum)
      if (value.equals(((LispBignum)val).getBigIntegerValue()))
        return T;
      else
        return NIL;

    if (val instanceof LispInteger)
    {
      LispBignum n = bignum(((LispInteger)val).getLongValue());
      if (value.equals(n.getBigIntegerValue()))
        return T;
      else
        return NIL;
    }

    if (val instanceof LispReal)
    {
      LispReal r = ((LispReal)val);

      if (StrictMath.round(r.getDoubleValue()) != r.getDoubleValue())  // If not integral
        return NIL;

      LispBignum n = bignum(r.getDoubleValue());
      if (value.equals(n.getBigIntegerValue()))
        return T;
      else
        return NIL;
    }


    return super.eql(val);
  }


  public LispValue equal(LispValue val)
  {
    return eql(val);
  }


  public LispNumber     add         (LispValue  args)
  {
    // Args is a list of numbers that has already been evaluated.
    // Terminate if we hit any non-numbers.
    BigInteger sum = this.value;
    LispValue addend;

    while (args != NIL)
    {
      // System.out.println("LispBignum.add: " + sum + " and " + args);
      addend = car(args);
      if (addend.numberp() != T)
      {
        this.add(car(args));
        return null;//(NIL);	// todo: throw exception?
      }

      // If an addend is a float, we need to convert the
      // pending result to a LispReal and add the rest of
      // the numbers as reals.
      if (addend.floatp() == T)
      {
        // Do we print a warning?
        LispReal realValue = real(this.getDoubleValue());
        return realValue.add(args);
      }

      if (addend instanceof LispBignum)
        sum = sum.add(((LispBignum)addend).getBigIntegerValue());
      else // must be an integer
        sum = sum.add(BigInteger.valueOf(((LispInteger)addend).getLongValue()));

      args = cdr(args);
    };

    if ((sum.compareTo(MAXINT) <= 0)    // If LispInteger size...
            && (sum.compareTo(MININT) >= 0))
      return (integer(sum.longValue()));
    else
      return (bignum(sum));
  }


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  /**
   * DIVIDE adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   *
   */
  public LispNumber     div      (LispValue   args)
  {
    BigInteger quotient    = this.getBigIntegerValue();
    BigInteger quotientAndRem[];
    BigInteger term_value;
    LispValue  term;
    int        argCount     = 1;

    while (args != NIL)
    {
      term = car(args);             /* Arglist is already evaluated. */
      if (term.numberp() != T)
      {
        this.div(car(args));  // generate error
        return null;//(NIL);
      }

      ++argCount;

      // If a term is a float, we need to convert the
      // pending result to a LispReal and divide the rest of
      // the numbers as reals.
      if (term.floatp() == T)
      {
        // Do we print a warning?
        LispReal realValue = real(quotient.doubleValue());
        return realValue.div(args);
      }


      // Do integer divide and check result.
      if (! (term instanceof LispBignum))
        term = bignum(((LispInteger)term).getLongValue());

      term_value = ((LispBignum)term).getBigIntegerValue();

      if (term_value.compareTo(BIGZERO) != 0)
      {
        quotientAndRem = quotient.divideAndRemainder(term_value);
        if (quotientAndRem[1].compareTo(BIGZERO) != 0)
        {
          // Won't divide evenly, so convert to a real.
          return real(quotient.doubleValue()).div(args);
        }
        else
          quotient = quotientAndRem[0];
      }
      else
      {
        System.out.print("\n;; *** ERROR: Attempt to divide by 0.\n");
        return null;//(NIL); todo: return NaN
      }

      args = cdr(args);
    }

    if (argCount == 1)           /* Have to handle n-arg differently from 1-arg */
      if (quotient.compareTo(BIGZERO) != 0)
        return (real(1.0 / quotient.doubleValue()));
      else
      {
        System.out.print("\n;; *** ERROR: Attempt to divide by 0.\n");
        return null;//(NIL);
      }


    if ((quotient.compareTo(MAXINT) <= 0)    // If LispInteger size...
            && (quotient.compareTo(MININT) >= 0))
      return (integer(quotient.longValue()));
    else
      return (bignum(quotient));
  }


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
    // 20050520 - fixed an error if args is not a list, but a single value instead.
  /**
   * MULTIPLY adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   */
  public LispNumber     mul    (LispValue  args)
  {
    BigInteger product     = this.getBigIntegerValue();
    LispValue  term,arglist;

    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    while (arglist != NIL)
    {
      term = car(arglist);

      // Generate an error if the multiplicand is not a number.
      if (term.numberp() != T)
      {
        super.mul(car(arglist));  // generates an error
        return null;//(NIL);
      }

      // If a term is a float, we need to convert the
      // pending result to a LispReal and multiply the rest of
      // the numbers as reals.
      if (term.floatp() == T)
      {
        // Do we print a warning?
        LispReal realValue = real(this.getDoubleValue());
        return realValue.mul(arglist);
      }

      if (term instanceof LispBignum)
      {
        //System.out.print("Bignum: multiplying " + this + " by Bignum " + ((LispBignum)term).getBigIntegerValue());
        product = product.multiply(((LispBignum)term).getBigIntegerValue());
        //System.out.println(", producing " + product);
      }
      else // (term instanceof LispInteger)
      {
        //System.out.print("Bignum: multiplying " + this + " by Integer " + ((LispInteger)term).getLongValue());
        product = product.multiply(BigInteger.valueOf(((LispInteger)term).getLongValue()));
        //System.out.println(", producing " + product);
      }

      arglist = cdr(arglist);
    };

    if ((product.compareTo(MAXINT) <= 0)    // If LispInteger size...
            && (product.compareTo(MININT) >= 0))
      return (integer(product.longValue()));
    else
      return (bignum(product));
  }


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  /**
   * SUBTRACT adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   */
  public LispNumber     sub         (LispValue  args)
  {
    // Args is a list of numbers that has already been evaluated.
    // Terminate if we hit any non-numbers.
    BigInteger sum = this.value.negate();
    LispValue term;
    int       argCount = 1;

    while (args != NIL)
    {
      // System.out.println("LispBignum.subtract: " + sum + " and " + args);
      term = car(args);
      if (term.numberp() != T)
      {
        this.sub(car(args));  // generate error
        return null;//(NIL);
      }

      // If a term is a float, we need to convert the
      // pending result to a LispReal and add the rest of
      // the numbers as reals.
      if (term.floatp() == T)
      {
        // Do we print a warning?
        LispReal realValue = real(this.getDoubleValue());
        return realValue.sub(args);
      }

      ++argCount;

      if (argCount == 2)
        sum = sum.negate();

      if (term instanceof LispBignum)
        sum = sum.subtract(((LispBignum)term).getBigIntegerValue());
      else // (term instanceof LispInteger)
        sum = sum.subtract(BigInteger.valueOf(((LispInteger)term).getLongValue()));

      args = cdr(args);
    };


    if ((sum.compareTo(MAXINT) <= 0)    // If LispInteger size...
            && (sum.compareTo(MININT) >= 0))
      return (integer(sum.longValue()));
    else
      return (bignum(sum));
  }



  public LispValue bignump   ()  { return T; }
  public LispValue integerp  ()  { return T; }

  public LispValue negate()
  {
    return bignum(value.negate());
  }
  

//  public LispValue type_of   ()  { return f_lisp.BIGNUM_TYPE;   }
/*  public LispValue typep(LispValue type)
  {
    LispValue result = super.typep(type);

    if ((result == T) || (type == f_lisp.BIGNUM_TYPE))
      return T;
    else
      return NIL;
  }*/

  public LispValue zerop     ()
  {
    if (value.equals(BIGZERO))
      return T;
    else
      return NIL;
  }

}