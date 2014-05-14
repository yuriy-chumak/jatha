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

import org.jatha.Lisp;
import org.jatha.exception.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

// See LispValue.java
import java.util.List;


//-------------------------------  LispCons  --------------------------------

public class StandardLispCons extends StandardLispList implements LispCons
{
	public static final long serialVersionUID = 1L;
	public static boolean DEBUG = false;

	protected LispValue carCell;
	protected LispValue cdrCell;

	// change theCdr to LispConsOrNil
	public  StandardLispCons(LispValue theCar, LispValue theCdr)
	{
		if (theCar == null) {
			System.err.println("** LispCons: attempting to create a CONS when CAR=null.  Substituting NIL");
			if (DEBUG)
				showStackTrace();
			theCar = NIL;
		}
		carCell = theCar;

		if (theCdr == null) {
			System.err.println("** LispCons: attempting to create a CONS when CDR=null.  Substituting NIL");
			if (DEBUG)
				showStackTrace();
			theCdr = NIL;
		}
		cdrCell = theCdr;
	}

	public  StandardLispCons()
	{
		carCell = NIL;
		cdrCell = NIL;
	}


	public LispValue car() { return carCell; }
	public LispValue setf_car(LispValue value)
	{ 
		carCell = value;
		return car();
	}
	  
	public LispValue cdr() { return cdrCell; }
	public LispValue setf_cdr(LispValue value)
	{ 
		cdrCell = value;
		return cdr();
	}
	
	
	public LispValue first  () { return carCell; }
	public LispValue second () { return ((LispList)cdr()).first();  }
	public LispValue third  () { return ((LispList)cdr()).second(); }
	
// =-( unsorted )-=========================================	
  public void internal_princ(PrintStream os)
  {
    os.print("(");
    carCell.internal_princ(os);
    cdrCell.internal_princ_as_cdr(os);
    os.print(")");
  }

  public void internal_princ_as_cdr(PrintStream os)
  {
    os.print(" ");
    carCell.internal_princ(os);
    cdrCell.internal_princ_as_cdr(os);
  }


  public void internal_prin1(PrintStream os)
  {
    os.print("(");
    carCell.internal_prin1(os);
    cdrCell.internal_prin1_as_cdr(os);
    os.print(")");
  }

  public void internal_prin1_as_cdr(PrintStream os)
  {
    os.print(" ");
    carCell.internal_prin1(os);
    cdrCell.internal_prin1_as_cdr(os);
  }


  public void internal_print(PrintStream os)
  {
    os.print("(");
    carCell.internal_print(os);
    (cdrCell).internal_print_as_cdr(os);
    os.print(")");
  }


  public void internal_print_as_cdr(PrintStream os)
  {
    os.print(" ");
    carCell.internal_print(os);
    (cdrCell).internal_print_as_cdr(os);
  }


  public boolean constantp()
  { // returns true if the list evaluates to itself - if it is quoted.
    return carCell == QUOTE;
  }

  public int basic_length()
  {
    return length();
  }


  // author  Micheal S. Hewett    hewett@cs.stanford.edu
  // date    Wed Feb 19 17:18:50 1997
  /**
   * <code>toString()</code> returns a printed representation
   * of the form (as printed by <code>(prin1)</code>) in
   * a Java string.
   * @return String The value in a string.
   */
  public String toString()
  {
    return toString_internal(0, 0);
  }

  /**
   * This printer is protected and won't print more
   * than *PRINT-LENGTH* items or *PRINT-LEVEL* levels.
   * @param length
   * @param level
   */
  public String toString_internal(long length, long level)
  {
    StringBuffer buf = new StringBuffer();

    buf.append("(");
    buf.append(carCell.toStringAsCar_internal(length, level+1));
    buf.append(cdrCell.toStringAsCdr_internal(length+1, level));
    buf.append(")");

    return buf.toString();
  }

