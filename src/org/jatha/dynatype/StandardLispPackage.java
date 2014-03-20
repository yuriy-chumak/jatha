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

package org.jatha.dynatype;

import java.io.*;

import org.jatha.Lisp;
import org.jatha.util.SymbolTable;


// date    Mon May  5 22:39:01 1997
/**
 * An implementation of ANSI Common LISP packages,
 * including defpackage, export, import, etc.
 *
 * @see org.jatha.Lisp
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 * @version 1.0
 * 
 * http://www.cs.cmu.edu/Groups/AI/html/cltl/cltl2.html
 */
public class StandardLispPackage extends StandardLispCons implements LispPackage
{
	protected Lisp f_lisp;	// todo: remove this!
	
/* ------------------  FIELDS   ------------------------------ */

	// author  Micheal S. Hewett    hewett@cs.stanford.edu
	// date    Mon May  5 22:46:11 1997
	/**
	 * SYMTAB is the local symbol table for this package.
	 */
	protected SymbolTable f_symbolTable;

	// author Ola Bini, ola.bini@itc.ki.se
	// date Sun May 22 20:27:00 2005
	/**
	 * The shadowing symbols of this package.
	 * Read CLTL, chapter 11.5 for more information.
	 */
	protected SymbolTable f_shadowingSymbols;

	// author  Micheal S. Hewett    hewett@cs.stanford.edu
	// date    Mon May  5 22:48:52 1997
	/**
	 * The LISP string giving the name of the package.
	 */
	protected LispValue f_name;     // A Lisp String
	protected LispValue f_nicknames;// List of Lisp Strings

	protected LispList f_uses;     // List of packages used by this one.

  /* ------------------  CONSTRUCTORS   ------------------------------ */

  // return the package
  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Mon May  5 22:39:01 1997
  /**
   * Creates a new package.  Caller should verify
   * that none exists by the same name before calling
   * this function.
   * @param name - a string giving the name of the new package
   */
  public StandardLispPackage(Lisp lisp, String name)
  {
    this(lisp, string(name));
  }


  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Mon May  5 22:39:01 1997
  /**
   * Creates a new package.  Caller should verify
   * that none exists by the same name before calling
   * this function.�
   * @param name - a LISP string giving the name of the package
   *
   */
  public StandardLispPackage(Lisp lisp, LispValue name)
  {
    this(lisp, name, null, null, null);
  }

  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Mon May  5 22:43:14 1997
  /**
   * Creates a package with a list of nicknames.
   * @param name - a LISP string giving the name.
   * @param nicknames - a LISP list of nicknames (all strings)
   *
   */
  public StandardLispPackage(Lisp lisp, LispValue name, LispValue nicknames)
  {
    this(lisp, name, nicknames, null, null);
  }

  public StandardLispPackage(Lisp lisp, LispValue name, LispValue nicknames,
                             LispList usesList)
  {
    this(lisp, name, nicknames, usesList, null);
  }


  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Mon May  5 22:43:14 1997
  /**
   * Creates a package with a list of nicknames and
   * a list of exported symbols.
   * @param pname a LISP string giving the name.
   * @param pnicknames a LISP list of nicknames (all strings)
   * @param puses a LISP list of exported symbols (all strings)
   * @param symtab a symbol table to use for this package.
   */
  public StandardLispPackage(Lisp lisp, LispValue pname, LispValue pnicknames,
		                 LispList puses, SymbolTable symtab)
  {
      this(lisp, pname, pnicknames, puses, symtab, null);
  }

  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Mon May  5 22:43:14 1997
  /**
   * Creates a package with a list of nicknames and
   * a list of exported symbols.
   * @param pname a LISP string giving the name.
   * @param pnicknames a LISP list of nicknames (all strings)
   * @param puses a LISP list of exported symbols (all strings)
   * @param symtab a symbol table to use for this package.
   * @param shadows a shadowing symbol table to use for this package.
   *
   */
  public StandardLispPackage(Lisp lisp, LispValue pname, LispValue pnicknames,
		                 LispList puses, SymbolTable symtab, final SymbolTable shadows)
  {
    f_lisp = lisp;

    if (pname == null) {
        f_name = NIL;
    } else if (pname instanceof LispSymbol) {
        f_name = ((LispSymbol)pname).symbol_name();
    } else {
        f_name = pname;
    }



    if (pnicknames == null)
      f_nicknames = NIL;
    else
      f_nicknames  = transformToStrings(pnicknames);

    if (puses == null)
      f_uses = NIL;
    else
      f_uses       = puses;

    if (symtab instanceof SymbolTable)
      f_symbolTable = symtab;
    else
      f_symbolTable = new SymbolTable(f_lisp);

    if (shadows instanceof SymbolTable)
      f_shadowingSymbols = shadows;
    else
      f_shadowingSymbols = new SymbolTable(f_lisp);
  }


  public SymbolTable getSymbolTable()
  {
    return f_symbolTable;
  }

/* ------------------  NON-LISP methods   ------------------------------ */

  public void internal_princ(PrintStream os)
  { os.print("#<The " + ((LispString)(f_name)).getValue() + " package>"); }

  public void internal_prin1(PrintStream os)
  { os.print("#<The " + ((LispString)(f_name)).getValue() + " package>"); }

  public void internal_print(PrintStream os)
  { os.print("#<The " + ((LispString)(f_name)).getValue() + " package>"); }


  public String toString()
  {
      if (f_nicknames != NIL && f_nicknames != null)
      return getAsString(f_lisp.car(f_nicknames)).getValue();
    else
      return ((LispString)(f_name)).getValue();
  }

    private LispValue transformToStrings(final LispValue list) {
        LispValue internal = list;
        LispValue build = NIL;
        while(internal != NIL) {
            build = f_lisp.makeCons(getAsString(f_lisp.car(internal)),build);
            internal = f_lisp.cdr(internal);
        }
        return build.reverse();
    }

    private LispString getAsString(final LispValue inp) {
    	// todo: check for LispSymbol
        return (LispString)(inp.basic_symbolp() ? ((LispSymbol)inp).symbol_name() : inp);
    }

	// Returns either NIL or the symbol.
	// Searches this package only for external symbols matching
	// the string.
	public LispSymbol getExternalSymbol(LispString str)
	{
		return f_symbolTable.get(str);
	}

	/**
	 * Stores a new symbol in this package.
	 * The lookup key is a LispString giving the print name.
	 * The value is a LispSymbol - or LispNil.
	 */
	public void addSymbol(LispString name, LispSymbol symbol)
	{
		f_symbolTable.put(name, symbol);
	}


	// Returns either NIL or the symbol.
	// Searches this package and recursively searches external
	// symbols of packages used by this package.
	public LispSymbol getSymbol(LispString str)
	{
		LispSymbol symbol = f_shadowingSymbols.get(str);

		if (symbol != null)
			return symbol;

		symbol = f_symbolTable.get(str);
		if (symbol != null)
			return symbol;

/*		// ELSE - search used packages
		LispValue p = f_uses;
		while (p != NIL)
		{
			symbol = ((LispPackage)(f_lisp.findPackage(f_lisp.car(p)))).getExternalSymbol(str);
			if (symbol != null)
				return symbol;
			else
				p = f_lisp.cdr(p);
		}*/

		// If all fails, return null.
		return null;
	}

/* ------------------  LISP methods   ------------------------------ */
  public LispString getName()
  {
    return (LispString)(f_name);
  }
}
