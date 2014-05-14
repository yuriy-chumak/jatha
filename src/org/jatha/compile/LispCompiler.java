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

import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;

import org.jatha.Lisp;
import org.jatha.LispProcessor;
import org.jatha.compile.LispExtension;
import org.jatha.dynatype.*;
import org.jatha.exception.*;
import org.jatha.machine.*;
import org.jatha.read.LispParser;

import static org.jatha.dynatype.LispValue.*;
import static org.jatha.machine.SECDMachine.*;

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
public class LispCompiler extends LispProcessor
{
	// Set this to true to produce debugging output during compilation.
	static boolean DEBUG = false;

//	static final LispValue QUOTE     = LispValue.QUOTE;
	static final LispSymbol PROGN    = symbol("PROGN");
	static final LispSymbol DEFUN    = symbol("DEFUN");
	static final LispSymbol BLOCK    = symbol("BLOCK");

	// These are "special forms" that get expanded in the compiler
	LispValue AND;
	LispValue DEFMACRO;
	LispValue IF;
	LispValue LAMBDA;
	LispValue LET;
	LispValue SET;
	LispValue LETREC;
	LispValue OR;

	static final LispSymbol AMP_REST = symbol("&REST");   // keyword &rest used in parameters list
	LispValue DUMMY_FUNCTION; // used for recursive definions
	LispValue DUMMY_MACRO;    // used for recursive definions

	LispPrimitive CONS;
	LispPrimitive LIST;
	LispPrimitive SETQ; // special for SET ' (does not evaluates first argument)