  /*
   (defun print-nested-sequence (self stream depth)
  (declare (ignore depth) (special *max-print-length* *cnesl-syntax*))
  (let ((data (nested-sequence-data self))
  (type (nested-sequence-type self)))
    (cond ((eql type 'char)
     (format stream "~s" (coerce data 'string)))
    ((= (length data) 0)
     (if *cnesl-syntax*
         (format stream "[]" vcode-sequence-letter)
       (format stream "#~a()" vcode-sequence-letter)))
    ((<= (length data) *max-print-length*)
     (if *cnesl-syntax*
         (format stream "[~s~{,~s~}]" (car data) (cdr data))
       (format stream "#~a~s" vcode-sequence-letter data)))
    (t 
     (let ((data (subseq data 0 *max-print-length*)))
       (if *cnesl-syntax*
     (format stream "[~s~{,~s~},...]" (car data) (cdr data))
         (format stream "#~a(~{~s ~}...)" vcode-sequence-letter)))))))
         
   */

  /**
   * *PRINT-LEVEL* is usually small enough to not have a stack overflow,
   * so we use recursion here.  Probably should be written like
   * toStringAsCdr_internal, though.
   */
  public String toStringAsCar_internal(long length, long level)
  {
    if (level > Lisp.PRINT_LEVEL_VALUE)
    {
      System.err.println("Printing list deeper than *PRINT-LEVEL*.  Truncated.");
      return "...";
    }
    
    // else
    return toString_internal(length, level +1);
   }

  /**
   * Counts cdrs so as not to have runaway lists.
   */
  public String toStringAsCdr_internal(long length, long level)
  {
    LispValue    ptr = this;
    StringBuffer buf = new StringBuffer();

    long maxLength = Lisp.PRINT_LENGTH_VALUE;
    while (length <= maxLength)
    {
      if (ptr == NIL)
        return buf.toString();
      buf.append(" ");
      buf.append(car(ptr).toString_internal(length, level+1));
      length++;
      ptr = cdr(ptr);
      if (! (ptr instanceof LispCons))
      {
        buf.append(ptr.toStringAsCdr());
        ptr = NIL;
      }
    }

    System.err.println("Printing list...longer than *PRINT-LENGTH*.  Truncated.");
    System.err.println("Next few items are: ");
    for (int i=0; i<10; ++i)
    {
      if (! (ptr instanceof LispCons))
        break;
      System.err.println("    " + car(ptr));
      ptr = cdr(ptr);
    }
    return "...";
  }


  public String toStringAsCdr()
  {
    StringBuffer buf = new StringBuffer();

    buf.append(" ");
    buf.append(carCell.toString());
    buf.append(cdrCell.toStringAsCdr());

    return buf.toString();
  }

  /**
   * Wrapper for member().
   * @return true if the object is in the list.
   */
  public boolean contains(LispValue object)
  {
    return (member(object) != NIL);
  }

  /**
   * Returns the Lisp value as a Collection.  Most useful
   * for lists, which are turned into Collections.
   * But also works for single values.
   */
  public Collection<LispValue> toCollection()
  {
    List<LispValue> result = new ArrayList<LispValue>(this.basic_length());

    for (Iterator iterator = this.iterator(); iterator.hasNext();)
      result.add((LispValue)iterator.next());

    return result;
  }
  

  // --------  LISP methods  --------------

  public LispValue assoc(LispValue index)
  {
    LispValue  ptr = this;
    LispValue  value;

    while (ptr != NIL)
    {
      value = car(ptr);

      if (!(ptr instanceof LispCons))
      {
        throw new LispValueNotAListException("An argument to ASSOC");
      }

      if (index.eql(car(value)) == T)
      {
        return value;
      }
      ptr = cdr(ptr);
    }
    return NIL;
  }

  public LispValue     copy_list    ()
  {
    return cons(car(), cdr().copy_list());
  }

  /**
   * Returns a full copy of any list, tree, array or table,
   * copying all the leaf elements.
   * Atoms like symbols, and numbers are not copied.
   * In Java, a string is not mutable so strings are also not copied.
   */
  public LispValue copy()
  {
    return cons(carCell.copy(), cdrCell.copy());
  }


  public LispValue     equal        (LispValue value)
  {
    if (! (value instanceof LispCons))
      return NIL;
    else
    {
      boolean result = ((carCell.equal(car(value)) != NIL) &&
              (cdrCell.equal(cdr(value)) != NIL));
      if (result)
        return T;
      else
        return NIL;
    }
  }

