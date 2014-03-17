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

import org.jatha.Jatha;
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

	// These are special forms that get expanded in the compiler
	LispValue COMMENT;
	LispValue PROGN;
  LispValue AND;
  LispValue DEFMACRO;
  LispValue DEFUN;
  LispValue IF;
  LispValue LAMBDA;
  LispValue LET;
  LispValue LETREC;
  LispValue OR;
  LispValue PRIMITIVE;
  LispValue SETQ;
    //  LispValue WHEN;
	LispValue QUOTE;

  LispValue AMP_REST;   // keyword &rest used in parameters list
  LispValue MACRO;      // keyword used at begenning of macro code to detect macro
  LispValue DUMMY_FUNCTION; // used for recursive definions
  LispValue DUMMY_MACRO;    // used for recursive definions
  
	Map<LispValue, Compiler> SpecialOperators = null;
	interface Compiler {
		public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
				throws CompilerException;
	}
  

  boolean WarnAboutSpecialsP = false;    // todo: Need some way to turn this on.
  private Jatha f_lisp = null;
  public Jatha getLisp() { return f_lisp; }
  private final Stack<LispValue> legalBlocks = new Stack<LispValue>();
  private final Stack<Set<LispValue>> legalTags   = new Stack<Set<LispValue>>();
  private final Map<Long, LispValue> registeredGos = new HashMap<Long, LispValue>();

  // static initializer.
	private void initializeConstants()
	{
		final LispPackage keyPkg = (LispPackage)(f_lisp.findPackage("KEYWORD"));
		final LispPackage SYSTEM_PKG = (LispPackage)(f_lisp.findPackage("SYSTEM"));

		QUOTE      = f_lisp.EVAL.internAndExport("QUOTE",   SYSTEM_PKG);
		AMP_REST   = f_lisp.EVAL.internAndExport("&REST", SYSTEM_PKG);
		MACRO      = f_lisp.EVAL.internAndExport("MACRO", keyPkg);
		PRIMITIVE  = f_lisp.EVAL.internAndExport("PRIMITIVE", keyPkg);
		SETQ       = f_lisp.EVAL.internAndExport("SETQ", SYSTEM_PKG);
    //    sysPkg.export(BLOCK);
    //    WHEN       = f_lisp.EVAL.intern("WHEN");
    
		SpecialOperators = new TreeMap<LispValue, Compiler>() {{
			put(PROGN =
				f_lisp.EVAL.internAndExport("PROGN", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileProgn(args, valueList, code);
						}
				});
			put(COMMENT =
				f_lisp.EVAL.internAndExport("COMMENT", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return cons(machine.T, code);
						}
				});
			put(DEFUN =
				f_lisp.EVAL.internAndExport("DEFUN", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileDefun(machine, f_lisp.car(args), f_lisp.cdr(args), valueList, code);
						}
				});

			put(LAMBDA =
				f_lisp.EVAL.internAndExport("LAMBDA", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileLambda(machine, f_lisp.makeCons(PROGN, f_lisp.cdr(args)),
		                               f_lisp.makeCons(f_lisp.car(args), valueList), code);
						}
				});
			put(DEFMACRO =
				f_lisp.EVAL.internAndExport("DEFMACRO", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileDefmacro(machine, f_lisp.car(args), f_lisp.cdr(args), valueList, code);
						}
				});
			put(AND =
				f_lisp.EVAL.internAndExport("AND", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileAnd(machine, args, valueList, code);
						}
				});
			put(OR =
				f_lisp.EVAL.internAndExport("OR", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileOr(machine, args, valueList, code);
						}
				});
			put(IF =
				f_lisp.EVAL.internAndExport("IF", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							return compileIf(machine, args.first(), args.second(), args.third(),
			                           valueList, code);
						}
				});
			put(LET =
				f_lisp.EVAL.internAndExport("LET", SYSTEM_PKG),
					new Compiler() {
						@Override
						public LispValue compile(SECDMachine machine, LispValue args, LispValue valueList, LispValue code) throws CompilerException {
							LispValue vars      = varsFromLetBindings(car(args));
							LispValue values    = valuesFromLetBindings(car(args));
							LispValue newValues = cons(vars, valueList);

							LispValue body      = cons(PROGN, cdr(args));
							return compileLet(machine, vars, values, valueList, body, code);
						}
				});
			put(LETREC =
				f_lisp.EVAL.internAndExport("LETREC", SYSTEM_PKG),
					new Compiler() {
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
    DUMMY_FUNCTION = new StandardLispFunction(f_lisp, null, f_lisp.makeCons(f_lisp.T, f_lisp.NIL));
    DUMMY_MACRO    = new StandardLispMacro(f_lisp, null, f_lisp.makeCons(f_lisp.T, f_lisp.NIL));
	}

  public LispCompiler(Jatha lisp)
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
		// Here put a call to in-package, then to export. for these things. I guess register should call export.
		final LispPackage SYSTEM_PKG = (LispPackage)f_lisp.findPackage("SYSTEM");

		// (require '(pkg1 pkg2 ...)) or (require 'package)
		Register(new LispPrimitive(f_lisp, "REQUIRE", 1, Long.MAX_VALUE) { 
			public LispValue Execute(LispValue values)
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
		}, SYSTEM_PKG);


		// http://jtra.cz/stuff/lisp/sclr/index.html
		// http://habrahabr.ru/post/65791/
		// Basic simple functions
		Register(new LispPrimitive(f_lisp, "QUOTE", 1) {
			@Override
			public LispValue CompileArgs(LispCompiler compiler, SECDMachine machine, LispValue function,
							LispValue args, LispValue valueList, LispValue code)
					throws CompilerException
			{
				// Don't evaluate the arg. (load it as a constant)
				return cons(machine.LDC,
				            cons(args.first(), code));
			}
			@Override
			public void Execute(SECDMachine machine)
					throws CompilerException
			{
				System.err.println(LispFunctionNameString() + " was compiled - shouldn't have been.");
				machine.C.pop();
			}
		}, SYSTEM_PKG);
		Register(new LispPrimitive(f_lisp, "CONS", 2) {
			public LispValue Execute(LispValue a, LispValue b) {
				return
				f_lisp.makeCons(a, b);
			}
		}, SYSTEM_PKG);

		
		// 
		Register(new LispPrimitive(f_lisp, "ATOM", 1) {
			public LispValue Execute(LispValue arg) {
				return
				f_lisp.makeBool(is_atom(arg));
			}
		}, SYSTEM_PKG);
		Register(new LispPrimitive(f_lisp, "NULL", 1) {
			public LispValue Execute(LispValue arg) {
				return
				f_lisp.makeBool(is_null(arg));
			}
		}, SYSTEM_PKG);
		
		Register(new LispPrimitive(f_lisp, "EQ", 2) {
			public LispValue Execute(LispValue a, LispValue b) {
				if (is_atom(a) && is_atom(b))
					return f_lisp.makeBool(a == b);
				return f_lisp.NIL;
			}
		}, SYSTEM_PKG);
		Register(new LispPrimitive(f_lisp, "NOT", 1) {
			public LispValue Execute(LispValue a) {
				return f_lisp.makeBool(is_null(a));
			}
		}, SYSTEM_PKG);

		Register(new LispPrimitive(f_lisp, "SETQ", 2) {
			// todo: refactor this
			public void Execute(SECDMachine machine)
			{
			    LispValue val = machine.S.pop();
			    LispValue sym = machine.S.pop();

			    if (sym instanceof LispList)   // local variable
			      machine.LD.setComponentAt(sym, machine.E.value(), val);

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
				
				if (lookupVal.second() instanceof LispNil)  // SETQ of a global var
				return
						f_lisp.makeCons(machine.LDC,
				                f_lisp.makeCons(args.first(),
				                		compiler.compile(args.second(), valueList, code)));
				
				else  // SETQ of a local var, inside a LET or something like that.
				return	f_lisp.makeCons(machine.LDC,
								f_lisp.makeCons(lookupVal.second(),
										compiler.compile(args.second(), valueList, code)));
			}
		}, SYSTEM_PKG);
		
		
		Register(new ComplexLispPrimitive(f_lisp, "+", 0, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				if (args == f_lisp.NIL)
					return f_lisp.ZERO;
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).add(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		}, SYSTEM_PKG);
		Register(new ComplexLispPrimitive(f_lisp, "-", 1, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).sub(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		}, SYSTEM_PKG);
		Register(new ComplexLispPrimitive(f_lisp, "*", 0, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				if (args == f_lisp.NIL)
					return f_lisp.ONE;
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).mul(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		}, SYSTEM_PKG);
		Register(new ComplexLispPrimitive(f_lisp, "/", 1, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				LispValue x = f_lisp.car(args);
				if (x instanceof LispNumber)
					return ((LispNumber)x).div(f_lisp.cdr(args));
				
				throw new LispValueNotANumberException(x);
			}
		}, SYSTEM_PKG);
		
		registerAccessorFunctions(SYSTEM_PKG);
