package org.jatha.extras;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.jatha.Tests;
import org.jatha.compile.LispCompiler;
import org.jatha.compile.LispExtension;
import org.jatha.compile.LispPrimitive;
import org.jatha.compile.LispPrimitive1;
import org.jatha.compile.LispPrimitive2;
import org.jatha.compile.LispPrimitiveC;
import org.jatha.dynatype.LispCons;
import org.jatha.dynatype.LispFunction;
import org.jatha.dynatype.LispList;
import org.jatha.dynatype.LispNumber;
import org.jatha.dynatype.LispSymbol;
import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispReal;
import org.jatha.exception.CompilerException;
import org.jatha.exception.LispAssertionException;
import org.jatha.exception.LispValueNotANumberException;
import org.jatha.exception.WrongArgumentTypeException;
import org.jatha.machine.SECDMachine;

public class FUNCTION implements LispExtension
{
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	@Override
	public void Register(final LispCompiler compiler)
	{
		final LispPrimitive EVAL;
		compiler.Register(EVAL = new LispPrimitive1("EVAL") {
			public void Execute(SECDMachine machine)
					throws CompilerException
			{
				machine.C.pop();  // Pop the EVAL command
				LispValue expr = machine.S.pop();
				LispCons code = compiler.compile(expr, NIL, (LispCons)machine.C.value());

				// Push the new code on the stack.
				machine.C.assign(code);
			}

			@Override
			protected LispValue Execute(LispValue arg) throws CompilerException {
				return null; //todo: throw new exception
			}});

		compiler.Register(new LispPrimitive1("FUNCTION") {
			@Override
			protected LispValue Execute(LispValue arg) throws CompilerException {
				if (arg instanceof LispSymbol)
					((LispSymbol)arg).symbol_function();
				return arg;
			}

			/**
			 * The argument is not evaluated.
			 */
			@Override
			public LispCons CompileArgs(LispCompiler compiler, SECDMachine machine, LispList args,
							LispList valueList, LispCons code)
					throws CompilerException
			{
				return compiler.compileConstantArgsLeftToRight(args, valueList, code);
			}});
		
		
		compiler.Register(new LispPrimitiveC("APPLY", 2) {
			public void Execute(SECDMachine machine)
					throws CompilerException
			{
				machine.C.pop();
			    
				LispValue args   = machine.S.pop();

				// The last arg must be a list.
				if (!validArgumentList(args))
					throw new WrongArgumentTypeException("APPLY", "", "");// todo: change this , "a CONS in the last argument",
//							"a " + car(fnArgs.last()).type_of().toString());*/
				
				LispValue fn     = car(args);
				LispValue fnArgs = cdr(args);
				
				machine.S.push(cons(fn,
				                    quoteList(constructArgList(fnArgs))));

			    // (mh) 4 Sep 2004
			    // This seems like a kludge, but I don't know how to get around it.
			    // if the fn is a user-defined function, we have to move the arguments to the E register.
				if ((fn instanceof LispFunction) && (! ((LispFunction)fn).isBuiltin()))
				{
					machine.S.pop();
					machine.S.push(list(fn));
					machine.E.push(fnArgs);
				}

				machine.C.push(EVAL);
			}
			
			/**
			 * Returns the input list with quotes added before every
			 * top-level expression.
			 */
			LispList quoteList(LispValue list)
			{
				/*				
				LispList rest = NIL;
				if (l != NIL)
				{
					List<LispValue> list = l.toRandomAccess();
					for (int i = list.size() - 1; i >= 0; i--)
					{
						rest = cons(list(QUOTE, list.get(i)),
						            rest);
					}
				}
				return rest;//*/
				LispCons head = cons(NIL, NIL);
				LispCons p = head;
				
				for (Iterator<LispValue> i = list.iterator(); i.hasNext();) {
					p.setf_cdr(cons(list(QUOTE, i.next()), NIL));
					p = (LispCons)cdr(p);
				}
				
				return (LispList)cdr(head);//*/
			}
			
			
			// The last arg is a list.  We need to cons the
			// rest onto the front of the list.
			LispValue constructArgList(LispValue args)
			{
				// The last argument is a list, and we need to quote
				// the values in that list.
				if (cdr(args) == NIL)
					return car(args);
				else
					return cons(car(args), constructArgList(cdr(args)));
			}

			public boolean validArgumentList(LispValue args)
			{
				// The last argument must be a CONS
//				if (car(args.last()) instanceof LispCons || car(args.last()) == NIL)
				if (car(args.last()) instanceof LispList) // (cons or nil)
					return super.validArgumentList(args);
				else
				{
					System.err.println(";; *ERROR*: Last argument to APPLY must be a CONS.");
					return false;
				}
			}
			

			@Override
			protected LispValue Execute(LispList arg) throws CompilerException {
				throw new LispAssertionException(LispFunctionNameString() + " was compiled - shouldn't have been.");
//				return null; //todo: throw new exception
			}});

		
/*		// todo: must be integrated, but needs to test
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
		
		InputStreamReader resourceReader = null;
		try
		{
			resourceReader = new InputStreamReader(
					FUNCTION.class.getClassLoader().getResourceAsStream(
							FUNCTION.class.getPackage().getName().replace(".", "/") +
							"/FUNCTION"
					));
			compiler.load(resourceReader);
			resourceReader.close();
	    }
		catch (CompilerException e) {
			e.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}