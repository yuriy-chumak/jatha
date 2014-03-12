package org.jatha.extras;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispComplex;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispPackage;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.LispValueNotANumberException;
import org.jatha.dynatype.StandardLispComplex;
import org.jatha.dynatype.StandardLispReal;

public class TRIGONOMETRIC implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();

//		/** Trigonometric Functions */
//		public LispValue sin();	// Sine function, argument is in radians.
//		public LispValue cos(); // Cosine function, argument in radians.
//		public LispValue tan(); // Tangent function, argument is in radians.
//		public LispValue cot(); // Cotangent function, 1/tan(x), argument in radians.
//		public LispValue sec(); // Secant function, 1/cos(x), argument in radians.
//		public LispValue csc(); // Cosecant function, 1/sin(x), argument in radians.
		
//		/** Inverse trigonometric functions */
//		public LispValue asin(); // Arcsine function, argument in radians.
//		public LispValue acos(); // Arccosine
//		public LispValue atan(); // Arctangent
//		public LispValue atan2(LispNumber x);

//  // Trigonometric Functions
//  // 	  	public LispComplex sin(LispComplex aa );
//  public LispComplex asin(LispComplex aa );
//  public LispComplex cos(LispComplex aa );
//  public LispComplex acos(LispComplex aa );
//  public LispComplex tan(LispComplex aa );
//  public LispComplex atan(LispComplex aa );
//  public LispComplex sinh(LispComplex aa );
//  public LispComplex asinh(LispComplex aa );
//  public LispComplex cosh(LispComplex aa );
//  public LispComplex acosh(LispComplex aa );
//  public LispComplex tanh(LispComplex aa );
//  public LispComplex atanh(LispComplex aa );

		/**
		 * Sine function, argument is in radians.
		 */
		compiler.Register(new LispPrimitive(f_lisp, "SIN", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispNumber)
					return new StandardLispReal(f_lisp,
							StrictMath.sin(((LispNumber)arg).getDoubleValue()));
				if (arg instanceof LispComplex) // tbd.
					return null;
				throw new LispValueNotANumberException(arg);
			}
		}, SYSTEM_PKG);
		/**
		 * Cosine function, argument is in radians.
		 */
		compiler.Register(new LispPrimitive(f_lisp, "COS", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispNumber)
					return new StandardLispReal(f_lisp,
							StrictMath.cos(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}
		}, SYSTEM_PKG);
		/**
		 * Tangent function, argument is in radians.
		 */
		compiler.Register(new LispPrimitive(f_lisp, "TAN", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispNumber)
					return new StandardLispReal(f_lisp,
							StrictMath.tan(((LispNumber)arg).getDoubleValue()));
				throw new LispValueNotANumberException(arg);
			}
		}, SYSTEM_PKG);
/*		Register(new LispPrimitive(f_lisp, "TAN", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.tan();
			}
		}, SYSTEM_PKG);*/
		
		// todo: make as atan optional second agrument
		compiler.Register(new LispPrimitive(f_lisp, "ATAN2", 2) {
			public LispValue Execute(LispValue x, LispValue y) {
				if (!(x instanceof LispNumber))
					throw new LispValueNotANumberException(x);
				if (!(y instanceof LispNumber))
					throw new LispValueNotANumberException(y);
				
				return new StandardLispReal(f_lisp,
							StrictMath.atan2(
									((LispNumber)y).getDoubleValue(),
									((LispNumber)x).getDoubleValue()));
			}
		}, SYSTEM_PKG);
	}

}
