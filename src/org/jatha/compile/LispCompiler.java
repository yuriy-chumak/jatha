/*
 * Jatha - a Common LISP-compatible LISP library in Java.
 * Copyright (C) 1997-2005 Micheal Scott Hewett
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * For further information, please contact Micheal Hewett at
 *   hewett@cs.stanford.edu
 *
 */

package org.jatha.compile;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.jatha.Lisp;
import org.jatha.Registrar;
import org.jatha.dynatype.*;
import org.jatha.exception.*;
import org.jatha.machine.*;

/**
 * LispCompiler has a <tt>compile()</tt> method that will
 * compile a LISP expression and return the SECD code
 * for that expression.
 *
 *
 * Example LISP read/eval/print loop:
 * <pre>
 *    expr   = parser.read(stream);
 *    code   = compiler.compile(expr);
 *    result = machine.eval(code);
 *    result.print();
 * </pre>
 * <p>
 * Macro compilation contributed by Jean-Pierre Gaillardon, April 2005
 * </p>
 * @see org.jatha.machine.SECDMachine
 * @see org.jatha.machine.SECDop
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public class LispCompiler
{
	// Set this to true to produce debugging output during compilation.
	static boolean DEBUG = false;

	static final LispList NIL = LispValue.NIL;
	
	static final LispValue QUOTE     = LispValue.QUOTE;
	
	static final LispValue MACRO     = LispValue.MACRO; // keyword used at begenning of macro code to detect macro
	static final LispValue PRIMITIVE = LispValue.PRIMITIVE;
  
	// These are special forms that get expanded in the compiler
	LispValue COMMENT;
	LispValue PROGN;
	LispValue DEFUN;
	LispValue BLOCK;
	
  LispValue AND;
  LispValue DEFMACRO;
  LispValue IF;
  LispValue LAMBDA;
  LispValue LET;
  LispValue LETREC;
  LispValue OR;
  LispValue SETQ;
    //  LispValue WHEN;

  LispValue AMP_REST;   // keyword &rest used in parameters list
  LispValue DUMMY_FUNCTION; // used for recursive definions
  LispValue DUMMY_MACRO;    // used for recursive definions
  
	LispPrimitive CONS;
  
	Map<LispValue, Compiler> SpecialOperators = null;
	interface Compiler {
		public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
				throws CompilerException;
	}
  

  boolean WarnAboutSpecialsP = false;    // todo: Need some way to turn this on.
  private Lisp f_lisp = null;
  public Lisp getLisp() { return f_lisp; }
  private final Stack<LispValue> legalBlocks = new Stack<LispValue>();
  private final Stack<Set<LispValue>> legalTags   = new Stack<Set<LispValue>>();
  private final Map<Long, LispValue> registeredGos = new HashMap<Long, LispValue>();

  // static initializer.
	private void initializeConstants()
	{
		AMP_REST   = f_lisp.symbol("&REST");
		SETQ       = f_lisp.symbol("SETQ");
    //    WHEN       = f_lisp.EVAL.intern("WHEN");
    
		SpecialOperators = new TreeMap<LispValue, Compiler>() {{
			put(COMMENT = f_lisp.symbol("COMMENT"), new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return cons(machine.LDT, code);
					}
				});
			
			put(PROGN = f_lisp.symbol("PROGN"), new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return compileProgn(args, valueList, code);
					}
				});
			put(DEFUN = f_lisp.symbol("DEFUN"), new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return compileDefun(machine, car(args), cdr(args), valueList, code);
					}
				});
			put(QUOTE/*f_lisp.symbol("QUOTE")*/, new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return cons(machine.LDC, cons(args.first(), code));
					}
				});
			put(BLOCK = f_lisp.symbol("BLOCK"), new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return compileBlock(machine, car(args), cdr(args), valueList, code);
					}
				});

			put(LAMBDA = f_lisp.symbol("LAMBDA"), new Compiler() {
					@Override
					public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
						return compileLambda(machine, cons(PROGN, cdr(args)),
						                     cons(car(args), valueList), code);
					}
				});
			put(DEFMACRO = f_lisp.symbol("DEFMACRO"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileDefmacro(machine, f_lisp.car(args), f_lisp.cdr(args), valueList, code);
						}
				});
			put(AND = f_lisp.symbol("AND"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileAnd(machine, args, valueList, code);
						}
				});
			put(OR = f_lisp.symbol("OR"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileOr(machine, args, valueList, code);
						}
				});
			put(IF =
				f_lisp.symbol("IF"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileIf(machine, args.first(), args.second(), args.third(),
			                           valueList, code);
						}
				});
			put(LET = f_lisp.symbol("LET"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							LispValue vars      = varsFromLetBindings(car(args));
							LispValue values    = valuesFromLetBindings(car(args));
							LispValue newValues = cons(vars, valueList);

							LispValue body      = cons(PROGN, cdr(args));
							return compileLet(machine, vars, values, valueList, body, code);
						}
				});
			put(LETREC = f_lisp.symbol("LETREC"), new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							LispValue vars      = varsFromLetBindings(car(args));
							LispValue values    = valuesFromLetBindings(car(args));
							LispValue newValues = cons(vars, valueList);

							LispValue body      = cons(PROGN, cdr(args));
							
							return cons(machine.DUM,
						            compileApp(machine, values, newValues,
						            		compileLambda(machine, body, newValues,
						            				cons(machine.RAP, code))));
						}
				});
		}};

    //##JPG added
    // should be used only to test type. basic_macrop() retutns true for DUMMY_MACRO and false for DUMMY_FUNCTION
		// this is NOT builtin function and macro
    DUMMY_FUNCTION = new StandardLispFunction(f_lisp, null, cons(f_lisp.T, NIL));
    DUMMY_MACRO    = new StandardLispMacro   (f_lisp, null, cons(f_lisp.T, NIL));
	}

  public LispCompiler(Lisp lisp)
  {
    super();

    f_lisp = lisp;

    initializeConstants();
  }


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Wed Feb  5 09:33:27 1997
  /**
   * Initializes the compiler by registering
   * the LISP primitive functions so that the
   * compiler can recognize them.
   *
   * @see org.jatha.compile.LispCompiler
   * 
   * Primitives list for basic use for leaving as different classes:
   * LispPrimitive, ConsPrimitive, ...
   * 
   * SimpleLispPrimitive, ComplexLispPrimitive (contains special CompileArgs function), ...
   * 
   * and special "optimized" primitive InlineLispPrimitive
   */
	public void init()
	{
		// (require '(pkg1 pkg2 ...)) or (require 'package)
		Register(new LispPrimitiveC(f_lisp, "REQUIRE", 1) { 
			protected LispValue Execute(LispValue values)
					throws CompilerException
			{
				if (values instanceof LispSymbol) {
					require(values);
					return f_lisp.T;
				}
				
				if (values instanceof LispCons) {
					Iterator<LispValue> valuesIt = values.iterator();
					while (valuesIt.hasNext())
					{
						LispValue value = valuesIt.next();
						require(value);
					}
				    return f_lisp.T;
				}
				
				throw new LispValueNotASymbolOrConsException(values);
			}
			
			Set<String> requires = new HashSet<String>();
			void require(LispValue value)
			{
				if (value instanceof LispSymbol) {
					String name = value.toStringSimple().replace('-','.');
					if (requires.contains(name))
						return;
					try {
						Registrar registrar = (Registrar)LispCompiler.this.getClass().getClassLoader().loadClass(
								"org.jatha.extras." + name
								).newInstance();
						registrar.Register(LispCompiler.this);
						requires.add(name);
					}
					catch (Exception ex) {
						throw new LispException("Can't load required " + value.toString() + " module.");
					}
					return;
				}
				throw new LispValueNotASymbolException(value);
			}
		});


    	// "inline" primitives (for perfomance purposes)

		Register(CONS = new LispPrimitive2(f_lisp, "CONS") {
			public LispValue Execute(LispValue a, LispValue b) {
				return cons(a, b);
			}
		});
		Register(new LispPrimitiveC(f_lisp, "LIST", 0) {
			@Override
			public LispValue CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue function,
							LispValue args, LispValue valueList, LispValue code)
					throws CompilerException
			{
				return CompileArgs(compiler, machine, args, valueList, code);
			}
			@Override
			protected LispValue Execute(LispValue arg) throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
			}
			
		});
		/*compiler.Register(new InlineLispPrimitive(f_lisp, "LIST*", 1, Long.MAX_VALUE) {
			LispValue CONS = new ConsPrimitive(f_lisp);
			public LispValue CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispValue args, final LispValue valueList, final LispValue code)
					throws CompilerException
			{
				if (args.cdr() == NIL)
					return compiler.compileArgsLeftToRight(args, valueList, code);
				return compiler.compile(args.car(), valueList,
						CompileArgs(compiler, machine, args.cdr(),
								valueList,
								f_lisp.makeCons(CONS, code)));
			}
		}, SYSTEM_PKG);*/

		Register(new LispPrimitive0(f_lisp, "EXIT") {
			protected LispValue Execute() {
				System.exit(0);
				return f_lisp.T; // no exit, actually
			}
		});
		
		// 
		Register(new LispPrimitive1(f_lisp, "ATOM") {
			protected LispValue Execute(LispValue arg) {
				return bool(is_atom(arg));
			}
		});
		Register(new LispPrimitive1(f_lisp, "NULL") {
			protected LispValue Execute(LispValue arg) {
				return bool(is_null(arg));
			}
		});
		
		Register(new LispPrimitive2(f_lisp, "EQ") {
			public LispValue Execute(LispValue a, LispValue b) {
				if (is_atom(a) && is_atom(b))
					return bool(a == b);
				return NIL;
			}
		});
		Register(new LispPrimitive2(f_lisp, "EQL") {
			public LispValue Execute(LispValue a, LispValue b) {
				return a.eql(b);
			}
		});
		Register(new LispPrimitive1(f_lisp, "NOT") {
			protected LispValue Execute(LispValue a) {
				return bool(is_null(a));
			}
		});

		Register(new LispPrimitive2(f_lisp, "SETQ") {
			// todo: refactor this
			public void Execute(SECDMachine machine)
			{
			    LispValue val = machine.S.pop();
			    LispValue sym = machine.S.pop();

			    if (sym instanceof LispCons) {   // local variable
			    	LispCons ij = (LispCons)sym;
			    	LispCons valueList = (LispCons)machine.E.value();
			    	LispValue newValue = val;
			    	
			    	Lisp.nth(ij, valueList).rplaca(newValue);
			    }

			    else if (sym.specialP())  // special variable
			      machine.special_set(sym, val);

			    else  // global variable
			      sym.setf_symbol_value(val);

			    machine.S.push(val);
			    machine.C.pop();
			}
			public LispValue CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue args,
			                             LispValue valueList, LispValue code)
					throws CompilerException
			{
				
				// 13 Dec 2005 (mh)
				// SETQ is not compiling correctly (noticed by Ola Bini).
				// Also, SET is not compiling correctly.  They are always modifying the
				// global value of the symbol, not the local value.  Fixed it by passing in
				// the index instead of the symbol name if it is known to have a binding.
				
				LispValue lookupVal = compiler.indexAndAttribute(args.first(), valueList);
				
				if (lookupVal.second() == NIL)  // SETQ of a global var
				return
						f_lisp.makeCons(machine.LDC,
				                f_lisp.makeCons(args.first(),
				                		compiler.compile(args.second(), valueList, code)));
				
				else  // SETQ of a local var, inside a LET or something like that.
				return	f_lisp.makeCons(machine.LDC,
								f_lisp.makeCons(lookupVal.second(),
										compiler.compile(args.second(), valueList, code)));
			}
			@Override
			protected LispValue Execute(LispValue arg1, LispValue arg2)
					throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
			}
		});

