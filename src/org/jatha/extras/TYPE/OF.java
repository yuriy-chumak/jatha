package org.jatha.extras.TYPE;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispPrimitive;

import org.jatha.dynatype.LispPackage;
import org.jatha.dynatype.LispValue;

public class OF implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();
		
		compiler.Register(new LispPrimitive(f_lisp, "TYPE-OF", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.type_of();
			}
		}, SYSTEM_PKG);
		
		compiler.Register(new LispPrimitive(f_lisp, "NUMBERP", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.numberp(); // (numberp object) ==  (typep object 'number)
			}
		}, SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "FLOATP", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.floatp();
			}
		}, SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "STRINGP", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.stringp();
			}
		}, SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "INTEGERP", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.integerp();
			}
		}, SYSTEM_PKG);
	}
}
