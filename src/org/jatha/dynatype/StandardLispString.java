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

import java.io.PrintStream;
import java.util.Iterator;

import org.jatha.Lisp;
import org.jatha.exception.*;

// See LispValue.java for documentation


//-------------------------------  LispString  ------------------------------

public class StandardLispString extends StandardLispAtom implements LispString
{
  // -----  Fields  -----
  private  String str;

  // -----  Constructors  -----
  public StandardLispString(String strName)
  {
    str = strName;
  }

  // -----  non-LISP methods  -----

  public String getValue() { return str; }

  public void internal_princ(PrintStream os)
  { os.print(str); }

  public void internal_prin1(PrintStream os)
  { os.print("\"" + str + "\""); }

  public void internal_print(PrintStream os)
  { os.print("\"" + str + "\""); }


  public String toString() { return "\"" + str + "\""; }

  /**
   * Strips double-quotes from a LispString value.
   */
  public String toStringSimple() { return str; }


  public boolean constantp() { return true; }
  public LispValue basic_elt(int n) { return elt(integer(n)); }
  public int     basic_length()    { return str.length(); }
  public boolean basic_stringp()   { return true; }


  // -----  LISP methods  ----------
  // contributed by Jean-Pierre Gaillardon, April 2005
  public LispValue elt(int index)
  {
    return elt(integer(index));
  }

  public LispValue elt (LispValue index)
  {
    long indexValue = assertInteger(index).getLongValue();

    if ((indexValue < 0) || (indexValue >= str.length()))
    	throw new LispIndexOutOfRangeException(String.valueOf(indexValue) + " to ELT");

    // All is okay - return the element, which is a character.
    return new StandardLispCharacter(str.charAt((int) indexValue));
  }


  public LispValue eql(LispValue other)
  {
    if (other instanceof LispString)
      if (str.equals(((LispString)other).getValue()))
        return T;

    return NIL;
  }


  public LispValue equal(LispValue other)
  {
    return eql(other);
  }


  public boolean equals(LispValue other)
  {
    return (eql(other) == T);
  }


  public LispValue     first        ()  { return elt(0); }
  public LispValue     second       ()  { return elt(1); }
  public LispValue     third        ()  { return elt(2); }
  public LispValue     fourth       ()  { return elt(3); }
  public LispValue     fifth        ()  { return elt(4); }
  public LispValue     sixth        ()  { return elt(5); }
  public LispValue     seventh      ()  { return elt(6); }
  public LispValue     eighth       ()  { return elt(7); }
  public LispValue     ninth        ()  { return elt(8); }
  public LispValue     tenth        ()  { return elt(9); }

  /**
   * Returns the last character in the string.
   */
  public LispValue     last         ()  { return elt(basic_length() - 1); }

	public int length()
	{
		return str.length();
	}

  public LispValue     stringp      ()     { return T; }

  /**
   * For Common LISP compatibility, but identical to stringUpcase.
   */
  public LispValue nstringUpcase()
  {
    return stringUpcase();
  }

  /**
   * For Common LISP compatibility, but identical to stringCapitalize.
   */
  public LispValue nstringCapitalize()
  {
    return stringCapitalize();
  }

  /**
   * For Common LISP compatibility, but identical to stringDowncase.
   */
  public LispValue nstringDowncase()
  {
    return stringDowncase();
  }

  /**
   * Returns the index of an element in a sequence.
   * Currently works for lists and strings.
   * Comparison is by EQL for lists and CHAR= for lists.
   * Returns NIL if the element is not found in the sequence.
   */
  public LispValue position(LispValue element)
  {
/*    // element must be a character
    if (element instanceof LispCharacter)
    {
      int elt = ((LispCharacter)element).getCharacterValue();
      int index = str.indexOf(elt);

      if (index < 0)
        return NIL;
      else
        return integer(index);
    }
    else*/
      throw new LispValueNotACharacterException("The argument to POSITION (" + element + ")");
  }

  /**
   * Converts a String, Symbol or Character to a string.
   */
  public LispValue string()
  {
    return this;
  }

  /**
   * Capitalizes the first character of a string and
   * converts the remaining characters to lower case.
   */
  public LispValue stringCapitalize()
  {
    if (str.length() > 0)
      return string(str.substring(0,1).toUpperCase() + str.substring(1).toLowerCase());
    else
      return string(str);
  }

  /**
   * Converts all of the characters to lower case.
   */
  public LispValue stringDowncase()
  {
    return string(str.toLowerCase());
  }

