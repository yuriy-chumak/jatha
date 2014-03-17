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

import org.jatha.Jatha;
import org.jatha.exception.*;

import java.util.Iterator;


// @date    Thu Mar 27 13:35:07 1997
/**
 * An abstract class for the CONS and NIL data types.
 *
 * @see LispValue
 * @see LispInteger
 * @see LispReal
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 */
public abstract class StandardLispConsOrNil extends StandardLispValue  implements LispConsOrNil
{
  public StandardLispConsOrNil()
  {
    super();
  }


  public StandardLispConsOrNil(Jatha lisp)
  {
    super(lisp);
  }


  // ------ LISP methods  ----------

  public LispValue butlast()
  {
    if (cdr() instanceof LispCons)
      return (f_lisp.makeCons(car(), cdr().butlast()));
    return f_lisp.NIL;
  }

  public LispValue elt (LispValue index)
  {
    long indexValue;

    if (!index.basic_integerp())
      throw new LispValueNotAnIntegerException("to ELT");
    else
    {
      indexValue = ((LispInteger)index).getLongValue();
      if ((indexValue < 0) || (indexValue > (((LispInteger)this.length()).getLongValue() - 1)))
	throw new LispIndexOutOfRangeException(String.valueOf(indexValue) + " to ELT");

      // All is okay
      LispValue element = this;

      for (int i = 0; i < indexValue; ++i)  element = f_lisp.cdr(element);

      return f_lisp.car(element);
    }
  }


  public LispValue length ()
  {
    long       count = 0;
    LispValue  ptr   = this;

    while (ptr != f_lisp.NIL) { ++count; ptr = f_lisp.cdr(ptr); }

    return new StandardLispInteger(f_lisp, count);
  }

  /**
   * If this object is NIL, returns the argument.
   * Otherwise, destructively appends the argument to this one.
   */
  public LispValue nconc(LispValue arg)
  {
    try {
      if (this == arg)
        throw new Exception("nconc: attaching me to myself: " + arg);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (this == f_lisp.NIL)
      return arg;

    else if (arg instanceof LispCons)
      this.last().rplacd(arg);

    return this;
  }

  public LispValue nreverse ()
  {
    LispValue head    = this;
    LispValue next    = cdr();
    LispValue result  = f_lisp.NIL;

    // p stays one ahead of the main list pointer.
    while (head != f_lisp.NIL)
    {
      next = f_lisp.cdr(head);     // Save pointer to next element in list.
      head.rplacd(result);   // Alter cdr of head.
      result = head;         // Reset pointer to top of result
      head = next;           // Start over with the next element of the list.
    }

    return result;
  }

  public LispValue pop ()
  {
    // If we get to this method, this must be NIL.
    return this;
  }

  /**
   * Returns the index of an element in a sequence.
   * Currently works for lists and strings.
   * Comparison is by EQL for lists and CHAR= for lists.
   * Returns NIL if the element is not found in the sequence.
   */
  public LispValue position(LispValue element)
  {
    LispValue ptr = this;
    int index = 0;
    while ((ptr != f_lisp.NIL) && (f_lisp.car(ptr).eql(element) instanceof LispNil))
    {
      ptr = f_lisp.cdr(ptr);
      index++;
    }

    if (ptr == f_lisp.NIL)
      return f_lisp.NIL;
    else
      return f_lisp.makeInteger(index);
  }

  public LispValue reverse ()
  {
    LispValue result = f_lisp.NIL;

    for (LispValue p=this; p != f_lisp.NIL; p = f_lisp.cdr(p) )
      result = new StandardLispCons(f_lisp, f_lisp.car(p), result);

    return result;
  }


  /* Implementation of Iterator interface */
  /**
   * Returns an iterator over the clauses of the path.
   * Each element type is a Clause.
   */
  public Iterator iterator()
  {
    return new LispConsIterator(this);
  }


  public abstract LispValue car();
  public abstract LispValue cdr();
}