  public LispValue last()
  {
    LispValue ptr = this;
    long      len = 0;  // To prevent runaway lists.
    long      maxLength = Lisp.MAX_LIST_LENGTH_VALUE;

    while (Lisp.cdr(ptr) != NIL)
      if (!(ptr instanceof LispCons))
      {
        throw new LispValueNotAListException("An argument to LAST");
      }
      else
      {
        ++len;
        if (len > maxLength)
          throw new LispValueNotAListException("Encountered a list whose length is greater than " +
                                               maxLength + ".  This is probably an error.  Adjust *MAX-LIST-LENGTH* if necessary.");
        ptr = cdr(ptr);
      }
    return ptr;
  }


	/**
	 * Returns a LispInteger containing the length of the list.
	 * Throws an error on a malformed list, and if the length
	 * of the list is greater than *MAX-LIST-LENGTH*.
	 *
	 */
	@Override
	public int length()
	{
		LispValue ptr = this;
		int       len = 0;
		long      maxLength = Lisp.MAX_LIST_LENGTH_VALUE;

		while (ptr != NIL)
		{
			++len;
			if (len > maxLength)
			{
				System.err.println("list is: " + this.car() + ", remainder is: " + car(ptr) + ", " + ptr.second() + ", " + ptr.third());
				throw new LispValueNotAListException("Encountered a list whose length is greater than " +
                                             maxLength + ".  This is probably an error.  Set *MAX-LIST-LENGTH* to a larger value if necessary.");
			}

			if (ptr instanceof LispCons)
				ptr = ((LispCons)ptr).cdr();
			else
				throw new LispValueNotAListException("An argument to LENGTH");
		}
		return len;
	}

	public LispValue member(LispValue elt)
	{
		if (car().eql(elt) == T)
			return this;
		else
			return cdr().member(elt);
	}

  public LispValue pop()
  {
    LispValue result = carCell;
    if (cdrCell instanceof LispList)
    {
      carCell = car(cdrCell);
      cdrCell = cdr(cdrCell);
    }
    else
      throw new LispValueNotAConsException("The cdr of the argument to POP ");

    return result;
  }

  public LispValue push(LispValue value)
  {
    cdrCell = cons(carCell, cdrCell);
    carCell = value;

    return this;
  }

  public LispValue     rassoc(LispValue index)
  {
    LispValue  ptr   = this;
    LispValue  value;
    long       len = 0;  // To prevent runaway lists.
    long       maxLength = Lisp.MAX_LIST_LENGTH_VALUE;

    while (ptr != NIL)
    {
      value = car(ptr);

      if (!(ptr instanceof LispCons))
      {
        throw new LispValueNotAListException("The second argument to RASSOC");
      }

      if (index.eql(cdr(value)) == T)
        return value;
      ptr = cdr(ptr);
      ++len;
      if (len > maxLength)
        throw new LispValueNotAListException("Encountered a list whose length is greater than " +
                                             maxLength + ".  This is probably an error.  Adjust *MAX-LIST-LENGTH* if necessary.");
    }
    return NIL;
  }

  public LispValue rest()
  {
    return cdrCell;
  }


  public LispValue remove(LispValue elt)
  {
    if (car().eql(elt) == T)
      return cdr().remove(elt);
    else
      return cons(car(), (cdr().remove(elt)));
  }

  public LispValue     rplaca(LispValue  newCar)
  { carCell = newCar; return this; };
  //todo: change argument to LispConsOrNil
  public LispValue     rplacd(LispValue  newCdr)
  { cdrCell = newCdr; return this; };

  public LispValue subst(LispValue newValue, LispValue oldValue)
  {
    if (oldValue.eql(car()) == T)
      return cons(newValue, cdr().subst(newValue, oldValue));
    else if (oldValue.eql(cdr()) == T)
      return cons(car(), newValue);
    else
      return cons(car(), cdr().subst(newValue, oldValue));
  }

	@Override
	public LispValue append(LispValue list)
	{
		LispCons head = cons(NIL, NIL);
		LispCons p = head;
		
		for (Iterator<LispValue> i = this.iterator(); i.hasNext();) {
			p.setf_cdr(cons(i.next(), NIL));
			p = (LispCons)cdr(p);
		}
		for (Iterator<LispValue> i = list.iterator(); i.hasNext();) {
			p.setf_cdr(cons(i.next(), NIL));
			p = (LispCons)cdr(p);
		}
		
		return cdr(head);
	}
  
};