	Map<LispValue, Compiler> SpecialOperators = null;
	interface Compiler {
		public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code)
				throws CompilerException;
	}
  

	boolean WarnAboutSpecialsP = false;    // todo: Need some way to turn this on.
	private Lisp f_lisp = null;
	public Lisp getLisp() { return f_lisp; }
  

	// static initializer.
	@SuppressWarnings("serial")
	private void initializeConstants()
	{
		f_lisp.intern("&REST", AMP_REST);
    
		SpecialOperators = new TreeMap<LispValue, Compiler>() {{
			put(QUOTE, new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileQuote(args, valueList, code);
					}
				});
			put(PROGN, new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileProgn(args, valueList, code);
					}
				});
			put(DEFUN, new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileDefun(machine, car(args), cdr(args), valueList, code);
					}
				});
			put(BLOCK, new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileBlock(machine, car(args), cdr(args), valueList, code);
					}
				});

			put(LET = f_lisp.intern("LET"), new Compiler() {
				@Override
				public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
					LispList vars      = varsFromLetBindings((LispList)car(args));
					LispList values    = valuesFromLetBindings((LispList)car(args));
//						LispValue newValues = cons(vars, valueList);

					LispCons body      = cons(PROGN, cdr(args));
					return compileLet(machine, vars, values, valueList, body, code);
				}
			});
			put(SET = f_lisp.intern("SET"), new Compiler() {
				@Override
				public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
					return compileArgsLeftToRight(args, valueList, cons(ST, code));
				}
			});
			
			
			put(LAMBDA = f_lisp.intern("LAMBDA"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileLambda(machine, cons(PROGN, cdr(args)),
						                     cons(car(args), valueList), code);
					}
				});
			put(DEFMACRO = f_lisp.intern("DEFMACRO"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileDefmacro(machine, car(args), cdr(args), valueList, code);
					}
				});
			put(AND = f_lisp.intern("AND"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileAnd(machine, args, valueList, code);
					}
				});
			put(OR = f_lisp.intern("OR"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileOr(machine, args, valueList, code);
					}
				});
			put(IF = f_lisp.intern("IF"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						return compileIf(machine, args.first(), args.second(), args.third(),
		                           valueList, code);
					}
				});
			put(LETREC = f_lisp.intern("LETREC"), new Compiler() {
					@Override
					public LispCons compile(SECDMachine machine, LispList args, LispList valueList, LispCons code) throws CompilerException {
						LispList vars      = varsFromLetBindings((LispList)car(args));
						LispList values    = valuesFromLetBindings((LispList)car(args));
						LispList newValues = cons(vars, valueList);

						LispCons body      = cons(PROGN, cdr(args));
						
						return cons(DUM,
						            compileApp(machine, values, newValues,
						            		compileLambda(machine, body, newValues,
						            				cons(RAP, code))));
					}
				});
		}};

		// should be used only to test type. basic_macrop() retutns true for DUMMY_MACRO and false for DUMMY_FUNCTION
		// this is NOT builtin function and macro
		DUMMY_FUNCTION = new StandardLispFunction(null, cons(T, NIL));
		DUMMY_MACRO    = new StandardLispMacro   (null, cons(T, NIL));
	}

	public LispCompiler(Lisp lisp)
	{
		super();
		f_lisp = lisp;

		initializeConstants();
	}

	Set<String> requires = new HashSet<String>();
	List<String> defaultPackages = new ArrayList<String>() {{
		add("");
		add("org.jatha.extras.");
		add("org.jatha.extension.");
	}};
	/**
	 * 1. try to load class as default
	 * 2. try to load file as default
	 * 3. try to load class as org.jatha.extension."name"
	 * @param value
	 * @return
	 */
	public final LispValue require(String module)
	{
		if (requires.contains(module))
			return T;
		ClassLoader classLoader = LispCompiler.class.getClassLoader();
		for (String pkg : defaultPackages)
		{
			// classLoader.loadClass
			try {
				// 1. search for the .class for executa as native code
				LispExtension extension = (LispExtension)Class.forName(
						pkg + module, true, classLoader
						).newInstance();
				extension.Register(LispCompiler.this);
				
				requires.add(module);
				return T;
			}
			catch (ClassNotFoundException ex) {
				// class not found, continue.
			}
			catch (ClassCastException ex) {
				// class cast exception, this module not a lisp extension, continue
			}
			catch (Exception ex) {
				throw new LispException("Can't load required " +
						module + " module.");
			}
			// try to load text file, not a class
			Reader resourceReader;
			try {
				resourceReader = new InputStreamReader(
						classLoader.getResourceAsStream(pkg.replace('.', '/') + module)
				);
				LispParser cli = new LispParser(f_lisp, resourceReader);
				while (true) {
					f_lisp.MACHINE.Execute(compile(f_lisp.MACHINE, cli.read(), NIL), NIL);
				}
			}
			catch (EOFException ex) // ok. loaded.
			{
				requires.add(module);
				return T;
			}
			catch (NullPointerException ex)
			{
				// file not found, continue search 
			}
			catch (LispUndefinedFunctionException ufe) {
				System.err.println("ERROR: " + ufe.getMessage());
				return string(ufe.getMessage());
			}
			catch (CompilerException ce) {
				System.err.println("ERROR: " + ce);
				return string(ce.toString());
			}
			catch (LispException le) {
				System.err.println("ERROR: " + le.getMessage());
				le.printStackTrace();
				return string(le.getMessage());
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new LispException("Can't load required " +
						module + " module.");
			}
			
			// temporary copy-paste. need to change.
			// try to load text file, not a class
			try {
				resourceReader = new InputStreamReader(
						classLoader.getResourceAsStream(pkg.replace('.', '/') + module + ".lisp")
				);
				LispParser cli = new LispParser(f_lisp, resourceReader);
				while (true) {
					f_lisp.MACHINE.Execute(compile(f_lisp.MACHINE, cli.read(), NIL), NIL);
				}
			}
			catch (EOFException ex) // ok. loaded.
			{
				requires.add(module);
				return T;
			}
			catch (NullPointerException ex)
			{
				// file not found, continue search 
			}
			catch (LispUndefinedFunctionException ufe) {
				System.err.println("ERROR: " + ufe.getMessage());
				return string(ufe.getMessage());
			}
			catch (CompilerException ce) {
				System.err.println("ERROR: " + ce);
				return string(ce.toString());
			}
			catch (LispException le) {
				System.err.println("ERROR: " + le.getMessage());
				le.printStackTrace();
				return string(le.getMessage());
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new LispException("Can't load required " +
						module + " module.");
			}
			
			
		}

		return NIL;
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
		Register(new LispPrimitiveC("REQUIRE", 1) { 
			protected LispValue Execute(LispList values)
					throws CompilerException
			{
				if (values instanceof LispSymbol) {
					return require(values);
				}
				
				if (values instanceof LispCons) {
					for (Iterator<LispValue> valuesIt = values.iterator(); valuesIt.hasNext(); ) 
						require(valuesIt.next());
				    return T;
				}
				
				throw new LispValueNotASymbolOrConsException(values);
			}
			LispValue require(LispValue value)
			{
//				assertSymbol(value);
				if (value instanceof LispString ||
					value instanceof LispSymbol) {
					return LispCompiler.this.require(value.toStringSimple());
				}
	/*				if (value instanceof LispSymbol) {
					String name = "org.jatha.extras." + value.toStringSimple().replace('-','.');
					return require(name);
				}*/
				throw new LispValueNotASymbolException(value);
			}
		});


		// inline primitives
		Register(CONS = new LispPrimitive2("CONS") {
			public LispValue Execute(LispValue a, LispValue b) {
				return cons(a, b);
			}
		});
		Register(LIST = new LispPrimitiveC("LIST", 0) {
			@Override
			public LispCons CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue function,
							LispList args, LispList valueList, LispCons code)
					throws CompilerException
			{
				return CompileArgs(compiler, machine, args, valueList, code);
			}
			@Override
			protected LispValue Execute(LispList arg) throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
			}
			
		});
		Register(SETQ = new LispPrimitive2("SETQ") {
			// if somebody want's to skip putting func as argument, must override this method
			@Override
			public LispCons CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue function,
							LispList args, LispList valueList, LispCons code)
					throws CompilerException
			{
				LispCons lookupVal = indexAndAttribute(args.first(), valueList);
				LispValue value = lookupVal.second();
				if (value == NIL) // not a local variable (inside a LET or something like that)
					value = args.first();
				
				return cons(LDC,
				            cons(value,
				                 compiler.compile(args.second(), valueList, cons(ST, code))));
			}
			@Override
			protected LispValue Execute(LispValue arg1, LispValue arg2)
					throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
			}
		});
		
		
