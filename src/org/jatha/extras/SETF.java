package org.jatha.extras;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.jatha.Tests;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive1;
import org.jatha.compile.LispPrimitive2;
import org.jatha.dynatype.LispCons;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispValueNotANumberException;

public class SETF implements LispExtension
{
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	@Override
	public void Register(LispCompiler compiler)
	{
		compiler.Register(new LispPrimitive2("SETF-CAR") {
			protected LispValue Execute(LispValue arg1, LispValue arg2) {
				LispCons a = assertCons(arg1);
				return a.setf_car(arg2);
			}
		});
		compiler.Register(new LispPrimitive2("SETF-CDR") {
			protected LispValue Execute(LispValue arg1, LispValue arg2) {
				LispCons a = assertCons(arg1);
				return a.setf_cdr(arg2);
			}
		});
		compiler.Register(new LispPrimitive2("SETF-SYMBOL-VALUE") {
			protected LispValue Execute(LispValue arg1, LispValue arg2) {
				return arg1.setf_symbol_value(arg2);
			}
		});
		compiler.Register(new LispPrimitive2("SETF-SYMBOL-FUNCTION") {
			protected LispValue Execute(LispValue arg1, LispValue arg2) {
				return arg1.setf_symbol_function(arg2);
			}
		});
		
		InputStreamReader resourceReader = null;
		try
		{
			resourceReader = new InputStreamReader(
					SETF.class.getClassLoader().getResourceAsStream(
							SETF.class.getPackage().getName().replace(".", "/") +
							"/SETF"
					));
			compiler.load(resourceReader);
			resourceReader.close();
	    }
		catch (CompilerException e) {
			e.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		
/*		compiler.Register(new LispPrimitive1("SIN") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(Math.sin(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});*/
	}
}