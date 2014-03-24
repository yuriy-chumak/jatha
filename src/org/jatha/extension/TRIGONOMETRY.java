package org.jatha.extension;

import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive1;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispValueNotANumberException;

public class TRIGONOMETRY implements LispExtension
{
	@Override
	public void Register(LispCompiler compiler) {
		
		compiler.Register(new LispPrimitive1("SIN") {
			@Override
			protected LispValue Execute(LispValue arg)
					throws CompilerException {
				if (arg instanceof LispNumber)
					return new StandardLispReal(StrictMath.sin(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}});
	}
}