/*		
		final LispPrimitive EVAL;
		Register(EVAL = new LispPrimitiveC(f_lisp, "EVAL", 1) {
			  public void Execute(SECDMachine machine)
					    throws CompilerException
					  {
					    LispValue expr = machine.S.pop();
					    LispValue code = f_lisp.COMPILER.compile(expr, NIL, NIL);

					    machine.C.pop();  // Pop the EVAL command

					    // Push the new code on the stack.
					    machine.C.assign(code.append(machine.C.value()));
					  }

			@Override
			protected LispValue Execute(LispValue arg) throws CompilerException {
				return null;
			}});

		
		// todo: must be integrated, but needs to test
		Register(new LispPrimitiveC(f_lisp, "FUNCALL", 1) {
			  public void Execute(SECDMachine machine)
			  {
			    // The args list is an expression to be evaluated.
			    // Need to quote the argument(s) because they have already been evaluated.
			    // The EVAL will evaluate them again.
			    LispValue args   = machine.S.pop();
			    LispValue fn     = ((LispCons)args).car();
			    LispValue fnArgs = ((LispCons)args).cdr();

			    machine.S.push(f_lisp.makeCons(fn, quoteList(fnArgs)));

			    // (mh) 4 Sep 2004
			    // This seems like a kludge, but I don't know how to get around it.
			    // if the fn is a user-defined function, we have to move the arguments to the E register.
			    if ((fn instanceof LispFunction) && (! ((LispFunction)fn).isBuiltin()))
			    {
			      machine.S.pop();
			      machine.S.push(f_lisp.makeList(fn));
			      machine.E.push(fnArgs);
			    }

			    machine.C.pop();
			    machine.C.push(EVAL);
			  }
			protected LispValue Execute(LispValue args) {
				return null;
			}
		}, SYSTEM_PKG);
*/
		// move to "math" ?
		Register(new LispPrimitiveC(f_lisp, "+", 0) {
			protected LispValue Execute(LispValue args) {
				if (args == NIL)
					return f_lisp.ZERO;
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).add(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		});
		Register(new LispPrimitiveC(f_lisp, "-", 1) {
			protected LispValue Execute(LispValue args) {
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).sub(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		});
		Register(new LispPrimitiveC(f_lisp, "*", 0) {
			protected LispValue Execute(LispValue args) {
				if (args == NIL)
					return f_lisp.ONE;
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).mul(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		});
		Register(new LispPrimitiveC(f_lisp, "/", 1) {
			protected LispValue Execute(LispValue args) {
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).div(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		});
		
		registerAccessorFunctions();
//		registerStringFunctions(SYSTEM_PKG);
		
		Register(new LispPrimitiveC(f_lisp, "=", 2) {
			protected LispValue Execute(LispValue args) {
			    if (args == NIL)
			        return f_lisp.T;
			      else
			      {
			        // There should be at least 2 arguments.
			        LispValue first = args.first();
			        args = Lisp.cdr(args);
			        for (Iterator<LispValue> iterator = args.iterator(); iterator.hasNext();)
			        {
			          LispValue arg = iterator.next();
			          if (first.equalNumeric(arg) == NIL)
			          {
			            return NIL;
			          }
			        }
			        return f_lisp.T;
			      }
			}			
		});
		
		Register(new LispPrimitive2(f_lisp, "<") {
			protected LispValue Execute(LispValue a, LispValue b) {
				return a.lessThan(b);
			}			
		});
		Register(new LispPrimitive1(f_lisp, "LAST") {
			protected LispValue Execute(LispValue a) {
				return a.last();
			}			
		});
		Register(new LispPrimitive1(f_lisp, "LENGTH") {
			protected LispValue Execute(LispValue a) {
				return a.length();
			}			
		});
		
		Register(new LispPrimitiveC(f_lisp, "APPEND", 0) {
			// First argument should be 'STRING
			// Apply concatenate to the next argument.
			protected LispValue Execute(LispValue args) {
				return appendArgs(args);
			}
			  // This is right-recursive so it only copies each arg once.
			  // The last arg is not copied, of course.
			  LispValue appendArgs(LispValue args)
			  {
			    if (f_lisp.cdr(args) == NIL)
			      return f_lisp.car(args);
			    else
			      return f_lisp.car(args).append(appendArgs(f_lisp.cdr(args)));
			  }
		});
		
		
		// TEMPORARY for TESTS (maybe need to set permanent)
		Register(new LispPrimitive1(f_lisp, "CONSP") {
			protected LispValue Execute(LispValue a) {
				if (a instanceof LispCons)
					return f_lisp.T;
				return NIL;
			}
		});
		Register(new LispPrimitive1(f_lisp, "CONSTANTP") {
			protected LispValue Execute(LispValue a) {
				if (a instanceof LispConstant)
					return f_lisp.T;
				return NIL;
			}
		});

	
		Register(new LispPrimitive1(f_lisp, "STRING") {
			protected LispValue Execute(LispValue a) {
				return a.string();
			}
		});
		Register(new LispPrimitive2(f_lisp, "STRING-EQUAL") {
			protected LispValue Execute(LispValue a, LispValue b) {
				return a.stringEqual(b);
			}
		});
		
		
		/**
		 * Concatenate a string to another string.
		 * Passing in any LispValue causes it to be converted to a string
		 * and concatenated to the end.
		 * This returns a new LispString.
		 */
		Register(new LispPrimitiveC(f_lisp, "CONCATENATE", 1) {
			// First argument should be 'STRING
			// Apply concatenate to the next argument.
			protected LispValue Execute(LispValue args) {
				LispValue concatType = Lisp.car(args);
				if (!concatType.toStringSimple().equalsIgnoreCase("string"))
					throw new LispUndefinedFunctionException("The first argument to Concatenate (" + concatType + ") must be the symbol STRING. Use 'string.");
				args = Lisp.cdr(args);
				
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
		});
		
		
		Register(new LispPrimitive1(f_lisp, "SQRT") {
			protected LispValue Execute(LispValue a) {
				return a.sqrt();
			}
		});
		
		Register(new LispPrimitive1(f_lisp, "PRINT") {
			protected LispValue Execute(LispValue a) {
				return a.print();
			}
		});
		Register(new LispPrimitive2(f_lisp, "SET") {
			protected LispValue Execute(LispValue arg1, LispValue arg2) {
				return arg1.setf_symbol_value(arg2);
			}
		});
		
		Register(new LispPrimitive1(f_lisp, "MACROEXPAND") {
			protected LispValue Execute(LispValue form) {
		        LispValue now = expand(form);
		        LispValue lastOne = form;
		        while(now != lastOne && now instanceof LispCons && !(now == NIL)) {
		            lastOne = now;
		            now = expand(now);
		        }
				return now;
			}
		    private LispValue expand(final LispValue form) {
		        final LispValue carForm = f_lisp.car(form); // todo: check for LispSymbol
		        if(carForm.fboundp() && carForm.symbol_function() != null && carForm.symbol_function().basic_macrop()) {
		            return f_lisp.eval(f_lisp.makeCons(f_lisp.symbol("%%%" + ((LispSymbol)carForm).symbol_name().toStringSimple()), quoteList(f_lisp.cdr(form))));
		        } else {
		            return form;
		        }
		    }

		    private LispValue quoteList(final LispValue intern) {
		        LispValue ret = NIL;
		        for(final Iterator<LispValue> iter = intern.iterator(); iter.hasNext();) {
		            final LispValue curr = (LispValue)iter.next();
		            ret = f_lisp.makeCons(f_lisp.makeList(QUOTE, curr), ret);
		        }
		        return ret.nreverse();
		    }
			
		});
		Register(new LispPrimitive1(f_lisp, "MACROEXPAND-1") {
			protected LispValue Execute(LispValue form) {
		        final LispValue carForm = f_lisp.car(form);	// todo: check for LispSymbol
		        
		        if(carForm.fboundp() && carForm.symbol_function() != null && carForm.symbol_function().basic_macrop()) {
		            return (f_lisp.eval(f_lisp.makeCons(f_lisp.symbol("%%%" + ((LispSymbol)carForm).symbol_name().toStringSimple()), quoteList(f_lisp.cdr(form)))));
		        } else {
		            return (form);
		        }
			}
		    private LispValue quoteList(final LispValue intern) {
		        LispValue ret = NIL;
		        for(final Iterator<LispValue> iter = intern.iterator();iter.hasNext();) {
		            final LispValue curr = (LispValue)iter.next();
		            ret = f_lisp.makeCons(f_lisp.makeList(QUOTE,curr),ret);
		        }
		        return ret.nreverse();
		    }
		});
		Register(new LispPrimitive1(f_lisp, "SIN") {
			protected LispValue Execute(LispValue a) {
				return new StandardLispReal(
						f_lisp,
						Math.sin(((LispNumber)a).getDoubleValue())
				);
			}			
		});
		
	}
	
	public LispValue eval(String expression)
	{
		return f_lisp.eval(expression);
	}

	// @author  Micheal S. Hewett    hewett@cs.stanford.edu
	// @date    Wed Feb  5 09:45:51 1997
	/**
	 * Use this function to register any new LISP primitives
	 * that you create from Java code.  The compiler will
	 * then recognize them and compile them appropriately.
	 *
	 * Example:
	 * <pre>
	 *   compiler.Register(new RevAppendPrimitive());
	 * </pre>
	 * @see LispPrimitive
	 * @param primitive
	 */
	public void Register(LispPrimitive primitive)
	{
		final LispSymbol symbol = f_lisp.symbol(primitive.LispFunctionNameString());
		symbol.setf_symbol_function(list(PRIMITIVE, primitive));
	}

    public Stack<LispValue> getLegalBlocks() {
        return legalBlocks;
    }

    public Stack<Set<LispValue>> getLegalTags() {
        return legalTags;
    }

    public Map<Long, LispValue> getRegisteredGos() {
      return registeredGos;
    }

    public boolean isLegalTag(final LispValue tag) {
        for(final java.util.Iterator<Set<LispValue>> iter = legalTags.iterator();iter.hasNext();) {
            if(iter.next().contains(tag)) {
                return true;
            }
        }
        return false;
    }

  /* --- Compiler flags   --- */

  public void WarnAboutSpecials(boolean value)
  {  WarnAboutSpecialsP = value; }


  /* --- Utility routines --- */
  // New IndexInList and IndexAndAttributes contributed by
  // Jean-Pierre Gaillardon, April 2005
  /**
   * Looks up the symbol in a list
   * @param e a Symbol
   * @param l a list
   * @param attribute The attribute of the found symbol is assigned to attribute[0]. It can be NIL or &rest
   * @return the index in list of found symbol (it start from 1) or 0 if symbol has not been found in list
   */
  public int indexInList(LispValue e, LispValue l, LispValue[] attribute)
  {
    int indexInList = 1;
    LispValue list = l ;
    LispValue previousAttribute = NIL;
    for(;;list = f_lisp.cdr(list))
    {
      if(list == NIL)
      {
        return 0;  // not found
      }
      LispValue elt = f_lisp.car(list);
      if(elt == AMP_REST)
      {
        previousAttribute = AMP_REST;
        continue;
      }
      if (elt == e)
      {
        attribute[0] = previousAttribute;
        return indexInList;
      }
      previousAttribute = NIL;
      indexInList++;
    }
  }


  // New IndexInList and IndexAndAttributes contributed by
  // Jean-Pierre Gaillardon, April 2005
  /**
   * Looks up the symbol in a list of lists.
   * Returns a dotted pair.
   *   - first element is the attribute of the found symbol
   *            it can be "&rest"  or NIL for no attribute (or if symbol has not been found)
   *   - second element is the index of the list in which it is found and
   *        the index in that list. Both indexes start from 1. The &rest keyword eventually
   *        present in the list is not taken into account for index count.
   *        index is NIL if not found.
   * Examples:
   * <pre>
   *     indexAndAttribute(b, ((a b c) (d e f))     = (NIL. (1 . 2))
   *     indexAndAttribute(f, ((a b c) (d e f))     = (NIL.(2 . 3))
   *     indexAndAttribute(z, ((a b c) (d e f))     = (NIL. NIL)
   *     indexAndAttribute(l, ((a &rest l) (d e f)) = (&rest .(1 . 2))
   * </pre>
   * @param e a Symbol
   * @param l a list of lists
   * @return either (NIL.NIL), if not found, or a dotted pair; first is the attribute for symbol, second is a Cons of 2 LispIntegers\
   (a . b) indicating list number (a) and index into that list (b)
   */
	public LispValue indexAndAttribute(LispValue e, LispValue l)
	{
		int indexSubList = 1;
		LispValue subList = l;
		LispValue[] attribute = new LispValue[] { NIL };

		for (;; indexSubList++, subList = f_lisp.cdr(subList))
		{
			if (subList == NIL)
				return cons(NIL, NIL); // not found
			int indexInSubList = indexInList(e, car(subList), attribute);
			if (indexInSubList != 0) // found
			{
				LispValue position = cons(cons(f_lisp.makeInteger(indexSubList),
				                               f_lisp.makeInteger(indexInSubList)),
				                          NIL);
				return cons(attribute[0], position);
			}
		}
	}


	// This places the args on the stack so that they will be evaluated L->R,
	// as is required in Common LISP.
	//
	public LispValue compileArgsLeftToRight(LispValue args,
                                            LispValue valueList,
                                            LispValue code)
		throws CompilerException
	{
		LispValue rest = code;
		if (args != NIL)
		{
			List<LispValue> list = args.toRandomAccess();
			for (int i = list.size() - 1; i >= 0; i--)
			{
				rest = compile(list.get(i), valueList, rest);
			}
		}
		return rest;
	}

	// This places the args on the stack L->R, but unevaluated.
	//
	public LispValue compileConstantArgsLeftToRight(SECDMachine machine,
                                                    LispValue args,
                                                    LispValue valueList,
                                                    LispValue code)
	{
		if (args == NIL)
			return code;
		else
			return cons(machine.LDC,
                        cons(car(args),
                             compileConstantArgsLeftToRight(machine, cdr(args), valueList, code)));
	}

	/**
	 * Returns the input list with quotes added before every
	 * top-level expression.
	 */
	public LispValue quoteList(LispValue l)
	{
		LispValue rest = NIL;
		if (l != NIL)
		{
			List<LispValue> list = l.toRandomAccess();
			for (int i = list.size() - 1; i >= 0; i--)
			{
				rest = cons(f_lisp.makeList(QUOTE, list.get(i)), rest);
			}
		}
		return rest;
	}



	// @author  Micheal S. Hewett    hewett@cs.stanford.edu
	// @date    Sun Feb  2 19:46:35 1997
	/**
	 * <tt>compile</tt> takes a LISP expression, a list of
	 * global variables, and optionally an already-generated
	 * list of code.  It returns compiled code in a list.
	 *
	 * @see LispCompiler
	 * @param expr expression to compile
	 * @param varValues  global or local variable list.
	 * @return LispValue - generated code
	 */
	public LispValue compile(SECDMachine machine, LispValue expr, LispValue varValues)
			throws CompilerException
	{
		if (DEBUG) {
			System.out.println("expr = " + expr);
			System.out.println("varValues = " + varValues);
			System.out.println("STOP = " + machine.STOP);
			System.out.println("NIL = " + NIL);
			System.out.println("initial code = " + f_lisp.makeCons(machine.STOP, NIL));
		}

		return compile(expr, varValues, cons(machine.STOP, NIL));
	}

	// @author  Micheal S. Hewett    hewett@cs.stanford.edu
	// @date    Sun Feb  2 19:46:35 1997
	/**
	 * <tt>compile</tt> takes a LISP expression, a list of
	 * global variables, and optionally an already-generated
	 * list of code.  It returns compiled code in a list.
	 *
	 * @see LispCompiler
	 * @param expr expression to compile
	 * @param valueList global variable list.
	 * @param  code [optional]
	 * @return LispValue - generated code
	 */
	public LispValue compile(LispValue expr, LispValue valueList, LispValue code)
			throws CompilerException
	{
		SECDMachine machine = f_lisp.MACHINE;
		
		if (DEBUG) {
			System.out.print("\nCompile: " + expr);
			System.out.print("\n   code: " + code);
		}

		if (is_atom(expr))
			return compileAtom(machine, expr, valueList, code);
		else
			return compileList(machine, expr, valueList, code);
	}


	LispValue compileAtom(SECDMachine machine, LispValue expr, LispValue valueList, LispValue code)
	{
		if (DEBUG) {
			System.out.print("\nCompile Atom: " + expr);
			System.out.print(" of type " + expr.getClass().getName());
			System.out.flush();
		}

		if (expr == NIL)
			return cons(machine.LDNIL, code);
		
		if (expr == f_lisp.T)
			return cons(machine.LDT, code);

		if (expr instanceof LispKeyword)
			return cons(machine.LDC, cons(expr, code));
		
		if (expr instanceof LispSymbol) {
			//LispValue varIndex = index(expr, valueList);
			//##JPG use indexAndAttributes() instead of index
			LispValue varIdxAndAttributes = indexAndAttribute(expr, valueList);
			LispValue paramAttribute      = car(varIdxAndAttributes);
			LispValue varIndex            = car(cdr(varIdxAndAttributes));

			if (varIndex == NIL)
			{
				/* Not a local variable, maybe it's global */
				if (!expr.specialP() && WarnAboutSpecialsP)
					System.err.print("\n;; ** Warning - " + expr.toString() + " assumed special.\n");

				return cons(machine.LD_GLOBAL, cons(expr, code));
//			 else
//			   throw new UndefinedVariableException(((LispString)(expr.symbol_name())).getValue());
		      }
		      else  /* Found the symbol.  Is it bound? */
		      {
		        //##JPG opcode LDR instead of LD for variable arguments
		        // note : paramAttribute can only be nil or &rest
		        LispValue loadOpCode = (paramAttribute == AMP_REST) ? machine.LDR : machine.LD;
		        return(f_lisp.makeCons(loadOpCode, f_lisp.makeCons(varIndex, code)));
		      }
		}
		
		return cons(machine.LDC, cons(expr, code));
	}

	LispValue compileList(SECDMachine machine, LispValue expr, LispValue valueList, LispValue code)
			throws CompilerException
	{
		LispValue function = f_lisp.car(expr);
		LispValue args     = f_lisp.cdr(expr);

		
		if (DEBUG)
			System.out.print("\nCompile List: " + expr);

		// User-defined function
		if (function instanceof LispFunction) // was: basic_functionp()
		{	// todo: doesn't called for now - maybe bug?
			LispFunction lFunc = (LispFunction) function;
			if (lFunc.isBuiltin())
				return compileBuiltin(machine, function, args, valueList, code);
			return compileUserDefinedFunction(machine, function, args, valueList, code);
		}

		// Function on a symbol
		if (is_atom(function))
		{
			if (Lisp.isBuiltinFunction(function))
				return compileBuiltin(machine, function, args, valueList, code);

			Compiler specialCompiler;
			if ((specialCompiler = SpecialOperators.get(function)) != null)
				return specialCompiler.compile(machine, args, valueList, code);
			
			// ordinary function
			{
				// ##JPG compileSpecialForm() has been modified to support DEFMACRO
				// LispValue defn = index(function, valueList);
				// ##JPG if function has a variable number of parameters (&rest is present in paraameters list)
				//   the opcode LDR (LoaD with Rest) is used in place of LD
				LispValue fnIdxAndAttributes = indexAndAttribute(function, valueList);
				LispValue defn = car(cdr(fnIdxAndAttributes));
				LispValue loadOpCode = (f_lisp.car(fnIdxAndAttributes) == AMP_REST) ? machine.LDR : machine.LD;

				if (defn == NIL)
				{
					try
					{
						defn = ((LispFunction)function.symbol_function()).getCode();
					}
					catch (LispException e)
					{
						defn = null;
					}


					if ((defn == NIL) || (defn == null))
					{
						if (function instanceof LispSymbol)
							throw new UndefinedFunctionException(((LispString)(((LispSymbol)function).symbol_name())).toString());
						else
							throw new UndefinedFunctionException(function.toString());
					}
				}

				// ##JPG add this if block to compile macro
				if (function.symbol_function().basic_macrop())
					//------------------------ compile macro --------------------------------
				{
					if (f_lisp.car(defn).numberp() == f_lisp.T) /* macro present in closure */
					{
						//##JPG idem compileApp but don't evaluate arguments
						return compileAppConstant(machine, args, valueList,
                                      f_lisp.makeCons(loadOpCode,
                                                      f_lisp.makeCons(defn,
                                                                      (f_lisp.car(code) == machine.RTN) ? f_lisp.makeCons(machine.DAP, f_lisp.cdr(code))
                                                                      : f_lisp.makeCons(machine.AP, code))));

					}
					else /* Compiled macro */
					{
						LispValue expandCode =
							f_lisp.makeCons(machine.DUM,
                                f_lisp.makeCons(machine.LDFC,
                                                f_lisp.makeCons(function,
                                                                f_lisp.makeCons(machine.LDNIL,
                                                                                f_lisp.makeCons(CONS,
                                                                                                compileLambda(machine, expr,  f_lisp.makeCons(
                                                                                                    f_lisp.makeCons(function, NIL),
                                                                                                    valueList),
                                                                                                              f_lisp.makeCons(machine.RAP,
                                                                                                                              NIL)))))));

						LispValue expandValue = machine.Execute(expandCode, NIL);
						if (DEBUG)
							System.out.print("\nMacro " + expr + " expanded to " + expandValue);
						return compile(expandValue, valueList,code );
					}
				}

				// compile a function  --------------------------------
				if (f_lisp.car(defn).numberp() == f_lisp.T)
					return compileApp(machine, args, valueList,
                              f_lisp.makeCons(loadOpCode,
                                              f_lisp.makeCons(defn,
                                                              (f_lisp.car(code) == machine.RTN) ? f_lisp.makeCons(machine.DAP, f_lisp.cdr(code))
                                                              : f_lisp.makeCons(machine.AP, code))));

				if (f_lisp.car(defn) == LAMBDA)    /* Interpreted fn */
					return compileApp(machine, args, valueList,
                              compileLambda(machine, f_lisp.cdr(f_lisp.cdr(defn)),
                                            f_lisp.makeCons(defn.second(), valueList),
                                            code));

					/* Compiled fn */
				return f_lisp.makeCons(machine.DUM,
                                   f_lisp.makeCons(machine.LDFC,
                                                   f_lisp.makeCons(function,
                                                                   f_lisp.makeCons(machine.LDNIL,
                                                                                 f_lisp.makeCons(CONS,
                                                                                                 compileLambda(machine, expr,
                                                                                                               f_lisp.makeCons(f_lisp.makeCons(function, NIL),
                                                                                                                               valueList),
                                                                                                               f_lisp.makeCons(machine.RAP, code)))))));
			}
		}
		
		/* an application from within a nested function */
		return compileApp(machine, args, valueList,
				compile(function, valueList,
						(car(code) == machine.RTN)
							? cons(machine.DAP, cdr(code))
							: cons(machine.AP, code)));
	}

  LispValue compileSpecialForm(SECDMachine machine, LispValue function,
                               LispValue args,
                               LispValue valueList,
                               LispValue code)
    throws CompilerException
  {
    if (DEBUG)
      System.out.print("\nCompile Special Form: " + function);


    // We have a return for every known branch, but I guess this
    // programmer *could* make a mistake sometime, so we'll put an
    // error message here.

    System.out.println("\n;; *** Compiler error in CompileAtom");
    return NIL;
  }


  public LispValue compileLet(SECDMachine machine,
                              LispValue vars, LispValue values,
                              LispValue valueList,
                              LispValue body, LispValue code)
    throws CompilerException
  {
    // Divide the variables into special and non-special var sets.
    // Special variables get extra binding instructions.

    LispValue specialVars = NIL;
    LispValue specialVals = NIL;
    LispValue localVars   = NIL;
    LispValue localVals   = NIL;
    LispValue varPtr      = vars;
    LispValue valPtr      = values;

    while (varPtr != NIL)
    {
      if (f_lisp.car(varPtr).specialP())
      {
        specialVars = f_lisp.makeCons(f_lisp.car(varPtr), specialVars);
        specialVals = f_lisp.makeCons(f_lisp.car(valPtr), specialVals);
      }
      else
      {
        localVars   = f_lisp.makeCons(f_lisp.car(varPtr), localVars);
        localVals   = f_lisp.makeCons(f_lisp.car(valPtr), localVals);
      }

      varPtr = f_lisp.cdr(varPtr);
      valPtr = f_lisp.cdr(valPtr);
    }

    // The local vars get compiled by the compileApp,
    // the special vars get compiled after that and just
    // before the Lambda is compiled.
    LispValue ret =
    		compileApp(machine, localVals, valueList,
    				compileSpecialBind(machine, specialVars, specialVals, valueList,
    						compileLambda(machine, body, cons(localVars, valueList),
    								cons(machine.AP,
    										compileSpecialUnbind(machine, specialVars, code)))));
    return ret;
    // (code.car() == machine.RTN) ?
    // f_lisp.makeCons(machine.DAP, code.cdr())
    //			        : f_lisp.makeCons(machine.AP, code));
  }


  // UTILITY functions for LET

  // Inserts special-bind opcode for each var.
  LispValue compileSpecialBind(SECDMachine machine, LispValue vars, LispValue values,
                               LispValue valueList, LispValue code)
    throws CompilerException
  {
    if (vars == NIL)
      return code;
    else
      return compile(f_lisp.car(values), valueList,
                     f_lisp.makeCons(machine.SP_BIND,
                                               f_lisp.makeCons(f_lisp.car(vars),
                                                                         compileSpecialBind(machine, f_lisp.cdr(vars), f_lisp.cdr(values), valueList, code))));
  }

  // Inserts special-bind opcode for each var.
  LispValue compileSpecialUnbind(SECDMachine machine, LispValue vars, LispValue code)
  {
    if (vars == NIL)
      return code;
    else
      return f_lisp.makeCons(machine.SP_UNBIND,
                                       f_lisp.makeCons(f_lisp.car(vars),
                                                                 compileSpecialUnbind(machine, f_lisp.cdr(vars), code)));
  }


  // each entry is (VAR VAL) or VAR.  Latter has implied value of NIL.
  public LispValue varsFromLetBindings(LispValue varValueList)
  {
    if (varValueList == NIL)
      return NIL;
    else if (f_lisp.car(varValueList) instanceof LispCons)
      return f_lisp.makeCons(f_lisp.car(f_lisp.car(varValueList)),
                                       varsFromLetBindings(f_lisp.cdr(varValueList)));
    else
      return f_lisp.makeCons(f_lisp.car(varValueList),
                                       varsFromLetBindings(f_lisp.cdr(varValueList)));
  }

  // each entry is (VAR VAL) or VAR.  Latter has implied value of NIL.
  public LispValue valuesFromLetBindings(LispValue varValueList)
  {
    if (varValueList == NIL)
      return NIL;
    else if (f_lisp.car(varValueList) instanceof LispCons)
      return f_lisp.makeCons(f_lisp.car(varValueList).second(),
                                       valuesFromLetBindings(f_lisp.cdr(varValueList)));
    else
      return f_lisp.makeCons(NIL,
                                       valuesFromLetBindings(f_lisp.cdr(varValueList)));
  }

  /* obsolete 1 Sep 2004 (mh)
  boolean builtinFunctionP(LispValue fn)
  {
    if ((! fn.basic_symbolp()) || (fn.fboundp() != f_lisp.T))
      return false;

    LispValue defn = fn.symbol_function();

    if (defn == null)
      return false;

    if ((defn.listp() == f_lisp.T) && (defn.first() == PRIMITIVE))
      return true;
    else
      return false;
  }
  */

  public boolean specialFormP(LispValue fn)
  {
    if ((fn instanceof LispSymbol)
        &&    ((fn == AND)
            || (fn == DEFMACRO)
            || (fn == DEFUN)
            || (fn == IF)
            || (fn == LET)
            || (fn == LAMBDA)
            || (fn == LETREC)
            || (fn == OR)
            || (fn == PROGN)
            || (fn == COMMENT)
                   //            || (fn == BLOCK)
                   //            || (fn == WHEN)
            ))
      return true;
    else
      return false;
  }

  // This version of 'compileApp' is modified from the version in Kogge's
  // book.  It puts args on stack in the correct L->R order and does
  // not require the caller to prepend a NIL instruction on the
  // resulting code.
  LispValue compileApp(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
    throws CompilerException
  {
    if (DEBUG)
      System.out.print("\nCompile App: " + args + ", valueList = " + valueList);

    if (args == NIL)
      return f_lisp.makeCons(machine.LDNIL, code);
    else
      return
              compile(f_lisp.car(args), valueList,
                      compileApp(machine, f_lisp.cdr(args), valueList,
                                 f_lisp.makeCons(CONS, code)));
  }


  // ##JPG added
  // similar to compileApp() but doesn't evaluate parameters
  LispValue compileAppConstant(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
  {
    if (DEBUG)
      System.out.print("\nCompile AppConstant: " + args + ", valueList = " + valueList + ", code = " + code);
    
    List<LispValue> list = args.toRandomAccess();
    if (args != NIL)
    {
      for (int i = list.size() - 1; i >= 0; i--)
      {
        code = f_lisp.makeCons(CONS, code);
      }
    }
    code = f_lisp.makeCons(machine.LDNIL, code);
    LispValue rest = code;
    if (args != NIL)
    {
      for (int i = list.size() - 1; i >= 0; i--)
      {
        rest = f_lisp.makeCons(machine.LDC, f_lisp.makeCons(list.get(i), rest));
      }
    }
    return rest;
  }

  LispValue compileBuiltin(SECDMachine machine, LispValue fn, LispValue args,
                           LispValue valueList, LispValue code) throws CompilerException
  {
    if (DEBUG)
      System.out.print("\nCompile Builtin: " + fn + "  " + args);


    // Builtin LISP primitives have a symbol-function of the
    // form (:PRIMITIVE <ic>).  We call the CompileArgs functions
    // of the primitive instruction.
    LispValue executableCode = NIL;

    if (is_atom(fn))
      executableCode = ((LispFunction)fn.symbol_function()).getCode().second();
    else if (fn.basic_functionp())
      executableCode = (((LispFunction)fn).getCode()).second();

    if (!((LispPrimitive)executableCode).validArgumentList(args))
      throw new ArgumentCountMismatchException(((LispString)((LispSymbol)fn).symbol_name()).getValue(),
                                               ((LispPrimitive)executableCode).parameterCountString(),
                                               ((LispInteger)(args.length())).getLongValue());

    return ((LispPrimitive)executableCode).CompileArgs(this, machine, fn, args, valueList, code);
  }

  /**
   * FN is an instance of StandardLispFunction
   * @param machine
   * @param fn an instance of StandardLispFunction
   * @param args
   * @param valueList
   * @param code
   * @throws CompilerException
   */
  LispValue compileUserDefinedFunction(SECDMachine machine, LispValue fn, LispValue args,
                                       LispValue valueList, LispValue code) throws CompilerException
  {
    if (DEBUG)
      System.out.print("\nCompile user-defined: (" + fn + "  " + args + "), vl = " + valueList);

    LispValue executableCode = ((LispFunction)fn).getCode();

    // Assume that the arguments are correct ?
    // TODO: how do we check arguments?  Besides, we've lost the name of the function.
    /*
    if (!((LispPrimitive)(executableCode.second())).validArgumentList(args))
      throw new ArgumentCountMismatchException(((LispString)fn.symbol_name()).getValue(),
                                               ((LispPrimitive)(executableCode.second())).parameterCountString(),
                                               ((LispInteger)(args.length())).getLongValue());
    */

    return compileArgsLeftToRight(args, valueList, executableCode.append(code));
  }


  LispValue compileAnd(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
    throws CompilerException
  {
    // No args: return default value of T
    if (args == NIL)
      return f_lisp.makeCons(machine.LDT, code);

    // 1 arg: just compile the argument.
    if (f_lisp.cdr(args) == NIL)
      return compile(args.first(), valueList, code);

    // Multiple arguments: construct an IF statement
    // (let ((*dummy* args.first())) (if ...))

    LispValue dummyVar = f_lisp.symbol("*AND-DUMMY-VAR*");
    dummyVar.set_special(true);

    return compile(f_lisp.makeList(LET,
      f_lisp.makeCons(f_lisp.makeList(dummyVar, args.first()), NIL),
      f_lisp.makeList(IF, dummyVar, compileAndAux(dummyVar, f_lisp.cdr(args)), NIL)),
      valueList,
      code);
  }


  LispValue compileAndAux(LispValue dummyVar, LispValue args)
  {
    if (f_lisp.cdr(args) == NIL)
      return (f_lisp.car(args));

    return
            f_lisp.makeList(PROGN,
              f_lisp.makeList(SETQ, dummyVar, f_lisp.car(args)),
                f_lisp.makeList(IF, dummyVar,
                  compileAndAux(dummyVar, f_lisp.cdr(args)),
                    NIL));
  }



  LispValue compileOr(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
    throws CompilerException
  {
    // No args: return default value of NIL
    if (args == NIL)
      return f_lisp.makeCons(machine.LDNIL, code);

    // 1 arg: just compile the argument.
    if (f_lisp.cdr(args) == NIL)
      return compile(args.first(), valueList, code);

    // Multiple arguments: construct an IF statement
    // (let ((*dummy* args.first())) (if ...))

    LispValue dummyVar = f_lisp.symbol("*OR-DUMMY-VAR*");
    dummyVar.set_special(true);

    return compile(f_lisp.makeList(LET,
      f_lisp.makeCons(f_lisp.makeList(dummyVar, args.first()),
        NIL),
      f_lisp.makeList(IF, dummyVar,
        dummyVar,
        compileOrAux(dummyVar, f_lisp.cdr(args)))),
      valueList,
      code);
  }


  LispValue compileOrAux(LispValue dummyVar, LispValue args)
  {
    if (f_lisp.cdr(args) == NIL)
      return (f_lisp.car(args));

    return
      f_lisp.makeList(PROGN,
        f_lisp.makeList(SETQ, dummyVar, f_lisp.car(args)),
        f_lisp.makeList(IF, dummyVar,
          dummyVar,
          compileOrAux(dummyVar, f_lisp.cdr(args))));
  }



  LispValue compileDefun(SECDMachine machine, LispValue name, LispValue argsAndBody,
                         LispValue valueList, LispValue code)
    throws CompilerException
  {
    // Change the DEFUN into a LAMBDA and compile it.
    //##JPG
    // for compilation of recursive functions, we need to know if the symbol under compilation is a
    // function or a macro. It's the aim of DUMMY_FUNCTION
    name.setf_symbol_function(DUMMY_FUNCTION);

    // OB:
    // Added support for documentation strings on defuns.
    // Todo, fix support, so that DECLARE statements are ignored.
    // If the body of the function only consists of a string, this is not taken as a
    // documentation string, as there would be no content of the function otherwise.
    final LispValue possibleDocumentation = argsAndBody.second();
    LispValue endArgsAndBody = argsAndBody;
    if(possibleDocumentation instanceof LispString && argsAndBody.basic_length() > 2) {
        name.setf_documentation(f_lisp.symbol("FUNCTION"), possibleDocumentation);
        endArgsAndBody = f_lisp.makeCons(f_lisp.car(argsAndBody), f_lisp.cdr(f_lisp.cdr(argsAndBody)));
    }
    // Adds an implicit BLOCK with the same name as the defun around the definition.
    endArgsAndBody = f_lisp.makeList(f_lisp.car(endArgsAndBody),f_lisp.makeCons(BLOCK, f_lisp.makeCons(name, f_lisp.cdr(endArgsAndBody))));
    name.setf_symbol_function(
            compileList(f_lisp.MACHINE, f_lisp.makeCons(LAMBDA, endArgsAndBody),
                        f_lisp.makeCons(f_lisp.makeCons(name, NIL),
                                                  valueList),
                        f_lisp.makeCons(machine.STOP, NIL)).second());

    return
            compileList(f_lisp.MACHINE, f_lisp.makeCons(QUOTE, f_lisp.makeCons(name, NIL)),
                        f_lisp.makeCons(f_lisp.makeCons(name, NIL),
                                                  valueList),
                        code);
  }
	LispValue compileBlock(SECDMachine machine, LispValue name, LispValue argsAndBody,
	                       LispValue valueList, LispValue code)
	                    		   throws CompilerException
	{
		getLegalBlocks().push(name);
		final LispValue fullCode = list(cons(PROGN, argsAndBody));
		final LispValue compiledCode = compileArgsLeftToRight(fullCode, valueList, cons(machine.BLK,
		                                                                                cons(name, code)));
		getLegalBlocks().pop();
		return compiledCode;
	}

  //##JPG  method added, compile DEFMACRO, April 2005
  // todo: change name to LispSymbol
   LispValue compileDefmacro(SECDMachine machine, LispValue name, LispValue argsAndBody,
         LispValue valueList, LispValue code)
      throws CompilerException
  {
      LispValue tempNew = f_lisp.intern("%%%" + ((LispSymbol)name).symbol_name().toStringSimple(), f_lisp.SYSTEM);
      compileDefun(machine,tempNew,argsAndBody,valueList,code); // TODO, fix an ew method for doing this.
    //  ##JPG
    // for compilation of recursive macros, we need to know if the symbol under compilation is a
    // function or a macro. It's the aim of DUMMY_FUNCTION
    name.setf_symbol_function(DUMMY_MACRO);

    // todo: Figure out how to imbed BACKQUOTE in the compiler.
    //##JPG  MACRO keyword if added in front of code to signal this code is a macro
    //       setf_symbol_function() has been modified to detect this keyword
    //LispValue args = argsAndBody.first();
    //LispValue body = argsAndBody.second();
    //LispValue expandedBody = NIL;
    //if (body.first().eq(f_lisp.BACKQUOTE) != NIL)
//      expandedBody = f_lisp.backquote(body.second());
//    else
//      expandedBody = body;

    // OB:
    // Added support for documentation strings on defmacros.
    // Todo, fix support, so that DECLARE statements are ignored.
    // If the body of the macro only consists of a string, this is not taken as a
    // documentation string, as there would be no content of the macro otherwise.
    final LispValue possibleDocumentation = argsAndBody.second();
    LispValue endArgsAndBody = argsAndBody;
    if(possibleDocumentation instanceof LispString && argsAndBody.basic_length() > 2) {
        name.setf_documentation(f_lisp.intern("FUNCTION"),possibleDocumentation);
        endArgsAndBody = f_lisp.makeCons(f_lisp.car(argsAndBody), f_lisp.cdr(f_lisp.cdr(argsAndBody)));
    }

    name.setf_symbol_function(
        f_lisp.makeCons(  MACRO ,
                          compileList(f_lisp.MACHINE,
                                      f_lisp.makeCons(LAMBDA, endArgsAndBody), // f_lisp.makeList(args, expandedBody)),
                                      f_lisp.makeCons(f_lisp.makeCons(name, NIL),
                                                      valueList),
                                      f_lisp.makeCons(machine.STOP, NIL)).second()
        ));

    return
        compileList(f_lisp.MACHINE, f_lisp.makeCons(QUOTE, f_lisp.makeCons(name, NIL)),
                    f_lisp.makeCons(f_lisp.makeCons(name, NIL),
                                    valueList),
                    code);
  }

  // Optimization, 15 April 97, p. 174 of Kogge.
  // SEL followed by RTN can be optimized to do a RTN
  // at the end of each branch and eliminate the final RTN.

  LispValue compileIf(SECDMachine machine, LispValue test, LispValue thenExpr, LispValue elseExpr,
                      LispValue valueList, LispValue code)
    throws CompilerException
{
    if ((f_lisp.car(code) == machine.RTN)
      || (f_lisp.car(code) == machine.STOP))
      return compileOptimizedIf(machine, test, thenExpr, elseExpr, valueList, code);
    else
      return
              compile(test, valueList,
                      f_lisp.makeCons(machine.SEL,
                                                f_lisp.makeCons(compile(thenExpr, valueList,
                                                                                  f_lisp.makeCons(machine.JOIN, NIL)),
                                                                          f_lisp.makeCons(compile(elseExpr, valueList,
                                                                                                            f_lisp.makeCons(machine.JOIN, NIL)),
                                                                                                    code))));
  }


  LispValue compileOptimizedIf(SECDMachine machine, LispValue test, LispValue thenExpr,
                               LispValue elseExpr, LispValue valueList,
                               LispValue code)
          // Remove final RTN and optimize by putting RTN in branches.
    throws CompilerException

  {
    return
            compile(test, valueList,
                    f_lisp.makeCons(machine.TEST,
                                              f_lisp.makeCons(compile(thenExpr, valueList,
                                                                                f_lisp.makeCons(f_lisp.car(code), NIL)),
                                                                        compile(elseExpr, valueList, code))));
  }


  LispValue compileProgn(LispValue body, LispValue valueList, LispValue code)
    throws CompilerException
  {
    if (body == NIL)
      return code;
    else
      return compile(f_lisp.car(body), valueList,
                     compileProgn(f_lisp.cdr(body), valueList, code));
  }
    /*
    private java.util.Map blocks = new java.util.HashMap();

  LispValue compileBlock(final SECDMachine machine, final LispValue body, final LispValue valueList, final LispValue code) throws CompilerException {
      System.err.println("We have a block");
      final LispValue tag = body.car();
      System.err.println("Tag " + tag);
      java.util.Stack stBlock = (java.util.Stack)blocks.get(tag);
      if(null == stBlock) {
          System.err.println("Creating new stack for this tag");
          stBlock = new java.util.Stack();
          blocks.put(tag,stBlock);
      }
      final LispValue uhm = f_lisp.makeList(machine.S.value(),machine.E.value(),machine.C.value(),machine.D.value());
      stBlock.push(uhm);
      final int size = stBlock.size();
      final LispValue rest = f_lisp.makeCons(PROGN,body.cdr());
      final LispValue afterC = compile(machine,rest,valueList);
      System.err.println("After compilation");
      if(stBlock.size() == size) {
          System.err.println("Popping block");
          System.err.println(stBlock.pop());
      }
      return afterC;
  }
    */


  LispValue compileLambda(SECDMachine machine, LispValue body, LispValue valueList, LispValue code)
    throws CompilerException
  {
    // System.out.print("\nCompile Lambda: "); body.prin1();
    // System.out.print("\n code = "); code.prin1();
    return f_lisp.makeCons(machine.LDF,
                           f_lisp.makeCons(compile(body, valueList,
                                                   f_lisp.makeCons(machine.RTN, NIL)),
                                           code));
  }


  // Contributed by Jean-Pierre Gaillardon, April 2005
  /**
   * @param code a Lisp list
   * @return true if code is code for a macro (the first element is :MACRO)
   */
  public static boolean isMacroCode(LispValue code)
  {
    return (code instanceof LispList) && (Lisp.car(code) == MACRO);
  }

	// init
	private void registerAccessorFunctions()
	{
		Register(new LispPrimitive1(f_lisp, "CAR") {
			@Override
			protected LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).car();
				throw new LispValueNotAConsException(arg);
			}
		});
		Register(new LispPrimitive1(f_lisp, "CDR") {
			@Override
			protected LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).cdr();
				throw new LispValueNotAConsException(arg); 
			}
		});
		Register(new LispPrimitive2(f_lisp, "ELT") {
			protected LispValue Execute(LispValue list, LispValue n) {
				return list.elt(n);
			}
		});
	}
	
	
	// util functions
	public static boolean is_atom(LispValue value)
	{
		return (value instanceof LispAtom || value == NIL);
	}
	public static boolean is_null(LispValue value)
	{
		return (value == NIL);
	}
	public final LispCons cons(LispValue car, LispValue cdr)
	{
		return new StandardLispCons(f_lisp, car, cdr);
	}
	public final LispList list(LispValue... parts)
	{
		LispList result = NIL;
		for (int i = parts.length-1 ; i >= 0; i--)
			result = cons(parts[i], result);
		return result;
	}
	public LispValue car(LispValue value)
	{
		return ((LispList)value).car();
	}
	public LispValue cdr(LispValue value)
	{
		return ((LispList)value).cdr();
	}
}