//		registerStringFunctions(SYSTEM_PKG);
		
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
      Register(primitive, f_lisp.PACKAGE);
  }


 /**
  * Use this function to register any new LISP primitives
  * that you create from Java code.  The compiler will
  * then recognize them and compile them appropriately.
  * This version of the constructor accepts a package in which
  * to intern the symbol.
  *
  * Example:
  * <pre>
  *   compiler.Register(new RevAppendPrimitive());
  * </pre>
  * @see LispPrimitive
  * @param primitive
  */
  public void Register(LispPrimitive primitive, LispPackage pkg)
  {
      final LispValue symbol = f_lisp.getEval().intern(primitive.LispFunctionNameString(), pkg);
      symbol.setf_symbol_function(f_lisp.makeList(PRIMITIVE, primitive));
      pkg.export(symbol);

  }


  /**
   * Use this function to register any new LISP primitives
   * that you create from Java code.  The compiler will
   * then recognize them and compile them appropriately.
   * This version of the constructor accepts a package in which
   * to intern the symbol.
   *
   * Example:
   * <pre>
   *   compiler.Register(new RevAppendPrimitive());
   * </pre>
   * @see LispPrimitive
   * @param primitive
   */
   public void Register(LispPrimitive primitive, String pkgName)
   {
       Register(primitive,(LispPackage)(f_lisp.findPackage(pkgName)));
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

/*  LispValue	loc(long y, LispValue z)
  {
    if (y == 1)
      return(z.car());
    else
      return loc(y-1, z.cdr());
  }


  LispValue getComponentAt(LispValue ij_indexes, LispValue valueList)
  {
    long i, j;

    i = ((LispInteger)(ij_indexes.car())).getLongValue();
    j = ((LispInteger)(ij_indexes.cdr())).getLongValue();

    return loc(j, loc(i, valueList));
  }


  LispValue index2(LispValue e, LispValue n, long j)
  {
    if (n == f_lisp.NIL)
      return n;
    else if (n.car() == e)
      return f_lisp.makeInteger(j);
    else
      return index2(e, n.cdr(), j+1);
  }


  LispValue index_aux(LispValue e, LispValue n, long i)
  {
    if (n == f_lisp.NIL)
      return n;
    else
    {
      LispValue j;

      j = index2(e, n.car(), 1);

      if (j == f_lisp.NIL)
        return index_aux(e, n.cdr(), i+1);
      else
        return f_lisp.makeCons(f_lisp.makeInteger(i), j);
    }
  }

  /**
   * Looks up the symbol in a list of lists.
   * Returns the index of the list in which it is found and
   * the index in that list.
   * Both indexes start from 1.
   * Returns NIL if not found.
   * Examples:
   * <pre>
   *     index(b, ((a b c) (d e f)) = (1 . 2)
   *     index(f, ((a b c) (d e f)) = (2 . 3)
   *     index(z, ((a b c) (d e f)) = NIL
   * </pre>
   * @param e a Symbol
   * @param n a list of lists
   * @return either NIL, if not found, or a Cons of 2 LispIntegers (a . b) indicating list number (a) and index into that list (b)
   */
/*  LispValue index(LispValue e, LispValue n)
  {
    return index_aux(e, n, 1);
  }
*/

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
    LispValue previousAttribute = f_lisp.NIL;
    for(;;list = f_lisp.cdr(list))
    {
      if(list == f_lisp.NIL)
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
      previousAttribute = f_lisp.NIL;
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
		LispValue[] attribute = new LispValue[] { f_lisp.NIL };

		for (;; indexSubList++, subList = f_lisp.cdr(subList))
		{
			if (subList == f_lisp.NIL)
				return cons(f_lisp.NIL, f_lisp.NIL); // not found
			int indexInSubList = indexInList(e, car(subList), attribute);
			if (indexInSubList != 0) // found
			{
				LispValue position = cons(cons(f_lisp.makeInteger(indexSubList),
				                               f_lisp.makeInteger(indexInSubList)),
				                          f_lisp.NIL);
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
		if (args != f_lisp.NIL)
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
		if (args == f_lisp.NIL)
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
		LispValue rest = f_lisp.NIL;
		if (l != f_lisp.NIL)
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
			System.out.println("NIL = " + f_lisp.NIL);
			System.out.println("initial code = " + f_lisp.makeCons(machine.STOP, f_lisp.NIL));
		}

		return compile(expr, varValues, cons(machine.STOP, f_lisp.NIL));
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

		if (expr == f_lisp.NIL)
			return cons(machine.NIL, code);
		
		if (expr == f_lisp.T)
			return cons(machine.T, code);

		if (expr.keywordp() == f_lisp.T)
			return cons(machine.LDC, cons(expr, code));
		
		if (expr instanceof LispSymbol) {
			//LispValue varIndex = index(expr, valueList);
			//##JPG use indexAndAttributes() instead of index
			LispValue varIdxAndAttributes = indexAndAttribute(expr, valueList);
			LispValue paramAttribute      = car(varIdxAndAttributes);
			LispValue varIndex            = car(cdr(varIdxAndAttributes));

			if (varIndex == f_lisp.NIL)
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
			if (isBuiltinFunction(function))
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

				if (defn == f_lisp.NIL)
				{
					try
					{
						defn = ((LispFunction)function.symbol_function()).getCode();
					}
					catch (LispException e)
					{
						defn = null;
					}


					if ((defn == f_lisp.NIL) || (defn == null))
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
                                                                f_lisp.makeCons(machine.NIL,
                                                                                f_lisp.makeCons(new ConsPrimitive(f_lisp),
                                                                                                compileLambda(machine, expr,  f_lisp.makeCons(
                                                                                                    f_lisp.makeCons(function, f_lisp.NIL),
                                                                                                    valueList),
                                                                                                              f_lisp.makeCons(machine.RAP,
                                                                                                                              f_lisp.NIL)))))));

						LispValue expandValue = machine.Execute(expandCode, f_lisp.NIL);
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
                                                                   f_lisp.makeCons(machine.NIL,
                                                                                 f_lisp.makeCons(new ConsPrimitive(f_lisp),
                                                                                                 compileLambda(machine, expr,
                                                                                                               f_lisp.makeCons(f_lisp.makeCons(function, f_lisp.NIL),
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
    return f_lisp.NIL;
  }


  public LispValue compileLet(SECDMachine machine,
                              LispValue vars, LispValue values,
                              LispValue valueList,
                              LispValue body, LispValue code)
    throws CompilerException
  {
    // Divide the variables into special and non-special var sets.
    // Special variables get extra binding instructions.

    LispValue specialVars = f_lisp.NIL;
    LispValue specialVals = f_lisp.NIL;
    LispValue localVars   = f_lisp.NIL;
    LispValue localVals   = f_lisp.NIL;
    LispValue varPtr      = vars;
    LispValue valPtr      = values;

    while (varPtr != f_lisp.NIL)
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
    if (vars == f_lisp.NIL)
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
    if (vars == f_lisp.NIL)
      return code;
    else
      return f_lisp.makeCons(machine.SP_UNBIND,
                                       f_lisp.makeCons(f_lisp.car(vars),
                                                                 compileSpecialUnbind(machine, f_lisp.cdr(vars), code)));
  }


  // each entry is (VAR VAL) or VAR.  Latter has implied value of NIL.
  public LispValue varsFromLetBindings(LispValue varValueList)
  {
    if (varValueList == f_lisp.NIL)
      return f_lisp.NIL;
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
    if (varValueList == f_lisp.NIL)
      return f_lisp.NIL;
    else if (f_lisp.car(varValueList) instanceof LispCons)
      return f_lisp.makeCons(f_lisp.car(varValueList).second(),
                                       valuesFromLetBindings(f_lisp.cdr(varValueList)));
    else
      return f_lisp.makeCons(f_lisp.NIL,
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

    if (args == f_lisp.NIL)
      return f_lisp.makeCons(machine.NIL, code);
    else
      return
              compile(f_lisp.car(args), valueList,
                      compileApp(machine, f_lisp.cdr(args), valueList,
                                 f_lisp.makeCons(new ConsPrimitive(f_lisp), code)));
  }


  // ##JPG added
  // similar to compileApp() but doesn't evaluate parameters
  LispValue compileAppConstant(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
  {
    if (DEBUG)
      System.out.print("\nCompile AppConstant: " + args + ", valueList = " + valueList + ", code = " + code);
    
    List<LispValue> list = args.toRandomAccess();
    if (args != f_lisp.NIL)
    {
      for (int i = list.size() - 1; i >= 0; i--)
      {
        code = f_lisp.makeCons(new ConsPrimitive(f_lisp), code);
      }
    }
    code = f_lisp.makeCons(machine.NIL, code);
    LispValue rest = code;
    if (args != f_lisp.NIL)
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
    LispValue executableCode = f_lisp.NIL;

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
    if (args == f_lisp.NIL)
      return f_lisp.makeCons(machine.T, code);

    // 1 arg: just compile the argument.
    if (f_lisp.cdr(args) == f_lisp.NIL)
      return compile(args.first(), valueList, code);

    // Multiple arguments: construct an IF statement
    // (let ((*dummy* args.first())) (if ...))

    LispValue dummyVar = f_lisp.EVAL.intern("*AND-DUMMY-VAR*");
    dummyVar.set_special(true);

    return compile(f_lisp.makeList(LET,
      f_lisp.makeCons(f_lisp.makeList(dummyVar, args.first()), f_lisp.NIL),
      f_lisp.makeList(IF, dummyVar, compileAndAux(dummyVar, f_lisp.cdr(args)), f_lisp.NIL)),
      valueList,
      code);
  }


  LispValue compileAndAux(LispValue dummyVar, LispValue args)
  {
    if (f_lisp.cdr(args) == f_lisp.NIL)
      return (f_lisp.car(args));

    return
            f_lisp.makeList(PROGN,
              f_lisp.makeList(SETQ, dummyVar, f_lisp.car(args)),
              f_lisp.makeList(IF, dummyVar,
                compileAndAux(dummyVar, f_lisp.cdr(args)),
                f_lisp.NIL));
  }



  LispValue compileOr(SECDMachine machine, LispValue args, LispValue valueList, LispValue code)
    throws CompilerException
  {
    // No args: return default value of NIL
    if (args == f_lisp.NIL)
      return f_lisp.makeCons(machine.NIL, code);

    // 1 arg: just compile the argument.
    if (f_lisp.cdr(args) == f_lisp.NIL)
      return compile(args.first(), valueList, code);

    // Multiple arguments: construct an IF statement
    // (let ((*dummy* args.first())) (if ...))

    LispValue dummyVar = f_lisp.EVAL.intern("*OR-DUMMY-VAR*");
    dummyVar.set_special(true);

    return compile(f_lisp.makeList(LET,
      f_lisp.makeCons(f_lisp.makeList(dummyVar, args.first()),
        f_lisp.NIL),
      f_lisp.makeList(IF, dummyVar,
        dummyVar,
        compileOrAux(dummyVar, f_lisp.cdr(args)))),
      valueList,
      code);
  }


  LispValue compileOrAux(LispValue dummyVar, LispValue args)
  {
    if (f_lisp.cdr(args) == f_lisp.NIL)
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
        name.setf_documentation(f_lisp.EVAL.intern("FUNCTION"),possibleDocumentation);
        endArgsAndBody = f_lisp.makeCons(f_lisp.car(argsAndBody), f_lisp.cdr(f_lisp.cdr(argsAndBody)));
    }
    // Adds an implicit BLOCK with the same name as the defun around the definition.
    endArgsAndBody = f_lisp.makeList(f_lisp.car(endArgsAndBody),f_lisp.makeCons(f_lisp.getEval().intern("BLOCK"),f_lisp.makeCons(name, f_lisp.cdr(endArgsAndBody))));
    name.setf_symbol_function(
            compileList(f_lisp.MACHINE, f_lisp.makeCons(LAMBDA, endArgsAndBody),
                        f_lisp.makeCons(f_lisp.makeCons(name, f_lisp.NIL),
                                                  valueList),
                        f_lisp.makeCons(machine.STOP, f_lisp.NIL)).second());

    return
            compileList(f_lisp.MACHINE, f_lisp.makeCons(QUOTE, f_lisp.makeCons(name, f_lisp.NIL)),
                        f_lisp.makeCons(f_lisp.makeCons(name, f_lisp.NIL),
                                                  valueList),
                        code);
  }

  //##JPG  method added, compile DEFMACRO, April 2005
  // todo: change name to LispSymbol
   LispValue compileDefmacro(SECDMachine machine, LispValue name, LispValue argsAndBody,
         LispValue valueList, LispValue code)
      throws CompilerException
  {
      LispValue tempNew = f_lisp.EVAL.intern("%%%" + ((LispSymbol)name).symbol_name().toStringSimple(), (LispPackage)f_lisp.findPackage("SYSTEM"));
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
    //LispValue expandedBody = f_lisp.NIL;
    //if (body.first().eq(f_lisp.BACKQUOTE) != f_lisp.NIL)
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
        name.setf_documentation(f_lisp.EVAL.intern("FUNCTION"),possibleDocumentation);
        endArgsAndBody = f_lisp.makeCons(f_lisp.car(argsAndBody), f_lisp.cdr(f_lisp.cdr(argsAndBody)));
    }

    name.setf_symbol_function(
        f_lisp.makeCons(  MACRO ,
                          compileList(f_lisp.MACHINE,
                                      f_lisp.makeCons(LAMBDA, endArgsAndBody), // f_lisp.makeList(args, expandedBody)),
                                      f_lisp.makeCons(f_lisp.makeCons(name, f_lisp.NIL),
                                                      valueList),
                                      f_lisp.makeCons(machine.STOP, f_lisp.NIL)).second()
        ));

    return
        compileList(f_lisp.MACHINE, f_lisp.makeCons(QUOTE, f_lisp.makeCons(name, f_lisp.NIL)),
                    f_lisp.makeCons(f_lisp.makeCons(name, f_lisp.NIL),
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
                                                                                  f_lisp.makeCons(machine.JOIN, f_lisp.NIL)),
                                                                          f_lisp.makeCons(compile(elseExpr, valueList,
                                                                                                            f_lisp.makeCons(machine.JOIN, f_lisp.NIL)),
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
                                                                                f_lisp.makeCons(f_lisp.car(code), f_lisp.NIL)),
                                                                        compile(elseExpr, valueList, code))));
  }


  LispValue compileProgn(LispValue body, LispValue valueList, LispValue code)
    throws CompilerException
  {
    if (body == f_lisp.NIL)
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
                                                   f_lisp.makeCons(machine.RTN, f_lisp.NIL)),
                                           code));
  }

  /**
   * Send in either code or a symbol with a function value.
   * Returns true only if the first element of the code list
   * is :PRIMITIVE.
   * @param code a LISP list.
   * @return true if the code indicates a built-in function
   */
  public static boolean isBuiltinFunction(LispValue code)
  {
    if ((code.basic_symbolp()) && (code.fboundp() == code.getLisp().T))
      code = code.symbol_function();

    if ((code == null) || (code == code.getLisp().NIL))
      return false;

    if (code.basic_functionp())
      code = ((LispFunction)code).getCode();

    return (code instanceof LispList && (code.first() == code.getLisp().EVAL.intern("PRIMITIVE",
                                   (LispPackage)(code.getLisp().findPackage("KEYWORD")))));
  }


  // Contributed by Jean-Pierre Gaillardon, April 2005
  /**
   * @param code a Lisp list
   * @return true if code is code for a macro (the first element is :MACRO)
   */
  public boolean isMacroCode(LispValue code)
  {
    return code instanceof LispList && (f_lisp.car(code) == MACRO);
  }

	// init
	private void registerAccessorFunctions(LispPackage pkg)
	{
		Register(new LispPrimitive(f_lisp, "CAR", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).car();
				throw new LispValueNotAConsException(arg);
			}
		}, pkg);
		Register(new LispPrimitive(f_lisp, "CDR", 1) {
			public LispValue Execute(LispValue arg) {
				if (arg instanceof LispList)
					return ((LispList)arg).cdr();
				throw new LispValueNotAConsException(arg); 
			}
		}, pkg);
		Register(new LispPrimitive(f_lisp, "ELT", 2) {
			public LispValue Execute(LispValue list, LispValue n) {
				return list.elt(n);
			}
		}, pkg);
	}
	
	
	// util functions
	public static boolean is_atom(LispValue value)
	{
		return (value instanceof LispNil ||
				value instanceof LispAtom);
	}
	public static boolean is_null(LispValue value)
	{
		return (value instanceof LispNil);
	}
	public final LispCons cons(LispValue car, LispValue cdr)
	{
		return f_lisp.makeCons(car, cdr);
	}
	public LispValue car(LispValue value)
	{
		return f_lisp.car(value);
	}
	public LispValue cdr(LispValue value)
	{
		return f_lisp.cdr(value);
	}
}
