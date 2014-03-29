package org.jatha;

import org.jatha.dynatype.*;
import
static org.jatha.dynatype.LispValue.*;

/**
 * This functions does not generate LispExceptions and assumes that all
 * agruments are correct
 */
public class LispProcessor
{
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
	public static final LispCons cons(LispValue car, LispValue cdr)
	{
		return new StandardLispCons(car, cdr);
	}
	public static final LispList list(LispValue... parts)
	{
		LispList result = NIL;
		for (int i = parts.length-1 ; i >= 0; i--)
			result = cons(parts[i], result);
		return result;
	}
}
