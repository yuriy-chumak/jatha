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

import org.jatha.Lisp;
import org.jatha.exception.*;

import java.util.Iterator;


// @date    Thu Mar 27 13:29:37 1997
/**
 * LispNumber is an abstract class that implements
 * the Common LISP NUMBER type.  It contains the
 * definitions of add, subtract, multiply and divide.
 *
 *
 * @see LispValue
 * @see LispAtom
 * @see LispInteger
 * @see LispReal
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
abstract public class StandardLispNumber extends StandardLispAtom implements LispNumber
{
	public StandardLispNumber() { }
	
  public boolean constantp()  { return true; }

	// ---  non-LISP methods  ---

	public abstract double getDoubleValue();

	// ---- LISP functions -------------
	/**
	 * Default implementation of abs.
	 */
	public LispNumber abs()
	{
		if (this.getDoubleValue() > 0.0)
			return this;
		else
			return real(this.getDoubleValue() * -1.0);
	}
	
	/**
	 * ADD adds any combination of real or integer numbers.
	 * May create a Bignum or signal floating-point overflow
	 * if necessary.
	 *
	 * @see LispReal
	 * @see LispInteger
	 */
	public LispNumber add(LispValue  args)
	{
		// The list of numbers has already been evaluated.
		// Terminate if we hit any non-numbers.
		// Keep the sum in a Long value until the value
		// either overflows, in which case we turn it into
		// a bignum, or else a real value is added, in which
		// case the result is a double.

		double    d_sum  = this.getDoubleValue();
		long      l_sum  = 0;
		long      l_addend = 0;
		LispNumber addend;
		LispValue arglist;
		boolean   allIntegers = this instanceof LispInteger;

		if (allIntegers)
			l_sum = this.getLongValue();

	    // Make sure the argument is a list of numbers.
		arglist = args;
		if (! (arglist instanceof LispList))
			arglist = list(arglist);

		while (arglist != NIL)
		{
			addend = assertNumber(car(arglist));

			// Might need to convert to a double
			if (allIntegers && (addend instanceof LispReal))
			{
				allIntegers = false;
				d_sum = l_sum;
			}

	      // Might need to convert to a Bignum
	      // a>0, b>0 : (MAX - a) < b
	      // a<0, b<0 : (MIN - a) > b

	      if (allIntegers)
	      {
	        if (addend instanceof LispBignum)
	        {
	          // System.out.println("Bignum arg...converting " + l_sum + " to bignum.");
	          LispBignum bn_val = bignum(l_sum);
	          return bn_val.add(arglist);
	        }

	        l_addend = ((LispInteger)addend).getLongValue();

	        if ((l_sum > 0) && (l_addend > 0))
	        {
	          // System.out.println("Comparing " + l_sum + " to " + Long.MAX_VALUE);

	          if ((Long.MAX_VALUE - l_sum) < l_addend)
	          // Need to convert to bignum
	          {
	            // System.out.println("Converting " + l_sum + " to bignum.");
	            LispBignum bn_val = bignum(l_sum);
	            return bn_val.add(arglist);
	          }
	        }
	        else if ((l_sum < 0) && (l_addend < 0))
	        {
	          // System.out.println("Comparing " + l_sum + " to " + Long.MIN_VALUE);
	          if ((Long.MIN_VALUE - l_sum) > l_addend)
	          {
	            // Need to convert to bignum
	            // System.out.println("Converting " + l_sum + " to bignum.");
	            LispBignum bn_val = bignum(l_sum);
	            return bn_val.add(arglist);
	          }
	        }
	      }

	      // If not allIntegers, result is a double.

	      if (allIntegers)
	        l_sum += l_addend;
	      else
	        if (addend instanceof LispReal)
	          d_sum += ((LispReal)addend).getDoubleValue();
	        else if (addend instanceof LispBignum)
	          d_sum += ((LispBignum)addend).getDoubleValue();
	        else
	          d_sum += ((LispInteger)addend).getLongValue();

	      arglist = cdr(arglist);
	    };

	    if (allIntegers)
	      return(integer(l_sum));
	    else
	      return(real(d_sum));
	  }
	
  
  // contributed by Jean-Pierre Gaillardon, April 2005

  /**
   * Converts a numeric value from degrees to radians.
   * @return The value in radians.
   */
  public LispValue degreesToRadians()
  {
    return real(this.getDoubleValue() * StrictMath.PI / 180.0);
  }

  /**
   * Converts a numeric value from radians to degrees.
   * @return The value in degrees.
   */
  public LispValue radiansToDegrees()
  {
    return real(this.getDoubleValue() * 180.0 / StrictMath.PI);
  }

	/**
	 * Calculate the object raised to the power of n.
	 */
	public LispNumber power(LispNumber n)
	{
		boolean allIntegers = (this instanceof LispInteger && n instanceof LispInteger);
		if (allIntegers)
			return bignum(getBigIntegerValue().pow((int) n.getLongValue()));
		else
			return real(StrictMath.pow(getDoubleValue(), n.getDoubleValue()));
	}

  /**
   * Calculate the object raised to the power of n.
   */
  public LispValue mod(LispValue n)
  {
    boolean allIntegers = (this instanceof LispInteger && n instanceof LispInteger);
    if (n instanceof LispNumber)
      if (allIntegers)
      {
        return bignum(getBigIntegerValue().mod(java.math.BigInteger.valueOf(((LispNumber)n).getLongValue())));
      }
      else
      {
        throw new LispValueNotANumberException("Must be integers to mod");
      }
    else
      throw new LispValueNotANumberException("The second argument to expt (" + n + ")");
  }
  
  /**
   * Returns the max of this number and its arguments, which may
   * be a list of numbers or a single number.
   * @param args a number or a list of numbers
   * @return the maximum numeric value.
   */
  public LispValue max(LispValue args)
  {
    // The list of numbers has already been evaluated.
    // Terminate if we hit any non-numbers.
    // Keep the max in a double until the value
    // either overflows, in which case we turn it into
    // a bignum

    double    maxValue    = this.getDoubleValue();
    double    otherValue  = 0;
    boolean   allIntegers = this instanceof LispInteger;
    LispValue arglist;

    //System.err.println("Checking max of " + this + " and " + args);
    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    while (arglist != NIL)
    {
      LispNumber arg = assertNumber(car(arglist));

      // Keep a flag to see whether the final value
      // should be an integer or not.
      if (allIntegers)
        if (! (arg instanceof LispInteger))
          allIntegers = false;

      // Do the MAX function.
      otherValue = ((LispNumber)arg).getDoubleValue();
      if (otherValue > maxValue)
        maxValue = otherValue;

      arglist = cdr(arglist);
      //System.err.println("  max value is " + maxValue);
    }

    if (allIntegers)
      return(integer((long)maxValue));
    else
      return(real(maxValue));
  }

  /**
   * Returns the min of this number and its arguments, which may
   * be a list of numbers or a single number.
   * @param args a number or a list of numbers
   * @return the minimum numeric value.
   */
  public LispValue min(LispValue args)
  {
    // The list of numbers has already been evaluated.
    // Terminate if we hit any non-numbers.
    // Keep the min in a double until the value
    // either overflows, in which case we turn it into
    // a bignum

    double    minValue    = this.getDoubleValue();
    double    otherValue  = 0;
    boolean   allIntegers = (this instanceof LispInteger);
    LispValue arglist;

    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    while (arglist != NIL)
    {
        LispNumber arg = assertNumber(car(arglist));

      // Keep a flag to see whether the final value
      // should be an integer or not.
      if (allIntegers)
        if (! (arg instanceof LispInteger))
          allIntegers = false;

      // Do the Min function.
      otherValue = ((LispNumber)arg).getDoubleValue();
      if (otherValue < minValue)
        minValue = otherValue;

      arglist = cdr(arglist);
    }

    if (allIntegers)
      return(integer((long)minValue));
    else
      return(real(minValue));
  }


  /**
   * Returns the negative of the given number.
   */
  public LispNumber negate()
  {
    return this.sub(NIL);  // Returns the negative.
  }

  /**
   * Computes 1/x of the given number.  Only valid for numbers.
   * @return a LispReal
   */
  public LispValue reciprocal()
  {
    return real(1.0 / getDoubleValue());
  }


