package org.jatha.extras;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.CompilerException;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.SymbolFunctionPrimitive;
import org.jatha.compile.SymbolPackagePrimitive;
import org.jatha.compile.SymbolPlistPrimitive;
import org.jatha.compile.SymbolValuePrimitive;
import org.jatha.dynatype.LispComplex;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispPackage;
import org.jatha.dynatype.LispSymbol;
import org.jatha.dynatype.LispValueNotANumberException;
import org.jatha.dynatype.LispValueNotASymbolException;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispValue;
import org.jatha.machine.SECDMachine;

public class SYMBOLS implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();

		compiler.Register(new LispPrimitive(f_lisp, "SYMBOLP", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispSymbol)
					return f_lisp.T;
				if (arg == f_lisp.NIL)
					return f_lisp.T;
				return f_lisp.NIL;
			}
		}, SYSTEM_PKG);
		
	    compiler.Register(new SymbolFunctionPrimitive(f_lisp),SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "SYMBOL-NAME", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispSymbol)
					return ((LispSymbol)arg).symbol_name();
				throw new LispValueNotASymbolException(arg);
			}
		}, SYSTEM_PKG);
	    compiler.Register(new SymbolPackagePrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new SymbolPlistPrimitive(f_lisp),SYSTEM_PKG);
	    compiler.Register(new SymbolValuePrimitive(f_lisp),SYSTEM_PKG);
	}
}