/*		Register(new LispPrimitive2("SET") {
			@Override
			protected LispValue Execute(LispValue arg1, LispValue arg2)
					throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
			}
			
			public void Execute(SECDMachine machine)
			{
				LispValue val = machine.S.pop();
				LispValue sym = machine.S.pop();

				if (sym instanceof LispCons) {   // local variable
					LispCons ij = (LispCons)sym;
					LispCons valueList = (LispCons)machine.E.value();
					LispValue newValue = val;
			    	
					nth(ij, valueList).rplaca(newValue);
				}
				else
				if (sym instanceof LispSymbol) {
					if (sym.specialP())  // special variable
						machine.special_set(sym, val);
					else  // global variable
						sym.setf_symbol_value(val);
				}
				else
					throw new LispValueNotASymbolException(sym);

				machine.S.push(val);
				machine.C.pop();
			}
		});*/
		
		/*compiler.Register(new InlineLispPrimitive(f_lisp, "LIST*", 1, Long.MAX_VALUE) {
			LispValue CONS = new ConsPrimitive(f_lisp);
			public LispValue CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispValue args, final LispList valueList, final LispValue code)
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

		Register(new LispPrimitive0("EXIT") {
			protected LispValue Execute() {
				System.exit(0);
				return T; // no exit, actually
			}
		});
		
		
		// comparators
		
		Register(new LispPrimitive2("EQ") {
			public LispValue Execute(LispValue a, LispValue b) {
				if (is_atom(a) && is_atom(b))
					return a.eq(b);
				return NIL;
			}
		});
		Register(new LispPrimitive2("EQL") {
			public LispValue Execute(LispValue a, LispValue b) {
				return a.eql(b);
			}
		});
		// todo: maybe need to be builin ?
		Register(new LispPrimitive1("NOT") {
			protected LispValue Execute(LispValue arg) {
				return arg == NIL ? T : NIL;
			}
		});

		require("FUNCTION");

		registerAccessorFunctions();
		require("MATH"); // register Math Functions

//		registerStringFunctions(SYSTEM_PKG);

		Register(new LispPrimitive1("LAST") {
			protected LispValue Execute(LispValue a) {
				return a.last();
			}			
		});
		Register(new LispPrimitive1("LENGTH") {
			protected LispValue Execute(LispValue a) {
				return integer(a.length());
			}			
		});
		
		Register(new LispPrimitiveC("APPEND", 0) {
			protected LispValue Execute(LispList args) {
				return appendArgs(args);
			}
			
			// This is right-recursive so it only copies each arg once.
			// The last arg is not copied, of course.
			LispValue appendArgs(LispValue args)
			{
				if (cdr(args) == NIL)
					return car(args);
				else
					return ((LispList)car(args)).append(appendArgs(cdr(args)));
			}
		});
		
		
		// Required by macroses (i.e. cond)
		// simple type checks
		Register(new LispPrimitive1("ATOM?") {
			protected LispValue Execute(LispValue arg) {
				return BOOL(is_atom(arg));
			}
		});
		Register(new LispPrimitive1("NULL?") {
			protected LispValue Execute(LispValue arg) {
				return BOOL(is_null(arg));
			}
		});
		Register(new LispPrimitive1("CONS?") {
			protected LispValue Execute(LispValue a) {
				return (a instanceof LispCons) ? T : NIL;
			}
		});
		Register(new LispPrimitive1("LIST?") {
			protected LispValue Execute(LispValue a) {
				return (a instanceof LispList) ? T : NIL;
			}
		});
		// complex
		Register(new LispPrimitive1("CONSTANT?") {
			protected LispValue Execute(LispValue a) {
				return BOOL(a.constantp());
			}
		});

	
		Register(new LispPrimitive1("SQRT") {
			protected LispValue Execute(LispValue a) {
				return a.sqrt();
			}
		});
		
		Register(new LispPrimitive1("PRINT") {
			protected LispValue Execute(LispValue a) {
				return a.print();
			}
		});
		
		require("BACKQUOTE");
		require("SETF");
		
		Register(new LispPrimitive1("MACROEXPAND") {
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
		        final LispValue carForm = car(form); // todo: check for LispSymbol
		        if (carForm.fboundp() && carForm.symbol_function() instanceof LispMacro) {
		            return f_lisp.eval(f_lisp.makeCons(f_lisp.intern("%%%" + ((LispSymbol)carForm).symbol_name().toStringSimple()), quoteList(cdr(form))));
		        } else {
		            return form;
		        }
		    }

		    private LispValue quoteList(final LispValue intern) {
		        LispValue ret = NIL;
		        for(final Iterator<LispValue> iter = intern.iterator(); iter.hasNext();) {
		            final LispValue curr = (LispValue)iter.next();
		            ret = cons(list(QUOTE, curr), ret);
		        }
		        return ret.nreverse();
		    }
			
		});
		Register(new LispPrimitive1("MACROEXPAND-1") {
			protected LispValue Execute(LispValue form) {
		        final LispValue carForm = car(form);	// todo: check for LispSymbol
		        
		        if(carForm.fboundp() && carForm.symbol_function() instanceof LispMacro) {
		            return (f_lisp.eval(f_lisp.makeCons(f_lisp.intern("%%%" + ((LispSymbol)carForm).symbol_name().toStringSimple()), quoteList(f_lisp.cdr(form)))));
		        } else {
		            return (form);
		        }
			}
		    private LispValue quoteList(final LispValue intern) {
		        LispValue ret = NIL;
		        for(final Iterator<LispValue> iter = intern.iterator();iter.hasNext();) {
		            final LispValue curr = (LispValue)iter.next();
		            ret = cons(list(QUOTE,curr),ret);
		        }
		        return ret.nreverse();
		    }
		});

		Register(new LispPrimitiveC("RETURN-FROM", 1, 2) {
			@Override
			public LispCons CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispList args, final LispList valueList, final LispCons code) throws CompilerException {
				final LispValue tag = car(args);
				if (!compiler.getLegalBlocks().contains(tag))
					throw new LispAssertionException("No enclosing lexical block with tag " + tag);

				final LispValue fullCode = cdr(args);
				final LispCons compiledCode = compiler.compileArgsLeftToRight(fullCode, valueList,
						cons(LIS,
						     cons(integer(fullCode.length()),
						          cons(LDC,
						               cons(tag, code)))));
				return compiledCode;
			}

			public void Execute(SECDMachine machine)
					throws CompilerException
			{
				final LispValue tag = machine.S.pop();
				final LispValue args = machine.S.pop();
				final LispValue retVal = (args.basic_length() == 0) ? NIL : car(args);
				machine.S.push(retVal);
				findBlock(tag,machine);
			}
			private void findBlock(final LispValue tag, final SECDMachine machine) throws CompilerException 
			{
				LispValue currVal = null;
				while (true) {
					currVal = machine.C.pop();
					while (
						currVal != NIL &&
						currVal != RTN && currVal != RTN_IF && currVal != RTN_IT && currVal != JOIN &&
						currVal != BLK/* && !(currVal instanceof TagbodyPrimitive)*/
					)
						currVal = machine.C.pop();
					if (currVal == BLK) 
					{
						currVal = machine.C.pop();
						if (tag == currVal) 
							return; // We found the place!
						continue;
					}

					if (currVal == RTN || currVal == RTN_IF || currVal == RTN_IT || currVal == JOIN)
						((LispPrimitive)currVal).Execute(machine);
					else
						throw new IllegalArgumentException("RETURN-FROM called with in bad form, no matching block outside");
					/* else if (currVal instanceof TagbodyPrimitive) {
			        machine.C.push(currVal);
			        ((LispPrimitive)currVal).Execute(machine);
			      } */
				}
			}
			
			@Override
			protected LispValue Execute(LispList arg) throws CompilerException {
				throw new LispAssertionException();
			}});

		require("Strings");
	}
	
	public LispValue eval(String expression)
			throws CompilerException
	{
		return f_lisp.eval(expression);
	}
	public LispValue load(Reader expression)
			throws CompilerException
	{
		return f_lisp.load(expression);
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
//		f_lisp.intern(primitive.LispFunctionNameString(), primitive);
		final LispSymbol symbol = f_lisp.intern(primitive.LispFunctionNameString());
		symbol.setf_symbol_function(primitive);//list(PRIMITIVE, primitive));
	}

	// required by "return-from" primitive
	private final Stack<LispValue> legalBlocks = new Stack<LispValue>();
	public Stack<LispValue> getLegalBlocks() {
		return legalBlocks;
	}

