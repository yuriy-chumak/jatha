package org.jatha.extras;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.jatha.Lisp;
import org.jatha.Tests;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive1;
import org.jatha.compile.LispPrimitive2;
import org.jatha.compile.LispPrimitiveC;
import org.jatha.dynatype.LispCons;
import org.jatha.dynatype.LispList;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispValueNotANumberException;

public class MATH implements LispExtension
{
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	@Override
	public void Register(LispCompiler compiler)
	{
		// +, -, *, /, =
		compiler.Register(new LispPrimitiveC("+", 0) {
			protected LispValue Execute(LispList args) {
				if (args == NIL)
					return LispNumber.ZERO;
				LispNumber x = assertNumber(args.car());
				return x.add(args.cdr());
			}
		});
		compiler.Register(new LispPrimitiveC("-", 1) {
			protected LispValue Execute(LispList args) {
				LispNumber x = assertNumber(args.car());
				return x.sub(args.cdr());
			}
		});
		compiler.Register(new LispPrimitiveC("*", 0) {
			protected LispValue Execute(LispList args) {
				if (args == NIL)
					return LispNumber.ONE;
				LispNumber x = assertNumber(args.car());
				return x.mul(args.cdr());
			}
		});
		compiler.Register(new LispPrimitiveC("/", 1) {
			protected LispValue Execute(LispList args) {
				LispNumber x = assertNumber(args.car());
				return x.div(args.cdr());
			}
		});
		
		compiler.Register(new LispPrimitiveC("=", 2) {
			protected LispValue Execute(LispList args) {
			    if (args == NIL)
			        return T;
			    else {
			    	// There should be at least 2 arguments.
			    	LispValue first = args.car();
			    	for (Iterator<LispValue> iterator = args.cdr().iterator(); iterator.hasNext();)
			    	{
			    		LispValue arg = iterator.next();
			    		if (first.equalNumeric(arg) == NIL)
			    			return NIL;
			    	}
			    	return T;
			    }
			}			
		});
		
		compiler.Register(new LispPrimitiveC("<", 2) {
			@Override
			protected final LispValue Execute(final LispList args) {
				LispValue a = args.car();
				for (final Iterator<LispValue> i = args.cdr().iterator(); i.hasNext(); ) {
					LispValue b = i.next();
					if (a.lessThan(b) == NIL)
						return NIL;
					a = b;
				}
				return T;
			}
		});
		compiler.Register(new LispPrimitiveC(">", 2) {
			@Override
			protected final LispValue Execute(final LispList args) {
				LispValue a = args.car();
				for (final Iterator<LispValue> i = args.cdr().iterator(); i.hasNext(); ) {
					LispValue b = i.next();
					if (a.greaterThan(b) == NIL)
						return NIL;
					a = b;
				}
				return T;
			}
		});
		
		
		// mod, floor, power
		compiler.Register(new LispPrimitive2("MOD") {
			protected LispValue Execute(LispValue arg, LispValue arg2) {
				return assertNumber(arg).mod(arg2);
			}			
		});
		compiler.Register(new LispPrimitive1("FLOOR") {
			protected LispValue Execute(LispValue arg) {
				return assertNumber(arg).floor();
			}			
		});
		compiler.Register(new LispPrimitive2("POWER") {
			protected LispValue Execute(LispValue arg, LispValue n) {
				return assertNumber(arg).power(
						assertNumber(n)
				);
			}			
		});
		
		// 
	}
}