package org.jatha.extras;

import java.util.Iterator;

import org.jatha.Lisp;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive1;
import org.jatha.compile.LispPrimitive2;
import org.jatha.compile.LispPrimitiveC;
import org.jatha.dynatype.LispList;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispString;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.dynatype.StandardLispString;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispUndefinedFunctionException;
import org.jatha.exception.LispValueNotANumberException;

public class Strings implements LispExtension
{
	@Override
	public void Register(LispCompiler compiler)
	{
		compiler.Register(new LispPrimitive1("STRING") {
			protected LispValue Execute(LispValue a) {
				return a.string();
			}});
		
		compiler.Register(new LispPrimitive2("STRING-EQUAL") {
			protected LispValue Execute(LispValue a, LispValue b) {
				return a.stringEqual(b);
			}});
		
		
		/**
		 * Concatenate a string to another string.
		 * Passing in any LispValue causes it to be converted to a string
		 * and concatenated to the end.
		 * This returns a new LispString.
		 */
		compiler.Register(new LispPrimitiveC("CONCATENATE", 1) {
			// First argument should be 'STRING
			// Apply concatenate to the next argument.
			protected LispValue Execute(LispList args) {
				LispValue concatType = args.car();
				if (!concatType.toStringSimple().equalsIgnoreCase("string"))
					throw new LispUndefinedFunctionException("The first argument to Concatenate (" + concatType + ") must be the symbol STRING. Use 'string.");
				
				args = (LispList)args.cdr();
				if (args.basic_length() == 0)
					return string("");
				
				StringBuffer buff = new StringBuffer(args.basic_length() * 5);

				Iterator<LispValue> valuesIt = args.iterator();
				while (valuesIt.hasNext()) {
					LispValue value = valuesIt.next();
					if (value instanceof LispString)
						buff.append(value.toStringSimple());
					else
						buff.append(value.toString());
				}
				return new StandardLispString(buff.toString());
				
/*				if (args.basic_length() > 1)
					return args.second().concatenate(f_lisp.makeCons(args.car(), args.cdr().cdr()));
				return f_lisp.makeString("");*/
			}});
		
		// ...
	}
}