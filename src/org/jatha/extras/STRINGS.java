package org.jatha.extras;

import java.util.Iterator;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.CompilerException;
import org.jatha.compile.ComplexLispPrimitive;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.NStringCapitalizePrimitive;
import org.jatha.compile.NStringDowncasePrimitive;
import org.jatha.compile.NStringUpcasePrimitive;
import org.jatha.compile.ReadFromStringPrimitive;
import org.jatha.compile.StringCapitalizePrimitive;
import org.jatha.compile.StringDowncasePrimitive;
import org.jatha.compile.StringEndsWithPrimitive;
import org.jatha.compile.StringEqPrimitive;
import org.jatha.compile.StringEqualPrimitive;
import org.jatha.compile.StringGreaterThanOrEqualPrimitive;
import org.jatha.compile.StringGreaterThanPrimitive;
import org.jatha.compile.StringGreaterpPrimitive;
import org.jatha.compile.StringLeftTrimPrimitive;
import org.jatha.compile.StringLessThanOrEqualPrimitive;
import org.jatha.compile.StringLessThanPrimitive;
import org.jatha.compile.StringLesspPrimitive;
import org.jatha.compile.StringNeqPrimitive;
import org.jatha.compile.StringNotGreaterpPrimitive;
import org.jatha.compile.StringNotLesspPrimitive;
import org.jatha.compile.StringRightTrimPrimitive;
import org.jatha.compile.StringStartsWithPrimitive;
import org.jatha.compile.StringTrimPrimitive;
import org.jatha.dynatype.LispPackage;
import org.jatha.dynatype.LispString;
import org.jatha.dynatype.LispUndefinedFunctionException;
import org.jatha.dynatype.StandardLispString;
import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispValue;
import org.jatha.machine.SECDMachine;

public class STRINGS implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();

		/**
		 * Concatenate a string to another string.
		 * Passing in any LispValue causes it to be converted to a string
		 * and concatenated to the end.
		 * This returns a new LispString.
		 */
		compiler.Register(new ComplexLispPrimitive(f_lisp, "CONCATENATE", 1, Long.MAX_VALUE) {
			// First argument should be 'STRING
			// Apply concatenate to the next argument.
			public LispValue Execute(LispValue args) {
				LispValue concatType = args.car();
				if (!concatType.toStringSimple().equalsIgnoreCase("string"))
					throw new LispUndefinedFunctionException("The first argument to Concatenate (" + concatType + ") must be the symbol STRING. Use 'string.");
				args = args.cdr();
				
				if (args.basic_length() == 0)
					return f_lisp.makeString("");
				
				StringBuffer buff = new StringBuffer(args.basic_length() * 5);

				Iterator<LispValue> valuesIt = args.iterator();
				while (valuesIt.hasNext())
				{
					LispValue value = valuesIt.next();
					if (value instanceof LispString)
						buff.append(value.toStringSimple());
					else
						buff.append(value.toString());
				}
				return new StandardLispString(f_lisp, buff.toString());
				
/*				if (args.basic_length() > 1)
					return args.second().concatenate(f_lisp.makeCons(args.car(), args.cdr().cdr()));
				return f_lisp.makeString("");*/
			}
		}, SYSTEM_PKG);
		
	    compiler.Register(new NStringCapitalizePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new NStringDowncasePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new NStringUpcasePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new ReadFromStringPrimitive(f_lisp),SYSTEM_PKG);
	    
	    compiler.Register(new StringDowncasePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringCapitalizePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringEndsWithPrimitive(f_lisp),SYSTEM_PKG);
	    
	    compiler.Register(new StringEqualPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringEqPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringNeqPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringLessThanPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringLesspPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringGreaterThanPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringGreaterpPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringLessThanOrEqualPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringGreaterThanOrEqualPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringNotLesspPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringNotGreaterpPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringStartsWithPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringTrimPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringLeftTrimPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new StringRightTrimPrimitive(f_lisp),SYSTEM_PKG);
	    
	}
}