  /**
   * Not in Common LISP, but useful.  This is case-sensitive.
   */
  public LispValue stringEndsWith(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.endsWith(((LispString)arg).getValue()))
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringEndsWith (" + arg + ")");
  }

  /**
   * Returns T if the argument is an identical string to
   * the object.  Character comparison is case-sensitive.
   * This is the LISP string= function.
   */
  public LispValue stringEq(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.equals(((LispString)arg).getValue()))
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringEq (" + arg + ")");
  }

  /**
   * Returns T if the argument is an identical string to
   * the object.  Character comparison is case-insensitive.
   * STRING-EQUAL.
   */
  public LispValue stringEqual(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.equalsIgnoreCase(((LispString)arg).getValue()))
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringEqual (" + arg + ")");
  }

  /**
   * This is the LISP string-greaterp function.
   * Case-insensitive comparison for ordering.
   */
  public LispValue stringGreaterP(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareToIgnoreCase(((LispString)arg).getValue()) > 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringGreaterP (" + arg + ")");
  }

  /**
   * This is the LISP string&gt; function.
   * Case-sensitive comparison for ordering.
   */
  public LispValue stringGreaterThan(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareTo(((LispString)arg).getValue()) > 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringGreaterThan (" + arg + ")");
  }

  /**
   * This is the LISP string&gt;= function.
   * Case-sensitive comparison for ordering.
   */
  public LispValue stringGreaterThanOrEqual(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareTo(((LispString)arg).getValue()) >= 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringGreaterThanOrEqual (" + arg + ")");
  }

  /**
   * Trims the left end of the string by deleting whitespace on both ends.
   */
  public LispValue stringLeftTrim()
  {
    int maxLength = str.length();
    int i=0;
    while ((i < maxLength) && (Character.isWhitespace(str.charAt(i))))
      i++;
    return string(str.substring(i));
  }

  /**
   * Trims the left end of the string by deleting characters in the input string on both ends.
   */
  public LispValue stringLeftTrim(LispValue deleteBag)
  {
    if (! (deleteBag instanceof LispString))
      throw new LispValueNotAStringException("The argument to stringLeftTrim (" + deleteBag + ")");

    int maxLength = str.length();
    int i=0;
    while ((i < maxLength) && (((LispString)deleteBag).getValue().indexOf(str.charAt(i)) >= 0))
      i++;
    return string(str.substring(i));
  }

  /**
   * This is the LISP string-lessp function.
   * Case-insensitive comparison for ordering.
   */
  public LispValue stringLessP(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareToIgnoreCase(((LispString)arg).getValue()) < 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringLessP (" + arg + ")");
  }

  /**
   * This is the LISP string&lt; function.
   * Case-sensitive comparison for ordering.
   */
  public LispValue stringLessThan(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareTo(((LispString)arg).getValue()) < 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringLessThan (" + arg + ")");
  }

  /**
   * This is the LISP string&lt;= function.
   * Case-sensitive comparison for ordering.
   */
  public LispValue stringLessThanOrEqual(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareTo(((LispString)arg).getValue()) <= 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringLessThanOrEqual (" + arg + ")");
  }

  /**
   * Returns T if the argument is not STRING= the given string.
   */
  public LispValue stringNeq(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.equals(((LispString)arg).getValue()))
        return NIL;
      else
        return T;
    else
      throw new LispValueNotAStringException("The argument to stringNeq (" + arg + ")");
  }

  /**
   * This is the LISP string-not-greaterp function.
   * Case-insensitive comparison for ordering.
   */
  public LispValue stringNotGreaterP(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareToIgnoreCase(((LispString)arg).getValue()) <= 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringNotGreaterP (" + arg + ")");
  }

  /**
   * This is the LISP string-not-lessp function.
   * Case-insensitive comparison for ordering.
   */
  public LispValue stringNotLessP(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.compareToIgnoreCase(((LispString)arg).getValue()) >= 0)
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringNotLessP (" + arg + ")");
  }

  /**
   * Trims the right end of the string by deleting whitespace on both ends.
   */
  public LispValue stringRightTrim()
  {
    int i = str.length() - 1;
    while ((i >= 0) && (Character.isWhitespace(str.charAt(i))))
      i--;
    return string(str.substring(0, i+1));
  }

  /**
   * Trims the right end of the string by deleting characters in the input string on both ends.
   */
  public LispValue stringRightTrim(LispValue deleteBag)
  {
    if (! (deleteBag instanceof LispString))
      throw new LispValueNotAStringException("The argument to stringRightTrim (" + deleteBag + ")");

    int i = str.length() - 1;
    while ((i >= 0) && (((LispString)deleteBag).getValue().indexOf(str.charAt(i)) >= 0))
      i--;
    return string(str.substring(0, i+1));
  }

  /**
   * Not in Common LISP, but useful.  This is case-sensitive.
   */
  public LispValue stringStartsWith(LispValue arg)
  {
    if (arg instanceof LispString)
      if (str.startsWith(((LispString)arg).getValue()))
        return T;
      else
        return NIL;
    else
      throw new LispValueNotAStringException("The argument to stringStartsWith (" + arg + ")");
  }

  /**
   * Trims the string by deleting whitespace on both ends.
   */
  public LispValue stringTrim()
  {
    return string(str.trim());
  }

  /**
   * Trims the string by deleting characters in the input string on both ends.
   */
  public LispValue stringTrim(LispValue deleteBag)
  {
    LispValue result = stringRightTrim(deleteBag);
    return result.stringLeftTrim(deleteBag);
  }

  /**
   * Converts all the characters to upper case.
   */
  public LispValue stringUpcase()
  {
    return string(str.toUpperCase());
  }

  /**
   * Returns the substring starting at position START
   * to the end.  Returns an empty string if START is past
   * the last position in the string.  START must be positive.
   */
  public LispValue substring(LispValue start)
  {
    return substring(start, integer(str.length()));
  }


  /**
   * Returns the substring starting at position START
   * to the end.  Returns an empty string if START is past
   * the last position in the string.  START must be positive.
   */
  public LispValue substring(LispValue start, LispValue end)
  {
    if (start instanceof LispInteger)
    {
      if (end instanceof LispInteger)
      {
        long i_begin  = ((LispInteger)start).getLongValue();
        long i_end    = ((LispInteger)end).getLongValue();
        long length   = str.length();

        if (i_begin < 0)
          throw new LispIndexOutOfRangeException("The start index of substring (" + i_begin + ")");
        else if (i_end < 0)
          throw new LispIndexOutOfRangeException("The end index of substring (" + i_end + ")");

        else if ((i_begin >= length) || (i_begin >= i_end))
          return string("");
        else
          return string(str.substring((int)i_begin, (int)i_end));
      }
      else
        throw new LispValueNotAnIntegerException("The operand of substring (" + start + ")");
    }
    else
      throw new LispValueNotAnIntegerException("The operand of substring (" + end + ")");
  }

};