/* ------------------  Arithmetic functions   ------------------------------ */

  // Added bignums:  20 May 1997 (mh)
  // detecting bignums:
  //   HI = java.lang.Long.MAX_VALUE
  //   LO = java.lang.Long.MIN_VALUE
  //   a + b:
  //     a>0, b>0  if (HI - a) < b  ==> use Bignum
  //     a>0, b<0  or a<0, b>0: won't overflow
  //     a<0, b<0  if (LO - a) > b  ==> use Bignum
  //
  //   a - b:
  //     a>0, b<0  if (HI + b) < a  ==> use Bignum
  //     a<0, b>0  if (LO + b) > a  ==> use Bignum
  //     a>0, b>0 : won't overflow.
  //     a<0, b<0 : won't overflow.
  //
  //   a * b:
  //     let (a * b) => X.  If (X / a) == b, okay, else overflow.  Hmmm, could be expensive.
  //




  /**
   * DIVIDE adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   * @see LispBignum
   */
  public LispNumber     div      (LispValue   args)
  {
    double    d_quotient = 0.0;
    long      l_quotient = 0, term_value;
    boolean   allIntegers  = this instanceof LispInteger;
    LispNumber term;
    int       argCount     = 1;
    LispValue arglist      = null;


    // This object is either a Real or an Integer.
    // If dividing by a Bignum, the result will be
    // less than one, so it will necessarily be a double.
    // So we don't have to worry about converting the result
    // to bignums unless the first argument is a bignum.

    if (!allIntegers)
      d_quotient = this.getDoubleValue();
    else
      l_quotient = this.getLongValue();

    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    while (arglist != NIL)
    {
      term = assertNumber(car(arglist));             /* Arglist is already evaluated. */

      ++argCount;

      if (allIntegers &&
    		  (term instanceof LispReal || term instanceof LispBignum))
      {
        allIntegers = false;
        d_quotient  = l_quotient;
      }

      if (term.zerop() == NIL)
      {
        if (!allIntegers)
        {
          if (term instanceof LispBignum)
            d_quotient = d_quotient
              / ((LispBignum)term).getDoubleValue();
          else if (term instanceof LispReal)
            d_quotient = d_quotient / ((LispReal)term).getDoubleValue();
          else
            d_quotient = d_quotient / ((LispInteger)term).getLongValue();
        }
        else // do integer divide and check result.
        {
          term_value = ((LispInteger)term).getLongValue();
          d_quotient = (double)l_quotient / (double)term_value;
          if (StrictMath.round(d_quotient) == d_quotient)
            l_quotient = l_quotient / term_value;
          else
            allIntegers = false;
        }
      }
      else
      {
        System.out.print("\n;; *** ERROR: Attempt to divide by 0.\n");
        return null;//(NIL);
      }

      arglist = cdr(arglist);
    }

    if (argCount == 1)        /* Have to handle n-arg differently from 1-arg */
      if (allIntegers && (l_quotient != 0))
      {
        allIntegers = false;
        d_quotient = 1.0 / l_quotient;
      }
      else if (!allIntegers && (d_quotient != 0))
        d_quotient = 1.0 / d_quotient;
      else
      {
        System.out.print("\n;; *** ERROR: Attempt to divide by 0.\n");
        return null;//(NIL);
      }

    if (allIntegers)
      return(integer(l_quotient));
    else if (d_quotient == (long)d_quotient)
      return(integer((long)d_quotient));
    else
      return(real(d_quotient));
  }


  /**
   * MULTIPLY adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   */
  public LispNumber     mul    (LispValue  args)
  {
    double     d_product     = this.getDoubleValue();
    long       l_product     = 0;
    long       l_term, l_result = 0;
    boolean    allIntegers = this instanceof LispInteger;
    LispValue  term, arglist;

    // Is this number zero?
    if (this.zerop() == T)
      return this;

    if (allIntegers)
      l_product = this.getLongValue();

    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    // Keep a pointer into the arglist because we may
    // have to send the remainder to BigNum to process
    LispValue ptr = arglist;

    for (Iterator<LispValue> iterator = arglist.iterator(); iterator.hasNext();)
    {
    	term = assertNumber(iterator.next());

      // Multiplying by one?
      if (term.equals(ONE))
      {
        ptr = cdr(ptr);
        continue;
      }

      // Multiplying by zero?
      if (term.zerop() == T)
        return ZERO;


      // Convert to a double if the next term is a floating-point number.
      if (allIntegers && term instanceof LispReal)
      {
        allIntegers = false;
        d_product = l_product;
      }

      // Might need to convert to a Bignum if the
      // next term is a BigNum, or if the product will
      // overflow the Long value.
      //
      //   a * b:
      //     let (a * b) => X.  If (X / a) == b, okay, else overflow.

      if (allIntegers)
      {
        if (term instanceof LispBignum)
        {
          LispBignum bn_val = bignum(l_product);
          return bn_val.mul(ptr);
        }

        l_term = ((LispInteger)term).getLongValue();

        // May need to convert to a bignum.
        // if ((l_result != 0) && ((l_result / l_term) != l_product))
        if ((Long.MAX_VALUE / l_product) < l_term)
        {
          // Need to convert to bignum
          //System.out.println("Converting " + l_product + " to bignum and multiplying by " + ptr);
          LispBignum bn_val = bignum(l_product);
          return bn_val.mul(ptr);
        }
        else
          l_result = l_product * l_term;
      }

      if (allIntegers)
        l_product =  l_result;
      else
        if (term instanceof LispReal)
          d_product *=  ((LispReal)term).getDoubleValue();
        else if (term instanceof LispBignum)
          d_product *= ((LispBignum)term).getDoubleValue();
        else
          d_product *= ((LispInteger)term).getLongValue();

      ptr = cdr(ptr);
    }

    if (allIntegers)
      return(integer(l_product));
    else if (d_product == (long)d_product)
      return(integer((long)d_product));
    else
      return(real(d_product));
  }


  /**
   * SUBTRACT adds any combination of real or integer numbers.
   *
   * @see LispReal
   * @see LispInteger
   */
  public LispNumber     sub    (LispValue  args)
  {
    // The list of numbers has already been evaluated.
    // Terminate if we hit any non-numbers.
    // Keep the sum in a Long value until the value
    // either overflows, in which case we turn it into
    // a bignum, or else a real value is added, in which
    // case the result is a double.

    double    d_sum  = - this.getDoubleValue();
    long      l_sum  = 0;
    long      l_addend = 0;
    LispNumber addend;
    LispValue arglist;
    boolean   allIntegers = this instanceof LispInteger;
    int       argCount = 1;

    //System.out.println("LispNumber.subtract: this = " + this + ", args = " + args);
    //todo: StandardLispNumber.subtract: this won't work with BigNums
    if (allIntegers)
      l_sum = - this.getLongValue();

    // Make sure the argument is a list of numbers.
    arglist = args;
    if (! (arglist instanceof LispList))
      arglist = list(arglist);

    while (arglist != NIL)
    {
      addend = assertNumber(car(arglist));

      // Might need to convert to a double
      if (allIntegers && addend instanceof LispReal)
      {
        allIntegers = false;
        d_sum = l_sum;
      }

      ++argCount;

      if (argCount == 2)
      {
        l_sum = - l_sum;
        d_sum = - d_sum;
      }

      // Might need to convert to a Bignum
      //
      //   a - b:
      //     a>0, b<0  if (HI + b) < a  ==> use Bignum
      //     a<0, b>0  if (LO + b) > a  ==> use Bignum

      if (allIntegers)
      {
        if (addend instanceof LispBignum)
        {
          // System.out.println("Bignum arg (sub)...converting " + l_sum + " to bignum.");
          LispBignum bn_val = bignum(l_sum);
          return bn_val.sub(arglist);
        }

        l_addend = ((LispInteger)addend).getLongValue();

        if ((l_sum > 0) && (l_addend < 0))
        {
          if ((Long.MAX_VALUE + l_addend) < l_sum)
          // Need to convert to bignum
          {
            // System.out.println("Converting " + l_sum + " to bignum.");
            LispBignum bn_val = bignum(l_sum);
            return bn_val.sub(arglist);
          }
        }
        else if ((l_sum < 0) && (l_addend > 0))
        {
          if ((Long.MIN_VALUE + l_addend) > l_sum)
          {
            // Need to convert to bignum
            // System.out.println("Converting " + l_sum + " to bignum.");
            LispBignum bn_val = bignum(l_sum);
            return bn_val.sub(arglist);
          }
        }
      }

      // If not allIntegers, result is a double.

      if (allIntegers)
        l_sum -= l_addend;
      else
        if (addend instanceof LispReal)
          d_sum -= ((LispReal)addend).getDoubleValue();
        else if (addend instanceof LispBignum)
          d_sum -= ((LispBignum)addend).getDoubleValue();
        else
          d_sum -= ((LispInteger)addend).getLongValue();

      arglist = cdr(arglist);
    };

    if (allIntegers)
      return(integer(l_sum));
    else
      return(real(d_sum));
  }


  /**
   * Returns the smallest integer greater than or equal to the input value.
   */
  public LispValue ceiling()
  {
    if (this instanceof LispInteger)
      return this;
    else if (this.getDoubleValue() > Long.MAX_VALUE)
      return bignum(StrictMath.floor(getDoubleValue()));
    else
      return integer((long)StrictMath.ceil(getDoubleValue()));
  }


  /**
   * Returns the largest integer less than or equal to the input value.
   */
  public LispValue floor()
  {
    if (this instanceof LispInteger)
      return this;
    else if (this.getDoubleValue() > Long.MAX_VALUE)
      return bignum(StrictMath.floor(getDoubleValue()));
    else
      return integer((long)Math.floor(getDoubleValue()));
  }

  public LispValue sqrt()
  {
    return real(StrictMath.sqrt(getDoubleValue()));
  }

  public LispValue greaterThan(LispValue arg)
  {
    if (arg instanceof LispNumber)
      if (this.getDoubleValue() > ((LispNumber)arg).getDoubleValue())
        return T;
      else
        return NIL;
    else
      throw new LispValueNotANumberException("> " + arg);
  }

  public LispValue greaterThanOrEqual(LispValue arg)
  {
    if (arg instanceof LispNumber)
      if (this.getDoubleValue() >= ((LispNumber)arg).getDoubleValue())
        return T;
      else
        return NIL;
    else
      throw new LispValueNotANumberException(">= " + arg);
  }

  public LispValue lessThan(LispValue arg)
  {
    if (arg instanceof LispNumber)
      if (this.getDoubleValue() < ((LispNumber)arg).getDoubleValue())
        return T;
      else
        return NIL;
    else
      throw new LispValueNotANumberException("< " + arg);
  }

  public LispValue lessThanOrEqual(LispValue arg)
  {
    if (arg instanceof LispNumber)
      if (this.getDoubleValue() <= ((LispNumber)arg).getDoubleValue())
        return T;
      else
        return NIL;
    else
      throw new LispValueNotANumberException("<= " + arg);
  }

  public LispValue equalNumeric(LispValue arg)
  {
    //System.err.println("StandardLispNumber.eql: comparing " + this.getDoubleValue() + " to " +
    //                   ((LispNumber)arg).getDoubleValue());

    if (arg instanceof LispNumber)
      if (this.getDoubleValue() == ((LispNumber)arg).getDoubleValue())
        return T;
      else
        return NIL;
    else
      throw new LispValueNotANumberException("= " + arg);
  }

  public LispValue eql(LispValue arg)
  {
    if (arg instanceof LispNumber)
      return equalNumeric(arg);
    else
      return NIL;
  }


}

