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

package org.jatha.machine;

import java.util.HashMap;

import org.jatha.Lisp;
import org.jatha.LispProcessor;
import org.jatha.dynatype.*;
import org.jatha.exception.*;
import org.jatha.compile.*;

import
static org.jatha.dynatype.LispValue.*;


// @date    Sat Feb  1 21:04:49 1997
/**
 * Landin's SECD Machine implementation
 * 
 * The SECDMachine contains the registers and
 * basic functionality of the SECD machine.
 * It exports constants corresponding to each
 * primitive machine instruction and to each
 * primitive LISP operation, so that the compiler
 * may insert them into code.
 *
 * A modification to the standard SECD machine is
 * the new 'B' register that handles dynamic
 * binding.
 * 
 * http://webdocs.cs.ualberta.ca/~you/courses/325/Mynotes/Fun/SECD-slides.html
 * http://akoub.narod.ru/funcbook/chapter4/c4.htm
 *
 * @see SECDop
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public class SECDMachine extends LispProcessor
{
	public static boolean DEBUG = false;

	// ------  Registers  --------------
	public final SECDRegister S = new SECDRegister("S-05171955");  // Stack register
	public final SECDRegister E = new SECDRegister("E-06141957");  // Environment register
	public final SECDRegister C = new SECDRegister("C-06151962");  // Control register
	public final SECDRegister D = new SECDRegister("D-06071966");  // Dump register
	// An X register for dumping tag information, as a stack. This is the same register as D, but not totally. =)
	public final SECDRegister X = new SECDRegister("X-02324255");

	// SPECIAL dynamic registers for machine:
	// The B register is for dynamic bindings.  It contains a hash table
	// that indexes on symbol name.  The value is a list of values,
	// the most recent value at the front of the list.
	//
	// There is a B register for each machine so that it will
	// function correctly in a multi-threaded environment.
//	public final SECDHashTable B = new SECDHashTable();
	public final HashMap<LispValue, LispValue> B = new HashMap<LispValue, LispValue>(103, 1.2f);
	
	// ------------------  BASIC MACHINE OPS   ------------------------------
	
	public final static SECDop BLK = new SECDop("BLK") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();  // pop the BLK
			machine.C.pop();  // pop the tag
		}
	};
	
	/**
	 * Pushes a nil pointer onto the stack
	 */
	public final static SECDop LDNIL = new SECDop("LDNIL") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
			machine.S.push(NIL);
		}
	};
	/**
	 * Pushes a T pointer onto the stack
	 */
	public final static SECDop LDT   = new SECDop("LDT") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
			machine.S.push(T);
		}
	};
	
	/**
	 * Pushes a constant argument onto the stack
	 */
	public final static SECDop LDC   = new SECDop("LDC") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
					
			machine.S.push(car(machine.C.value()));
			machine.C.pop();
		}
	};
	// set the variable value
	public final static SECDop ST   = new SECDop("ST") {
		@Override
		public void Execute(SECDMachine machine) {
/*			machine.C.pop();
					
			machine.S.push(car(machine.C.value()));
			machine.C.pop();*/
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
			machine.C.pop(); // ST
		}
	};
	public final static SECDop LDR   = new SECDop("LDR") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
			LispCons indexes = (LispCons)car(machine.C.value());
			machine.S.push(rest(indexes, (LispCons)machine.E.value()));

			machine.C.pop();
		}
	};

	/**
	 * Pushes the value of a variable onto the stack.
	 * The variable is indicated by the argument, a pair.
	 * The pair's car specifies the level, the cdr the position.
	 * So "(1 . 3)" gives the current function's (level 1) third parameter.
	 */
	public static final SECDop LD    = new SECDop("LD") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
			LispCons ij = (LispCons)car(machine.C.value());
			LispCons valueList = (LispCons)machine.E.value();
			LispCons r = (LispCons)nth(ij, valueList);
			
			machine.S.push(r.car());
			
			machine.C.pop();
		}
	};
	
	public static final SECDop LD_GLOBAL = new SECDop("LD_GLOBAL") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
		    LispValue symb = car(machine.C.value());
		    machine.S.push(get_special_value(machine, symb));

		    machine.C.pop(); // symbol
		}
		// Assume the caller has verified that this is a special variable.
		private LispValue get_special_value(SECDMachine machine, LispValue symbol)
		{
			if (symbol.get_specialCount() <= 0)
				return ((LispSymbol)symbol).symbol_value();
			
			LispValue value = machine.B.get(symbol);
			if (value == null)
				return NIL;
			return car(value);
		}
	};
	
	/**
	 * Takes one list argument representing a function. It constructs
	 * a closure (a pair containing the function and the current environment)
	 * and pushes that onto the stack.
	 */
	public final static SECDop LDF   = new SECDop("LDF") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
			
			LispValue code = machine.C.pop();  // Get the new code.
			machine.S.assign(cons(cons(code, machine.E.value()),
		                          machine.S.value())); 
		}
	};
	public final static SECDop LDFC  = new SECDop("LDFC") {
		@Override
		public void Execute(SECDMachine machine) {
			/* Make a closure and push it on the S Register. */
			machine.C.pop();   // pop the LDFC symbol.

			LispValue code = machine.C.pop().symbol_function();
			if (code instanceof LispFunction)
				code = ((LispFunction)code).getCode();

			machine.S.assign(cons(cons(code, machine.E.value()),
			                      machine.S.value()));
		}
	};
	
	
	/**
	 * Expects two list arguments, and pops a value from the stack.
	 * The first list is executed if the popped value was non-nil,
	 * the second list otherwise. Before one of these list pointers
	 * is made the new C, a pointer to the instruction following sel
	 * is saved on the dump.
	 */
	public static final SECDop SEL   = new SECDop("SEL") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();                    // Pop the SEL command.
			
			LispValue selector    = machine.S.pop();
			LispValue trueCodeBranch   = machine.C.pop();
			LispValue falseCodeBranch  = machine.C.pop();

			machine.D.push(machine.C.value());  // push remaining code.

			if (selector == NIL)
				machine.C.assign(falseCodeBranch);
			else
				machine.C.assign(trueCodeBranch);
		}
	};
	
	public static final SECDop TEST  = new SECDop("TEST") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();               /* Pop the TEST command. */
		    
			LispValue selector    = machine.S.pop();
			LispValue trueValue   = machine.C.pop();
			if (selector != NIL)
				machine.C.assign(trueValue);
		}
	};
	
	
	
	/**
	 * Pops a list reference from the dump and makes this the new value of C.
	 * This instruction occurs at the end of both alternatives of a sel.
	 */
	public final static SECDop JOIN  = new SECDop("JOIN") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.assign(machine.D.pop());
		}
	};
	
	/**
	 * Pops a closure and a list of parameter values from the stack.
	 * The closure is applied to the parameters by installing its environment as
	 * the current one, pushing the parameter list in front of that, clearing
	 * the stack, and setting C to the closure's function pointer.
	 * The previous values of S, E, and the next value of C are saved on the dump.
	 */
	public final static SECDop AP    = new SECDop("AP") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();   // Get rid of 'AP' opcode.
			
			LispCons fe = (LispCons)machine.S.pop();   /* (f . e) */
			LispValue v  = machine.S.pop();

			LispValue code = fe.car();
			if (code instanceof LispFunction)
				code = ((LispFunction)code).getCode();


			machine.D.assign(cons(machine.S.value(),
			                      cons(machine.E.value(),
			                           cons(machine.C.value(),
			                                machine.D.value()))));
			machine.C.assign(code);
			machine.E.assign(cons(v, fe.cdr()));
			machine.S.assign(NIL);
		}
	};
	
	/**
	 * Works like ap, only that it replaces an occurrence of a dummy environment with the current one, thus making recursive functions possible
	 */
	public final static SECDop RAP   = new SECDop("RAP") {
		@Override
		public void Execute(SECDMachine machine) {
			LispValue recursiveClosure = machine.S.pop();  /* (f . (nil.e1)) */
			LispValue v                = machine.S.pop();  /* v = list of closures */

			// machine.E.pop();
			machine.C.pop();


			/*
		      System.out.println("\nRAP:   closure = " + recursiveClosure);
		      System.out.println("\nRAP:   v       = " + v);
			 */



			/*
		    machine.D.push(machine.C.pop());
		    machine.D.push(machine.E.pop());
		    machine.D.push(machine.S.pop());
			 */
			/*
		    LispValue Evalue = machine.E.value();

		    machine.D.assign(f_lisp.makeCons(machine.S.value(),
		                                     f_lisp.makeCons(Evalue.cdr(),
		                                                     f_lisp.makeCons(machine.C.value(),
		                                                                     machine.D.value()))));

			 */
			LispValue e2 = machine.E.value();
			machine.D.assign(cons(machine.S.value(),
			                      cons(cdr(e2),
			                           cons(machine.C.value(),
			                                machine.D.value()))));

			machine.C.assign(car(recursiveClosure));  /* f */

			// The car of E should be rplaca'd with the list of closures
			//machine.E.assign(f_lisp.makeCons(v, recursiveClosure.cdr().cdr())); //  (v . e1)
			machine.E.value().rplaca(v);

			machine.S.assign(NIL);
		}
	};
	
	
	/**
	 * Pops one return value from the stack, restores S, E, and C from the dump,
	 * and pushes the return value onto the now-current stack.
	 */
	public final static SECDop RTN   = new SECDop("RTN") {
		@Override
		public void Execute(SECDMachine machine) {
			LispValue save = machine.S.pop();
//			machine.C.pop(); // Pop the RTN command. Can be skipped.

			machine.S.assign(cons(save, machine.D.pop()));
			machine.E.assign(machine.D.pop());
			machine.C.assign(machine.D.pop());
		}
	};
	public final static SECDop RTN_IF = new SECDop("RTN_IF") {
		@Override
		public void Execute(SECDMachine machine) {
			LispValue save = machine.S.pop();
			machine.C.pop();               /* Pop the RTN_IF command. */

			if (save == NIL) {
				machine.S.assign(cons(save, machine.D.pop()));
				machine.E.assign(machine.D.pop());
				machine.C.assign(machine.D.pop());
			}
		    // else do nothing and continue processing.
		}
	};
	public final static SECDop RTN_IT = new SECDop("RTN_IT") {
		@Override
		public void Execute(SECDMachine machine) {
			LispValue save = machine.S.pop();
			machine.C.pop();               /* Pop the RTN_IF command. */

			if (save != NIL) {
				machine.S.assign(cons(save, machine.D.pop()));
				machine.E.assign(machine.D.pop());
				machine.C.assign(machine.D.pop());
			}
		    // else do nothing and continue processing.
		}
	};
	
	/**
	 * DAP optimizes the   (... AP RTN) sequence in (... DAP).
	 */
	public final static SECDop DAP    = new SECDop("DAP") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();  // Pop DAD instruction
		    
			LispValue fe = machine.S.pop();   /* (f . e) */
			LispValue v  = machine.S.pop();

			machine.C.assign(car(fe));
			machine.E.assign(cons(v, cdr(fe)));
			machine.S.assign(NIL);
		}
	};
	
	
	/**
	 * Pushes a "dummy", an empty list, in front of the environment list.
	 */
	public final static SECDop DUM   = new SECDop("DUM") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.E.push(NIL);
			machine.C.pop();
		}
	};
	
	public final static SECDop LIS   = new SECDop("LIS") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();               /* Pop the LIS command. */
			
			long numArgs = ((LispInteger)machine.C.pop()).getLongValue();    /* Pop the number of args. */
			LispValue argList = NIL;
			for (int i=0; i < numArgs; ++i)
				argList = cons(machine.S.pop(), argList);

			machine.S.push(argList);
		}
	};
	
	public final static SECDop SP_BIND   = new SECDop("SP_BIND") {
		@Override
		public void Execute(SECDMachine machine) {
		    machine.C.pop();
		    
		    LispValue val = machine.S.pop();
		    LispValue sym = machine.C.pop();
		    machine.special_bind(sym, val);
		}
	};
	public final static SECDop SP_UNBIND = new SECDop("SP_UNBIND") {
		@Override
		public void Execute(SECDMachine machine) {
		    machine.C.pop();
		    
		    LispValue sym = machine.C.pop();   // Pop the symbol name
		    machine.special_unbind(sym);
		}
	};
	
	public final static SECDop STOP  = new SECDop("STOP") {
		@Override
		public void Execute(SECDMachine machine) {
			machine.C.pop();
		    /* How do we stop? */
		}
	};
	
