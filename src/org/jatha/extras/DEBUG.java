package org.jatha.extras;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.CompilerException;
import org.jatha.compile.LispCompiler;
import org.jatha.dynatype.LispPackage;

import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispValue;
import org.jatha.machine.SECDMachine;

public class DEBUG implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();

		// (time (...))
		compiler.Register(new LispPrimitive(f_lisp, "TIME", 1) {
			public LispValue Execute(LispValue expression)
					throws CompilerException
			{
			    LispValue code = f_lisp.COMPILER.compile(f_lisp.MACHINE, expression, f_lisp.NIL);
			    SECDMachine newMachine = new SECDMachine(f_lisp);

			    long startMemory  = f_lisp.SYSTEM_INFO.freeMemory();
			    long startTime    = System.currentTimeMillis();
			    LispValue value   = newMachine.Execute(code, f_lisp.NIL);
			    long endTime      = System.currentTimeMillis();
			    long endMemory    = f_lisp.SYSTEM_INFO.freeMemory();

			    System.out.println("\n; real time: " + (endTime - startTime)     + " ms");
			    System.out.println(  ";     space: " + (startMemory - endMemory) + " bytes");

			    return value;
			}
		}, SYSTEM_PKG);
	}
}
