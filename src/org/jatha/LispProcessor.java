package org.jatha;

import java.math.BigInteger;

import org.jatha.dynatype.*;

import
static org.jatha.dynatype.LispValue.*;

/**
 * This functions does not generate LispExceptions and assumes that all
 * agruments are correct
 */
public class LispProcessor
{
	/**
	 * This functions does not generate LispExceptions and assumes that all
	 * agruments are correct
	 */
	
	public static final LispValue car(LispValue arg)
	{
		return ((LispList)arg).car();
	}
	public static final LispValue cdr(LispValue arg) 
	{
		return ((LispList)arg).cdr();
	}
	public static final LispValue nth(long i, LispCons arg)
	{
		while (--i > 0)
			arg = (LispCons)arg.cdr();
		return arg.car();
	}
	public static LispValue nth(LispCons ij, LispCons arg)
	{
		long i = ((LispInteger)(car(ij))).getLongValue();
		long j = ((LispInteger)(cdr(ij))).getLongValue();
		
		arg = (LispCons)Lisp.nth(i, arg);
		while (--j > 0)
			arg = (LispCons)arg.cdr();
		
		return arg;
	}
	
	public static final LispValue cddr(LispValue arg) 
	{
		return cdr(cdr(arg));
	}
	
	
	public static final LispCons cons(LispValue car, LispValue cdr)
	{
		return new StandardLispCons(car, cdr);
	}
	public static final LispCons variable(LispValue car, LispValue cdr)
	{
		return new StandardLispVariable(car, cdr);
	}
	public static final LispList list(LispValue... parts)
	{
		LispList result = NIL;
		for (int i = parts.length-1 ; i >= 0; i--)
			result = cons(parts[i], result);
		return result;
	}
	
	// constructors
	public static final LispSymbol symbol(String name)
	{
		return new StandardLispSymbol(name);
	}
	
	public static final LispString string(String str)
	{
		return new StandardLispString(str);
	}
	
	public static final LispNumber number(long value)
	{
		return new StandardLispInteger(value);
	}
	public static final LispNumber number(float value)
	{
		return new StandardLispReal(value);
	}
	public static final LispNumber number(BigInteger value)
	{
		return new StandardLispBignum(value);
	}
	
	

	public static final LispInteger integer(Long value)
	{
		return new StandardLispInteger(value.longValue());
	}

	public static final LispInteger integer(long value)
	{
		return new StandardLispInteger(value);
	}

	public static final LispInteger integer(Integer value)
	{
		return new StandardLispInteger(value.longValue());
	}

	public static final LispInteger integer(int value)
	{
		return new StandardLispInteger(value);
	}
	
	/**
	 * Creates a LispBignum type initialized with the value provided.
	 * @see LispBignum
	 * @see java.math.BigInteger
	 */
	public static final LispBignum bignum(BigInteger value)
	{
		return new StandardLispBignum(value);
	}

	public static final LispBignum bignum(LispInteger value)
	{
		return new StandardLispBignum(BigInteger.valueOf(value.getLongValue()));
	}

	public static final LispBignum bignum(double value)
	{
		return new StandardLispBignum(BigInteger.valueOf((long) value));
	}

	public static final LispBignum bignum(long value)
	{
		return new StandardLispBignum(BigInteger.valueOf(value));
	}

	public static final LispInteger integer()
	{
		return new StandardLispInteger(0);
	}
	
}