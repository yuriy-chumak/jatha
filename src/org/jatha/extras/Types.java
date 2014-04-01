package org.jatha.extras;

import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive1;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispValueNotANumberException;

public class Types implements LispExtension
{
	@Override
	public void Register(LispCompiler compiler) {
		
/*		compiler.Register(new LispPrimitive1("SIN") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(Math.sin(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		compiler.Register(new LispPrimitive1("COS") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(Math.cos(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		compiler.Register(new LispPrimitive1("TAN") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(Math.tan(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		compiler.Register(new LispPrimitive1("CTG") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(1 / Math.tan(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		compiler.Register(new LispPrimitive1("SEC") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(1 / Math.cos(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		compiler.Register(new LispPrimitive1("CSC") {
			@Override
			protected final LispValue Execute(final LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(1 / Math.sin(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
		
		
		// asin
		// acos
		// atan
		// atan (2 args)
		
		// cosh*/
	}
}