//   public Stack<Set<LispValue>> getLegalTags() {
//        return legalTags;
//    }

//    public Map<Long, LispValue> getRegisteredGos() {
//      return registeredGos;
//    }

//    public boolean isLegalTag(final LispValue tag) {
//        for(final java.util.Iterator<Set<LispValue>> iter = legalTags.iterator();iter.hasNext();) {
//            if(iter.next().contains(tag)) {
//                return true;
//            }
//        }
//        return false;
//    }

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
  public static int indexInList(LispValue e, LispValue l, LispValue[] attribute)
  {
    int indexInList = 1;
    LispValue list = l ;
    LispValue previousAttribute = NIL;
    for(;;list = cdr(list))
    {
      if(list == NIL)
      {
        return 0;  // not found
      }
      LispValue elt = car(list);
      if (elt == AMP_REST)
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
	public static LispCons indexAndAttribute(LispValue e, LispValue l)
	{
		if (l == NIL)
			return cons(NIL, NIL);
		int indexSubList = 1;
		LispValue subList = l;
		LispValue[] attribute = new LispValue[] { NIL };

		for (;; indexSubList++, subList = cdr(subList))
		{
			if (subList == NIL)
				return cons(NIL, NIL); // not found
			int indexInSubList = indexInList(e, car(subList), attribute);
			if (indexInSubList != 0) // found
			{
				LispValue position = cons(cons(integer(indexSubList),
				                               integer(indexInSubList)),
				                          NIL);
				return cons(attribute[0], position);
			}
		}
	}


	// This places the args on the stack so that they will be evaluated L->R,
	// as is required in Common LISP.
	// todo: speedup!
	public LispCons compileArgsLeftToRight(LispValue args,
					LispList valueList,
					LispCons code)
			throws CompilerException
	{
/*		was:
		LispValue rest = code;
		if (args != NIL) {
			List<LispValue> list = args.toRandomAccess();
			for (int i = list.size() - 1; i >= 0; i--)
				rest = compile(list.get(i), valueList, rest);
		}
		return rest;*/
		if (args == NIL)
			return code;
		
		return compile(car(args), valueList,
				compileArgsLeftToRight(cdr(args), valueList, code));
	}
	
	// This places the args on the stack L->R, but unevaluated.
	//
	public LispCons compileConstantArgsLeftToRight(LispValue args,
					LispList valueList,
					LispCons code)
	{
		if (args == NIL)
			return code;
		
		return cons(LDC,
		            cons(car(args),
		                 compileConstantArgsLeftToRight(cdr(args), valueList, code)));
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
	public LispCons compile(SECDMachine machine, LispValue expr, LispList varValues)
			throws CompilerException
	{
		if (DEBUG) {
			System.out.println("expr = " + expr);
			System.out.println("varValues = " + varValues);
			System.out.println("STOP = " + STOP);
			System.out.println("NIL = " + NIL);
			System.out.println("initial code = " + cons(STOP, NIL));
		}

		return compile(expr, varValues, cons(STOP, NIL));
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
	public LispCons compile(LispValue expr, LispList valueList, LispCons code)
			throws CompilerException
	{
		SECDMachine machine = f_lisp.MACHINE;
		
		if (DEBUG) {
			System.out.print("\nCompile: " + expr);
			System.out.print("\n   code: " + code);
		}

		if (expr instanceof LispAtom)
			return compileAtom(machine, (LispAtom)expr, valueList, code);
		else
			return compileList(machine, (LispList)expr, valueList, code);
	}


	LispCons compileAtom(SECDMachine machine, LispAtom expr, LispList valueList, LispCons code)
	{
		if (DEBUG) {
			System.out.print("\nCompile Atom: " + expr);
			System.out.print(" of type " + expr.getClass().getName());
			System.out.flush();
		}

		if (expr == T)
			return cons(LDT, code);

		// todo: remove LispConstant from code as unneeded
		if (expr instanceof LispConstant)
			return cons(LDC, cons(expr, code));

		if (expr instanceof LispSymbol) {
			//LispValue varIndex = index(expr, valueList);
			//##JPG use indexAndAttributes() instead of index
			LispValue varIdxAndAttributes = indexAndAttribute(expr, valueList);
			LispValue paramAttribute      = car(varIdxAndAttributes);
			LispValue varIndex            = car(cdr(varIdxAndAttributes));

			if (varIndex == NIL)
			{
				if (!expr.specialP() && WarnAboutSpecialsP) /* Not a local variable, maybe it's global */
					System.err.print("\n;; ** Warning - " + expr.toString() + " assumed special.\n");

				return cons(LD_GLOBAL, cons(expr, code));
			}
			else  /* Found the symbol.  Is it bound? */
			{
		        //##JPG opcode LDR instead of LD for variable arguments
		        // note : paramAttribute can only be nil or &rest
				LispValue loadOpCode = (paramAttribute == AMP_REST) ? LDR : LD;
				return cons(loadOpCode, cons(varIndex, code));
			}
		}
		
		return cons(LDC, cons(expr, code));
	}

	LispCons compileList(SECDMachine machine, LispList expr, LispList valueList, LispCons code)
			throws CompilerException
	{
		if (DEBUG) {
			System.out.print("\nCompile List: " + expr);
		}
		
		if (expr == NIL)
			return cons(LDNIL, code);

		LispValue function = car(expr);
		LispList args      = (LispList)cdr(expr);

		// comment, progn, defun, quote, block, lambda, defmacro, and, or, if, let, letrec
		Compiler specialCompiler;
		if ((specialCompiler = SpecialOperators.get(function)) != null)
			return specialCompiler.compile(machine, args, valueList, code);
		
		if (function instanceof LispPrimitive) {
			// special for manual add for LIST etc.
			LispPrimitive primitive = (LispPrimitive)function;
			if (!primitive.validArgumentList(args))
				throw new ArgumentCountMismatchException(primitive, args.length());

			return primitive.CompileArgs(this, machine, function, args, valueList, code);
		}

		// user-defined function
		// todo: remove LispFunction from code as unneeded (?)
		// TODO: change LispFunction to LispPrimitive
		if (function instanceof LispFunction && !(function instanceof LispMacro)) // was: basic_functionp()
		{	// todo: doesn't called for now - maybe bug?
			LispFunction lFunc = (LispFunction) function;
//			if (lFunc.isBuiltin())
//				return compileBuiltin(machine, function, args, valueList, code);
			return compileUserDefinedFunction(machine, function, args, valueList, code);
		}

		// Function on a symbol
		if (function instanceof LispAtom) // what if NIL?
		{
			if (Lisp.isBuiltinFunction(function))
				return compileBuiltin(machine, function, args, valueList, code);

			// ordinary function
			{
				// ##JPG compileSpecialForm() has been modified to support DEFMACRO
				// LispValue defn = index(function, valueList);
				// ##JPG if function has a variable number of parameters (&rest is present in paraameters list)
				//   the opcode LDR (LoaD with Rest) is used in place of LD
				LispValue fnIdxAndAttributes = indexAndAttribute(function, valueList);
				LispValue defn = car(cdr(fnIdxAndAttributes));
				LispValue loadOpCode = (car(fnIdxAndAttributes) == AMP_REST) ? LDR : LD;

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
				if (function.symbol_function() instanceof LispMacro)
					//------------------------ compile macro --------------------------------
				{
					if (car(defn) instanceof LispNumber) /* macro present in closure */
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
				if (car(defn) instanceof LispNumber)
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
                               LispList valueList,
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


	public LispCons compileLet(SECDMachine machine,
					LispList vars, LispList values,
					LispList valueList,
					LispValue body, LispCons code)
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
			if (car(varPtr).specialP())
			{
				specialVars = cons(car(varPtr), specialVars);
				specialVals = cons(car(valPtr), specialVals);
			}
			else
			{
				localVars   = cons(car(varPtr), localVars);
				localVals   = cons(car(valPtr), localVals);
			}

			varPtr = cdr(varPtr);
			valPtr = cdr(valPtr);
		}

		// The local vars get compiled by the compileApp,
		// the special vars get compiled after that and just
		// before the Lambda is compiled.
		LispCons ret =
				compileApp(machine, localVals, valueList,
						compileSpecialBind(machine, specialVars, specialVals, valueList,
								compileLambda(machine, body, cons(localVars, valueList),
										cons(AP,
										     compileSpecialUnbind(machine, specialVars, code)))));
		return ret;
		// (code.car() == machine.RTN) ?
		// f_lisp.makeCons(machine.DAP, code.cdr())
		//			        : f_lisp.makeCons(machine.AP, code));
	}


  // UTILITY functions for LET

	// Inserts special-bind opcode for each var.
	LispCons compileSpecialBind(SECDMachine machine, LispValue vars, LispValue values,
						LispList valueList, LispCons code)
			throws CompilerException
	{
		if (vars == NIL)
			return code;
		return compile(car(values), valueList,
				cons(SP_BIND,
				     cons(car(vars),
				          compileSpecialBind(machine, cdr(vars), cdr(values), valueList, code))));
	}

	// Inserts special-bind opcode for each var.
	LispCons compileSpecialUnbind(SECDMachine machine, LispValue vars, LispCons code)
	{
		if (vars == NIL)
			return code;
		return cons(SP_UNBIND,
		            cons(car(vars),
		                 compileSpecialUnbind(machine, cdr(vars), code)));
	}


	// each entry is (VAR VAL) or VAR.  Latter has implied value of NIL.
	public LispList varsFromLetBindings(LispList varValueList) // is it true, that varValueList is List ? 
	{
		if (varValueList == NIL)
			return NIL;
		
		if (car(varValueList) instanceof LispCons)
			return cons(car(car(varValueList)),
			            varsFromLetBindings((LispList)cdr(varValueList)));
		return cons(car(varValueList),
		            varsFromLetBindings((LispList)cdr(varValueList)));
	}

	// each entry is (VAR VAL) or VAR.  Latter has implied value of NIL.
	public LispList valuesFromLetBindings(LispList varValueList)
	{
		if (varValueList == NIL)
			return NIL;
		
		if (car(varValueList) instanceof LispCons)
			return cons(car(varValueList).second(), // todo: change second to cdr().first()  
			            valuesFromLetBindings((LispList)cdr(varValueList)));
		return cons(NIL,
		            valuesFromLetBindings((LispList)cdr(varValueList)));
	}

  /* obsolete 1 Sep 2004 (mh)
  boolean builtinFunctionP(LispValue fn)
  {
    if ((! fn.basic_symbolp()) || (fn.fboundp() != T))
      return false;

    LispValue defn = fn.symbol_function();

    if (defn == null)
      return false;

    if ((defn.listp() == T) && (defn.first() == PRIMITIVE))
      return true;
    else
      return false;
  }
  */
  // This version of 'compileApp' is modified from the version in Kogge's
  // book.  It puts args on stack in the correct L->R order and does
  // not require the caller to prepend a NIL instruction on the
  // resulting code.
	LispCons compileApp(SECDMachine machine, LispValue args, LispList valueList, LispCons code)
			throws CompilerException
	{
		if (DEBUG)
			System.out.print("\nCompile App: " + args + ", valueList = " + valueList);

		if (args == NIL)
			return cons(LDNIL, code);
		return compile(car(args), valueList,
				compileApp(machine, cdr(args), valueList, cons(CONS, code)));
	}


	// ##JPG added
	// similar to compileApp() but doesn't evaluate parameters
	LispCons compileAppConstant(SECDMachine machine, LispList args, LispList valueList, LispCons code)
	{
		if (DEBUG)
			System.out.print("\nCompile AppConstant: " + args + ", valueList = " + valueList + ", code = " + code);
    
		List<LispValue> list = args.toRandomAccess();
		if (args != NIL)
			for (int i = list.size() - 1; i >= 0; i--)
				code = cons(CONS, code);
		code = cons(LDNIL, code);
		
		LispCons rest = code;
		if (args != NIL)
			for (int i = list.size() - 1; i >= 0; i--)
				rest = cons(LDC, cons(list.get(i), rest));
		return rest;
	}

	LispCons compileBuiltin(SECDMachine machine, LispValue fn, LispList args,
					LispList valueList, LispCons code) throws CompilerException
	{
		if (DEBUG)
			System.out.print("\nCompile Builtin: " + fn + "  " + args);

		if (fn instanceof LispSymbol)
			if (fn.fboundp())
				fn = fn.symbol_function();
	
		if (fn instanceof LispFunction)
			fn = ((LispFunction)fn).getCode();

		assert fn instanceof LispPrimitive;

		// Builtin LISP primitives have a symbol-function of the
		// form (:PRIMITIVE <ic>).  We call the CompileArgs functions
		// of the primitive instruction.
		LispPrimitive executableCode = (LispPrimitive)fn;
/*
    if (is_atom(fn))
      executableCode = ((LispFunction)fn.symbol_function()).getCode().second();
    else if (fn.basic_functionp())
      executableCode = (((LispFunction)fn).getCode()).second();
*/
		if (!executableCode.validArgumentList(args))
			throw new ArgumentCountMismatchException(executableCode,
                                               args.basic_length());

		return executableCode.CompileArgs(this, machine, executableCode, args, valueList, code);
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
	LispCons compileUserDefinedFunction(SECDMachine machine, LispValue fn,
					LispList args,
					LispList valueList, LispCons code) throws CompilerException
	{
		if (DEBUG)
			System.out.print("\nCompile user-defined: (" + fn + "  " + args + "), vl = " + valueList);

		LispCons executableCode = (LispCons)((LispFunction)fn).getCode();

    // Assume that the arguments are correct ?
    // TODO: how do we check arguments?  Besides, we've lost the name of the function.
    /*
    if (!((LispPrimitive)(executableCode.second())).validArgumentList(args))
      throw new ArgumentCountMismatchException(((LispString)fn.symbol_name()).getValue(),
                                               ((LispPrimitive)(executableCode.second())).parameterCountString(),
                                               ((LispInteger)(args.length())).getLongValue());
    */

		return compileArgsLeftToRight(args, valueList, (LispCons)executableCode.append(code));
	}


	LispCons compileAnd(SECDMachine machine, LispList args, LispList valueList, LispCons code)
			throws CompilerException
	{
		// No args: return default value of T
		if (args == NIL)
			return cons(LDT, code);

		// 1 arg: just compile the argument.
		if (cdr(args) == NIL)
			return compile(car(args), valueList, code);

		// Multiple arguments: construct an IF statement
		// (let ((*dummy* args.first())) (if ...))

		LispValue var = new StandardLispSymbol(); // here add new temporary variable
		return
		compile(list(LET,
		             cons(list(var, car(args)), NIL),
		             list(IF, var, compileAndAux(var, cdr(args)), NIL)),
		        valueList, code);
	}
	LispValue compileAndAux(LispValue var, LispValue args)
	{
		if (cdr(args) == NIL)
			return car(args);

		return list(PROGN,
		            list(SETQ, var, car(args)),
		            list(IF, var, compileAndAux(var, cdr(args)), NIL));
	}


	LispCons compileOr(SECDMachine machine, LispList args, LispList valueList, LispCons code)
			throws CompilerException
	{
		// No args: return default value of NIL
		if (args == NIL)
			return cons(LDNIL, code);

		// 1 arg: just compile the argument.
		if (cdr(args) == NIL)
			return compile(car(args), valueList, code);

		// Multiple arguments: construct an IF statement
		// (let ((*dummy* args.first())) (if ...))

		LispValue var = new StandardLispSymbol(); // here add new temporary variable
		return
		compile(list(LET,
	                 cons(list(var, car(args)), NIL),
	                 list(IF, var, var, compileOrAux(var, cdr(args)))),
	            valueList, code);
	}
	LispValue compileOrAux(LispValue var, LispValue args)
	{
		if (cdr(args) == NIL)
			return car(args);

		return list(PROGN,
		            list(SETQ, var, car(args)),
		            list(IF, var, var, compileOrAux(var, cdr(args))));
	}


	LispCons compileQuote(LispList argsAndBody, LispList valueList, LispCons code)
					throws CompilerException
	{
		LispValue f = argsAndBody.first();
		if (f instanceof LispSymbol) {
			LispCons lookupVal = indexAndAttribute(f, valueList);
			
			if (lookupVal.second() != NIL)  // local var, inside a LET or something like that.
				return cons(LDC,
				            cons(lookupVal.second(), code));
		}
		return cons(LDC, cons(f, code)); // compileQuote(args, code);
	}
	
	LispCons compileDefun(SECDMachine machine, LispValue name, LispValue argsAndBody,
	                       LispList valueList, LispCons code)
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
		
		if (possibleDocumentation instanceof LispString && argsAndBody.basic_length() > 2)
		{
		    /*public LispValue documentation(final LispValue type) {
		        if(!(type instanceof LispSymbol)) {
		            throw new LispValueNotASymbolException("The second argument to DOCUMENTATION");
		        }
		        final LispValue val = (LispValue)f_documentation.get(type);
		        return (val == null) ? NIL : val;
		    }
		
		    public LispValue setf_documentation(final LispValue type, final LispValue value) {
		        if(!(type instanceof LispSymbol)) {
                    throw new LispValueNotASymbolException("The second argument to SETF-DOCUMENTATION");
            }
		        if(!(value instanceof LispString) && value != NIL) {
		            throw new LispValueNotAStringException("The third argument to SETF-DOCUMENTATION");
		        }
		        f_documentation.put(type,value);
		        return value;
		    }*/

			//name.setf_documentation(f_lisp.intern("FUNCTION"), possibleDocumentation);
			endArgsAndBody = cons(car(argsAndBody), cddr(argsAndBody));
		}
		// Adds an implicit BLOCK with the same name as the defun around the definition.
		endArgsAndBody = list(car(endArgsAndBody), cons(BLOCK, cons(name, cdr(endArgsAndBody))));
		name.setf_symbol_function(
				compileList(machine, cons(LAMBDA, endArgsAndBody),
						cons(cons(name, NIL), valueList),
                        cons(STOP, NIL)).second());
		
		return compileList(machine, cons(QUOTE, cons(name, NIL)),
									cons(cons(name, NIL), valueList),
							code);
	}
	LispCons compileBlock(SECDMachine machine, LispValue name, LispValue argsAndBody,
					LispList valueList, LispCons code)
			throws CompilerException
	{
		getLegalBlocks().push(name);
		final LispList fullCode = list(cons(PROGN, argsAndBody));
		final LispCons compiledCode = compileArgsLeftToRight(fullCode, valueList, cons(BLK, cons(name, code)));
		getLegalBlocks().pop();
		return compiledCode;
	}

	//##JPG  method added, compile DEFMACRO, April 2005
	// todo: change name to LispSymbol
	LispCons compileDefmacro(SECDMachine machine, LispValue name, LispValue argsAndBody,
					LispList valueList, LispCons code)
			throws CompilerException
	{
		LispValue tempNew = f_lisp.intern("%%%" + ((LispSymbol)name).symbol_name().toStringSimple());
		compileDefun(machine, tempNew, argsAndBody, valueList, code); // TODO, fix an ew method for doing this.
		
		//  ##JPG
		// for compilation of recursive macros, we need to know if the symbol under compilation is a
		// function or a macro. It's the aim of DUMMY_MACRO
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
		if (possibleDocumentation instanceof LispString && argsAndBody.basic_length() > 2) {
			//name.setf_documentation(f_lisp.intern("FUNCTION"),possibleDocumentation);
			endArgsAndBody = f_lisp.makeCons(f_lisp.car(argsAndBody), f_lisp.cdr(f_lisp.cdr(argsAndBody)));
		}

		name.setf_symbol_function(cons(MACRO, // same as defun function, but with MACRO keyword
				compileList(machine, cons(LAMBDA, endArgsAndBody), // f_lisp.makeList(args, expandedBody)),
						cons(cons(name, NIL), valueList),
						cons(STOP, NIL)).second()));

		return compileList(machine, cons(QUOTE, cons(name, NIL)),
		                            cons(cons(name, NIL), valueList),
		                   code);
	}

	// Optimization, 15 April 97, p. 174 of Kogge.
	// SEL followed by RTN can be optimized to do a RTN
	// at the end of each branch and eliminate the final RTN.

	LispCons compileIf(SECDMachine machine, LispValue test, LispValue thenExpr, LispValue elseExpr,
						LispList valueList, LispCons code)
			throws CompilerException
	{
		if ((car(code) == RTN) || (car(code) == STOP))
			return compileOptimizedIf(machine, test, thenExpr, elseExpr, valueList, code);
		return compile(test, valueList,
				cons(SEL,
				     cons(compile(thenExpr, valueList, cons(JOIN, NIL)),
				          cons(compile(elseExpr, valueList, cons(JOIN, NIL)),
				               code))));
	}


	LispCons compileOptimizedIf(SECDMachine machine, LispValue test, LispValue thenExpr,
								LispValue elseExpr, LispList valueList,
								LispCons code)
          // Remove final RTN and optimize by putting RTN in branches.
			throws CompilerException
	{
		return compile(test, valueList,
				cons(TEST,
				     cons(compile(thenExpr, valueList, cons(car(code), NIL)),
				          compile(elseExpr, valueList, code))));
	}


	LispCons compileProgn(LispValue body, LispList valueList, LispCons code)
			throws CompilerException
	{
		if (body == NIL)
			return code;
		return compile(car(body), valueList,
				compileProgn(cdr(body), valueList, code));
	}
    /*
    private java.util.Map blocks = new java.util.HashMap();

  LispValue compileBlock(final SECDMachine machine, final LispValue body, final LispList valueList, final LispValue code) throws CompilerException {
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


	LispCons compileLambda(SECDMachine machine, LispValue body, LispList valueList, LispCons code)
			throws CompilerException
	{
		// System.out.print("\nCompile Lambda: "); body.prin1();
		// System.out.print("\n code = "); code.prin1();
		return cons(LDF,
		            cons(compile(body, valueList, cons(RTN, NIL)),
		                 code));
	}

  
	// init
	private void registerAccessorFunctions()
	{
		Register(new LispPrimitive1("CAR") {
			@Override
			protected LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).car();
				throw new LispValueNotAConsException(arg);
			}
		});
		Register(new LispPrimitive1("CDR") {
			@Override
			protected LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).cdr();
				throw new LispValueNotAConsException(arg); 
			}
		});
		Register(new LispPrimitive2("ELT") {
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
}