//	public SECDop TAG_B = null;
	
	public SECDMachine()
	{
/*    
		TAG_B = new SECDop("TAG_B") {
			@Override
			public void Execute(SECDMachine machine) {
				machine.X.assign(
						cons(list(
						          machine.E.value(), machine.D.value(),
						          new StandardLispHashTable(lisp, (StandardLispHashTable)machine.B)),
						     machine.X.value()));
				
				machine.C.pop();
			}
		};*/
	}

/* ------------------  SPECIAL BINDING   ------------------------------ */

  // Assume the caller has verified that this is a special variable.
  public void special_bind(LispValue symbol, LispValue value)
  {
    if (symbol.constantp())
    {
      // Cause a LispConstant Redefined error
      symbol.setf_symbol_value(value);
    }
    else
    {
      LispValue bindings = B.get(symbol);
      if (bindings == null) bindings = NIL;

      B.put(symbol, cons(value, bindings));
      symbol.adjustSpecialCount(+1);
    }
  }


  public void special_unbind(LispValue symbol)
  {
    LispValue bindings = B.get(symbol);
    if (bindings == null) bindings = NIL;

    B.put(symbol, cdr(bindings));
    symbol.adjustSpecialCount(-1);
  }


  // Sets the binding of a special variable.
	// used by AND and OR primitives
  public void special_set(LispValue symbol, LispValue value)
  {
	  
    if (symbol.get_specialCount() > 0)
    {
        LispValue bindings = B.get(symbol);
        if (bindings == null) bindings = NIL;
        B.put(symbol, cons(value, cdr(bindings)));
    }
    else
      symbol.setf_symbol_value(value);
  }


	/**
	 * Executor
	 * @param code
	 * @param globals
	 * @return
	 * @throws CompilerException
	 */
	public LispValue Execute(LispValue code, LispList globals)
			throws CompilerException
	{
		LispValue opcode;

		// System.out.print("\nExecuting code: ");
		// code.prin1();

		S.assign(NIL);
		E.assign(globals);
		C.assign(code);
		D.assign(NIL);

		opcode = car(C.value());

		while ((opcode != STOP) && (opcode != NIL))
		{
			if (DEBUG)
			{
				// Test output.
				//
				System.out.print("\n  S: " + S.value());
				System.out.print("\n  E: " + E.value());
				System.out.print("\n  C: " + C.value());
				System.out.print("\n  D: " + D.value());
				System.out.print("\n  B: " + B.toString());
				System.out.print("\n  X: " + X.value());
				// System.out.print(" of class " + opcode.getClass().getName());
				System.out.print("\n" + opcode);   // Testing
				System.out.flush();
			}

			if (opcode == null) {
				System.err.println("internal error in Jatha.SECDMachine.Execute: opcode is null");
				System.err.println("remaining code is " + C.value().toString());
			}
			else
				((LispPrimitive)opcode).Execute(this);

			try {
				opcode = car(C.value());  // Each opcode pops the C register as necessary
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("\n  S: " + S.value());
				System.err.print("\n  E: " + E.value());
				System.err.print("\n  C: " + C.value());
				System.err.print("\n  D: " + D.value());
				System.out.print("\n  B: " + B.toString());
				System.out.print("\n  X: " + X.value());
				// System.out.print(" of class " + opcode.getClass().getName());
				System.err.print("\n" + opcode);   // Testing
				System.err.flush();

				opcode = NIL;
			}
		}

		return  car(S.value()); //  Top value on Stack is the return value.
	}

	public void setStackValue(SECDRegister e, LispValue val)
	{
	}
}

/**
 * SECDop is the abstract class that encompasses all SECD
 * machine ops.
 *
 * @see org.jatha.compile.LispPrimitive
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
abstract class SECDop extends LispPrimitive0
{
	public SECDop(String opName)
	{
		super(opName);
	}

	/**
	 * The output of this function is printed when the
	 * instruction needs to be printed.
	 */
	public String toString()
	{
		return "SECD." + functionName;
	}

	protected LispValue Execute()
			throws CompilerException
	{
		throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been."); 
	}
}