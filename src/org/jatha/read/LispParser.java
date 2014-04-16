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

package org.jatha.read;

import java.io.*;
import java.math.BigInteger;
import java.util.regex.Pattern;

import org.jatha.dynatype.*;
import org.jatha.Lisp;
import org.jatha.LispProcessor;
import org.jatha.exception.*;
import org.jatha.util.SymbolTools;

import static org.jatha.dynatype.LispValue.*;


/**
 * A parser that reads LISP-syntax text from a text
 * stream or string.  It recognizes all standard
 * LISP datatypes, although not structured ones.
 * This function is designed to fulfill the function
 * of the reader in a LISP <tt>read-eval-print</tt> loop.
 *
 * Once the LISP parser is instantiated, the
 * <tt>parse()</tt> function can be used to read from
 * a string or stream.
 *
 * It is best not to instantiate the LispParser yourself.
 * Instead, do the following:
 * <pre>
 *   1.  LispParser parser = Jatha.getParser();
 *   2.  parser.setInputString(myString);
 *   3.  parser.setCaseSensitivity(LispParser.PRESERVE);
 *   4.  LispValue result = parser.parse();
 * </pre>
 * Normal usage is to parse a string.  If you want to use a Reader,
 * do: <code>new PushbackReader(myReader)</code>.
 *
 * example:
 * http://www.lispworks.com/documentation/lw50/CLHS/Body/02_cd.htm
 * 
 * todo: implements sha-bang (#!) in scenario
 * 
 * @see org.jatha.dynatype.LispValue
 * @author Micheal S. Hewett
 * @version 1.0
 */
public class LispParser extends LispProcessor
{
//	static final LispSymbol COMMA_FN        = LispValue.COMMA_FN;
//	static final LispSymbol COMMA_ATSIGN_FN = LispValue.COMMA_ATSIGN_FN;
//	static final LispSymbol COMMA_DOT_FN    = LispValue.COMMA_DOT_FN;
	
	public static final LispSymbol COMMA_FN        = new StandardLispSymbol("COMMA");
	public static final LispSymbol COMMA_ATSIGN_FN = new StandardLispSymbol("COMMA_ATSIGN");
	public static final LispSymbol COMMA_DOT_FN    = new StandardLispSymbol("COMMA_DOT");
	
	public static final int UPCASE         = 1;
	public static final int DOWNCASE       = 2;
	public static final int PRESERVE       = 3;

	static final char AT_SIGN              = '@';
	static final char BACK_QUOTE           = '`';
	static final char SINGLE_QUOTE         = '\'';
	static final char BACKSLASH            = '\\';
	static final char COLON                = ':';
	static final char COMMA                = ',';
	static final char DECIMAL              = '.';
	static final char DOUBLE_QUOTE         = '"';
	static final char EQUAL_SIGN           = '=';
	static final char LEFT_ANGLE_BRACKET   = '<';
	static final char LEFT_PAREN           = '(';
	static final char HYPHEN               = '-';
	static final char OR_BAR               = '|';
	static final char POUND                = '#';
	static final char PERIOD               = '.';
	static final char RIGHT_PAREN          = ')';
	static final char SEMICOLON            = ';';
	static final char RIGHT_ANGLE_BRACKET  = '>';
	static final char UNDERSCORE           = '_';

	// Parser states
	static final int READING_NOTHING           = 0;
	static final int READING_SYMBOL            = 1;
	static final int READING_MIXED_CASE_SYMBOL = 2;
	static final int READING_CHARACTER         = 3;
	static final int READING_STRING            = 4;
	static final int READING_BACKQUOTED_LIST   = 5;

	private      int BackQuoteLevel            = 0;
	private      PushbackReader  inputReader;

	private int f_caseSensitivity = UPCASE;  // default LISP behavior.

	private static LispParser f_myParser = null;


	// required by tokenToLispValue() to use "intern" and "keyword" functions
	private Lisp f_lisp = null;

	public LispParser(Lisp lisp, Reader inStream)
	{
		this(lisp, inStream, UPCASE);
	}

	public LispParser(Lisp lisp, String inString)
	{
		this(lisp, new StringReader(inString), UPCASE);
	}


	/**
	 * Allows you to create a parser that handles
	 * input case conversion as you like.
	 * Default is UPCASE.  Other values are DOWNCASE and PRESERVE.
	 * @param inStream
	 */
	public LispParser(Lisp lisp, Reader inStream, int caseSensitivity)
	{
		f_lisp = lisp;

		if (inStream instanceof PushbackReader)
			inputReader = (PushbackReader)inStream;
		else
			inputReader   = new PushbackReader(inStream);
		setCaseSensitivity(caseSensitivity);
	}

	/**
	 * Allows you to create a parser that handles
	 * input case conversion as you like.
	 * Default is UPCASE.  Other values are DOWNCASE and PRESERVE.
	 */
	public LispParser(Lisp lisp, String inString, int caseSensitivity)
	{
		this(lisp, new StringReader(inString));
		setCaseSensitivity(caseSensitivity);
	}

  /**
   * Retrieves the current case-sensitivity of the parser.
   * It can be eiher LispParser.UPCASE, LispParser.DOWNCASE
   * or LispParser.PRESERVE
   * @return UPCASE, DOWNCASE or PRESERVE
   */
  public int getCaseSensitivity()
  {
    return f_caseSensitivity;
  }

  /**
   * Sets the current case-sensitivity of the parser.
   * It can be eiher LispParser.UPCASE, LispParser.DOWNCASE
   * or LispParser.PRESERVE
   */
  public void setCaseSensitivity(int caseSensitivity)
  {
    f_caseSensitivity = caseSensitivity;
  }


  /**
   * Gets the current reader to be parsed.
   */
  public PushbackReader getInputReader()
  {
    return inputReader;
  }


  /**
   * Sets the input reader for the Parser.
   */
  public void setInputReader(PushbackReader inputReader)
  {
    this.inputReader = inputReader;
  }


  /**
   * Sets the input string for the parser.  This is the
   * String to parse.
   */
  public void setInputString(String s)
  {
    this.inputReader = new PushbackReader(new StringReader(s));
  }


	/**
	 * Parse() assumes that there is only one expression in the input
	 * string or file.  If you need to read multiple items from a
	 * string or file, use the read() function.
	 * Parse just calls read right now.
	 *
	 * @see #read
	 */
	public LispValue parse()
			throws EOFException
	{
		return read();
	}

	/**
	 * Reads one s-expression from the input stream (a string or file).
	 * Throws an EOFxception when EOF is reached.
	 * Call this method repeatedly to do read-eval-print on a file.
	 */
	public LispValue read()
			throws EOFException
	{
		StringBuffer token = new StringBuffer(80);    // Should cover most tokens.
		char ch;
		int  intCh = 0;
		int  parseState = READING_NOTHING;

		while (true)
		{
			try { intCh = inputReader.read(); }
			catch (IOException ioex) { break; }

			if (intCh < 0) {
				if (parseState != READING_SYMBOL)  // end of symbol at end of input
					throw new EOFException("Premature end of LISP input at: " + token.toString());
			
				ch = ' ';
			}
			else
				ch = (char) intCh;

			// Encounter a comment?: flush the remaining characters on the line.
			if (isSemicolon(ch)
					&& (parseState != READING_STRING)
					&& (parseState != READING_CHARACTER))
			{
				do {
					try { intCh = inputReader.read(); }
					catch (IOException ioex) { break; }
					if (intCh < 0)
						throw new EOFException("Premature end of LISP input at: " + token.toString());
					
					ch = (char) intCh;
				}
				while (ch != '\n' && ch != '\r');
				continue;
			}

			switch (parseState) {
			case READING_SYMBOL:
				if (isTerminator(ch))       // Terminate reading token.
				{
					unread(ch);
					parseState = READING_NOTHING;
					return tokenToLispValue(
							f_caseSensitivity == UPCASE   ? token.toString().toUpperCase() :
							f_caseSensitivity == DOWNCASE ? token.toString().toLowerCase() :
								token.toString()
							);
				}
				
				token.append(ch);
				break;

			case READING_MIXED_CASE_SYMBOL:
				if (isOrBar(ch))       // Terminate reading token.
				{
					String s = token.toString();

					token.append(ch);
					parseState = READING_NOTHING;
					// Strip off the beginning and ending Or Bars.
					return tokenToLispValue(s.substring(1, s.length()));
				}
				
				token.append(ch);
				break;

			case READING_STRING:
				if (ch == BACKSLASH)  // Next char is always in the string
				{
					try { intCh = inputReader.read(); }
					catch (IOException ioex) { break; }
					if (intCh < 0)
						throw new EOFException("Premature end of LISP input at: " + token.toString());
					
					ch = (char)intCh;
					token.append(ch);
					break;
				}

				if (ch == DOUBLE_QUOTE)
				{
					token.append(ch);
					parseState = READING_NOTHING;
					return tokenToLispValue(token.toString());
				}
				
				token.append(ch);
				break;
			case READING_NOTHING:
				if (!isSpace(ch))         /* Start reading a token */
				{
					if (isLparen(ch))
						return read_list_token(inputReader);
					if (isRparen(ch))
					{
						System.err.println("WARNING: Too many right parentheses.  NIL assumed.");
						return NIL;
					}
					if (isQuote(ch))
						return read_quoted_token(inputReader, UPCASE);
					if (isColon(ch))
						return read_quoted_token(inputReader, PRESERVE);
					if (isDoubleQuote(ch)) {
						token.append(ch);
						parseState = READING_STRING;
						break;
					}
					if (isPound(ch))
						return(applyReaderMacro(inputReader));
					if (isBackQuote(ch))
						return(read_backquoted_token(inputReader));
					if (isComma(ch))
						return(read_comma_token(inputReader));
					
					if (isOrBar(ch)) {
						token.append(ch);
						parseState = READING_MIXED_CASE_SYMBOL;
						break;
					}
					
					parseState = READING_SYMBOL;
					unread(ch);
				}  /* if (!isSpace(ch)) */
				break;
			}
		} /* main WHILE loop */

		/* WE ONLY EXECUTE THIS CODE IF WE HIT end of input string or file. */
		if (token.length() > 0)
			return(tokenToLispValue(token.toString()));
		else
			return(NIL);
	}
	
	private final void unread(char ch)
	{
		try { inputReader.unread(ch); }
		catch (IOException ioex) {
			System.err.println("\n *** I/O error while unreading character '" + ch + "'.");
		}
	}


  /**
   * Reads one list expression from the input stream and returns it.
   * The input pointer should be on the character following the left parenthesis.
   */
  public LispValue read_list_token(PushbackReader stream) throws EOFException
  {
    boolean firstTime  = true;
    boolean haveDot    = false;
    char    ch;
    int     intCh = 0;
    LispValue      newToken;
    LispValue      newList, newCell;

    newList =  NIL;
    newCell =  NIL;

    LispValue newListLast = NIL;

    while (true)
    {
      try { intCh = inputReader.read(); }
      catch (IOException e)
      { break; }
      if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
      else
        ch = (char) intCh;

      if (!isSpace(ch))
      {
        if (isRparen(ch))
          return(newList);

        if (isPeriod(ch))
        {
          if (haveDot)
          {
            System.err.println("WARNING: Illegal dotted syntax.  NIL assumed.");
            return NIL;
          }
          haveDot = true;
          continue;             // Skip to end of while loop.
        }

        // Encounter a comment?: flush the remaining characters on the line.
        if (isSemicolon(ch))
        {
          do
          {
            try { intCh = inputReader.read(); }
            catch (IOException e)
            { break; }
            if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
            else
              ch = (char) intCh;

            // Apparently read() doesn't do translation.
            if (ch == '\r')  ch = '\n';
          }
          while (ch != '\n');
          continue;
        }

        // otherwise process a normal token.

        unread(ch);

        // System.err.print("\nRLT calling parse()");
        newToken = read();
        // System.err.print("...got back: " + newToken.toString());

        if (firstTime)
        {
          newList   = cons(NIL, NIL);
          newList.rplaca(newToken);
          newListLast = newList;
          firstTime = false;
        }
        else
        {
          if (haveDot)
          {
            newListLast.rplacd(newToken);
            newListLast = newToken;
          }
          else
          {
            newCell  = cons(newToken, NIL);  /* (NIL . NIL) */
            
            newListLast.rplacd(newCell);
            newListLast = newCell;
          }
        }
      }  // if (!isSpace())
    }    // while ()...

    return NIL;     // Shouldn't get here.
  }

	/**
	 * This routine is called by parse when it encounters
	 * a quote mark.  It calls parse recursively.
	 */
	LispValue read_quoted_token(PushbackReader stream, int newCS) throws EOFException
	{
		int
		wasCS = f_caseSensitivity;
		f_caseSensitivity = newCS;
		LispValue value = read();
		f_caseSensitivity = wasCS;
    
		return cons(QUOTE, cons(value, NIL));
	}


	/**
	 * This routine is called by parse when it encounters
	 * a backquote mark.  It calls parse recursively.
	 */
	public LispValue read_backquoted_token(PushbackReader stream) throws EOFException
	{
		LispValue newCell          = NIL;

		/* Construct the quoted list (SYS::BACKQUOTE . (NIL . NIL)) then
		 * read a token and replace the first NIL by the token read.
		 */
		++BackQuoteLevel;
		newCell = read();
		--BackQuoteLevel;

		return cons(BACKQUOTE, cons(newCell, NIL));
	}


  /**
   * This routine is called by parse when it encounters
   * a comma, which is only legal inside a backquote.
   */
  LispValue read_comma_token(PushbackReader stream) throws EOFException
  {
    LispValue newCell          = NIL;
    LispValue newQuotedList    = NIL;
    LispValue identifier       = NIL;
    int  intCh;
    char ch;

    if (BackQuoteLevel <= 0)
    {
      System.err.println(";; *** ERROR: Comma not inside backquote.");
      return NIL;
    }

    try { intCh = inputReader.read(); }
    catch (IOException e) { return NIL; }

    if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
    else
      ch = (char) intCh;

    // Apparently read() doesn't do translation.
    if (ch == '\r')  ch = '\n';

    if (isAtSign(ch))
      identifier = COMMA_ATSIGN_FN;
    else if (isPeriod(ch))
      identifier = COMMA_DOT_FN;
    else
    {
      identifier = COMMA_FN;
      unread(ch);
    }

    newQuotedList = cons(identifier,
                         cons(NIL, NIL));

    newCell = read();

    cdr(newQuotedList).rplaca(newCell);
    return(newQuotedList);
  }


  /**
   * This routine is called by parse when it encounters
   * a pound (#) mark.
   */
  public LispValue applyReaderMacro(PushbackReader stream) throws EOFException
  {
    char   ch = '0';
    int    intCh = 0;

    try { intCh = inputReader.read(); }
    catch (IOException e)
    { System.err.println("\n *** I/O error while reading '#' token."); }
    if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
    else
      ch = (char) intCh;

    // #:foo is an uninterned symbol.
/*    if (isColon(ch))
    {
      StringBuffer token = new StringBuffer(80);
      token.append('#');

      while (!isTerminator(ch))       // Terminate reading token.
      {
        token.append(ch);
        try { intCh = inputReader.read(); }
        catch (IOException e)
        { System.err.println("\n *** I/O error while reading '#:' token."); }
        if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
        else
          ch = (char) intCh;
      }

      unread(ch);

      if (f_caseSensitivity == UPCASE)
        return(tokenToLispValue(token.toString().toUpperCase()));
      else if (f_caseSensitivity == DOWNCASE)
        return(tokenToLispValue(token.toString().toLowerCase()));
      else // if (f_caseSensitivity == PRESERVE)
        return(tokenToLispValue(token.toString()));
    }

    // #'foo means (function foo)
    else */
    if (isQuote(ch))
    {
      LispValue result = list(tokenToLispValue("FUNCTION"), read());
      return result;

      //LispValue result = f_lisp.makeList(tokenToLispValue("FUNCTION"));
//
//      try {
//        intCh = inputReader.read();
//      } catch (IOException e) {
//        System.err.println("\n *** I/O error while reading a #' expression." + e);
//        return tokenToLispValue(token.toString());
//      }
//      if (intCh < 0)
//       throw new EOFException("Premature end of LISP input.");
//      else
//        ch = (char) intCh;

      //LispValue fnToken = read();

//      if (intCh == LEFT_PAREN)   // #'(lambda...
//      {
//        fnToken = read_list_token(inputReader);
//      }
//
//      else
//      {
//        while (! isTerminator(ch))
//        {
//          token.append(ch);
//          try {
//            intCh = inputReader.read();
//          } catch (IOException e) {
//            System.err.println("\n *** I/O error while reading a #'symbol expression." + e);
//          }
//          if (intCh < 0)
//            throw new EOFException("Premature end of LISP input.");
//          else
//            ch = (char) intCh;
//        }
//
//        try {
//          inputReader.unread(ch);
//        } catch (IOException e) {
//          System.err.println("\n *** I/O error while reading a #' expression." + e);
//          return tokenToLispValue(token.toString());
//        }
//
//        if (f_caseSensitivity == UPCASE)
//          fnToken = tokenToLispValue(token.toString().toUpperCase());
//        else if (f_caseSensitivity == DOWNCASE)
//          fnToken = tokenToLispValue(token.toString().toLowerCase());
//        else // if (f_caseSensitivity == PRESERVE)
//          fnToken = tokenToLispValue(token.toString());
//      }

      //return result.append(f_lisp.makeList(fnToken));

    }

    // #\ reads a character macro
    else if (isBackSlash(ch))
    {
      StringBuffer token = new StringBuffer(80);
      try { intCh = inputReader.read(); }
      catch (IOException e)
      { System.err.println("\n *** I/O error while reading character token."); }
      if (intCh < 0)  { throw new EOFException("Premature end of LISP input."); }
      else
        ch = (char) intCh;

      while (! isTerminator(ch))
      {
        token.append(ch);
        try {
          intCh = inputReader.read();
        } catch (IOException e) {
          System.err.println("\n *** I/O error while reading a #' expression." + e);
        }
        if (intCh < 0)
          throw new EOFException("Premature end of LISP input.");
        else
          ch = (char) intCh;
      }

      try {
        inputReader.unread(ch);
      } catch (IOException e) {
        System.err.println("\n *** I/O error while reading a #' expression." + e);
        return tokenToLispValue(token.toString());
      }

      if(token.length()>1) {
          final String tok = token.toString();
          if(tok.equalsIgnoreCase("SPACE")) {
              ch = ' ';
          } else if(tok.equalsIgnoreCase("NEWLINE")) {
              ch = '\n';
          } else {
              ch = 0;
          }
      } else {
          ch = token.charAt(0);
      }

      return new StandardLispCharacter(ch);
    }

    // #< usually starts a structure
    else if (isLeftAngleBracket(ch))
    {
      System.err.println("\n *** parser can't read structures yet.");
      while (! isRightAngleBracket(ch))
        try {
          intCh = inputReader.read();
          ch = (char)intCh;
        } catch (IOException e) {
          System.err.println("\n *** I/O error while reading a structure.");
        }

      if (intCh < 0)
      {
        throw new EOFException("Premature end of LISP input.");
      }

      else
        ch = (char) intCh;

      return NIL;
    }

    // -- #| ... |# is a block comment
    else if (isOrBar(ch))
    {
      try {
        boolean done = false;
        boolean terminating = false;
        while (! done)
        {
          intCh = inputReader.read();
          ch = (char)intCh;
          if (isOrBar(ch))
            terminating = true;
          else if (terminating && isPound(ch))
            done = true;
          else
            terminating = false;
        }
      } catch (IOException e) {
        System.err.println("\n *** I/O error while reading a block comment.  Not terminated?");
      }
      return NIL;
    }

    else
    {
      System.err.println("\n *** unknown '#' construct.");
      return NIL;
    }
  }


  /**
   * This library can't read backquotes yet.
   */
  public LispValue read_backquoted_list_token(PushbackReader stream)
  {
    System.err.println("\n *** Parser can't read backquoted lists yet.");
    return NIL;
  }

	/**
	 * Converts a string to a LISP value such as
	 * NIL, T, an integer, a real number, a string or a symbol.
	 */
	public LispValue tokenToLispValue(String token)
	{
		LispValue newCell = null;

		if (T_token_p(token))
			newCell = T;
		else if (NIL_token_p(token))
			newCell = NIL;
		else if (INTEGER_token_p(token))
		{
			// It may be an Fixnum or a Bignum.
			// Let Java tell us by generating a NumberFormatException
			// when the number is too big (or too negatively big).

			// SK: java cannot parse integer with '+' in front of it?  25 Jul 2009
			if (token.charAt(0) == '+' )
				token = token.substring(1);

			try {
				newCell = StandardLispValue.integer(new Long(token)); 
			} catch (NumberFormatException e) {
				newCell = StandardLispValue.bignum(new BigInteger(token)); 
			}
		}
		else if (REAL_token_p(token))
			newCell = StandardLispValue.real(new Double(token));
		else if (STRING_token_p(token))
		{ /* remove the first and last double quotes. */
			try
			{
				newCell = new StandardLispString(token.substring(1, token.length() - 1));
			}
			catch (StringIndexOutOfBoundsException e)
			{
				System.err.println("Hey, got a bad string index in 'tokenToLispValue'!");
			}
		}
		else if (SYMBOL_token_p(token))
		{
			// Added packages, 10 May 1997 (mh)
			// Removed packages, 19 Mar 2014 (yc)
			/*if (token.indexOf(':') >= 0)
			{
				String packageStr = token.substring(0, token.indexOf(':'));
				token = token.substring(packageStr.length() + 1, token.length());

				// assertion!
				if (token.startsWith(":")) {
					System.err.println("Warning: ignored extra ':' in '" + packageStr + token + "'.");
					token = token.substring(token.lastIndexOf(':')+1, token.length());
				}
				
				if (packageStr.equals("#"))   // Uninterned symbol
					newCell = new StandardLispSymbol(token);	// no package
				else {
					if (!"".equals(packageStr))
						throw(new LispUndefinedPackageException(packageStr));
					
					// was: keyword
					newCell = f_lisp.intern(token.toUpperCase());
				}
			}
			else*/
				newCell = f_lisp.intern(token);
		}
		else {
			System.err.println("ERROR: Unrecognized input: \"" + token + "\"");
			newCell = NIL;
		}

		if (newCell == null)
		{
			System.err.println("MEMORY_ERROR in  \"tokenToLispValue\" " + "for token \""
					+ token + "\", returning NIL.");
			newCell = NIL;
		}

		return newCell;
	}

	// ----  Utility functions  ----------------------------------

	static final boolean isLparen(char x)             { return (x == LEFT_PAREN);   }
	static final boolean isRparen(char x)             { return (x == RIGHT_PAREN);  }
	static final boolean isAtSign(char x)             { return (x == AT_SIGN);      }
	static final boolean isBackQuote(char x)          { return (x == BACK_QUOTE);   }
	static final boolean isBackSlash(char x)          { return (x == BACKSLASH);    }
	static final boolean isColon(char x)              { return (x == COLON);        }
	static final boolean isComma(char x)              { return (x == COMMA);        }
	static final boolean isDoubleQuote(char x)        { return (x == DOUBLE_QUOTE); }
	static final boolean isOrBar(char x)              { return (x == OR_BAR);       }
	static final boolean isPound(char x)              { return (x == POUND);        }
	static final boolean isPeriod(char x)             { return (x == PERIOD);       }
	static final boolean isQuote(char x)              { return (x == SINGLE_QUOTE); }
	static final boolean isSemicolon(char x)          { return (x == SEMICOLON);    }
	static final boolean isLeftAngleBracket(char x)   { return (x == LEFT_ANGLE_BRACKET); }
	static final boolean isRightAngleBracket(char x)  { return (x == RIGHT_ANGLE_BRACKET); }

	static boolean isSpace(char x)
	{
		return (x == ' ')       // real space
			|| (x == '\n')      // newline
			|| (x == '\r')      // carriage return
			|| (x == '\t')      // tab
			|| (x == '\f')      // form feed
			|| (x == '\b');     // backspace
	}

   /*
    * Because isTerminator is called in a very tight loop (for every input
    * character) and we want to deal with potentailly massive literals,
    * we use a small lookup table to make it as fast as possible.
    * todo: change this
    */
   private static final boolean[] terminatorLookupTable = new boolean[256];
   static
   {
       for (int i = 0; i < 256; i++)
       {
           terminatorLookupTable[i] = isSpace((char)i) // white space
             || isLparen((char)i) || isRparen((char)i)
             || isComma((char)i)  || isSemicolon((char)i)
             || isDoubleQuote((char)i)
             || isQuote((char)i);
       }
   }
   static boolean isTerminator(char x)
   {
	   if ((int)x < 256)
		   return terminatorLookupTable[(byte)x];
	   return false;
   }



  private static final Pattern REAL_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
  boolean REAL_token_p(String str)
  {
      return REAL_PATTERN.matcher(str).matches();
  }
  
  
  /**
   * Does NOT recognize an isolated '+' or '-' as a real number.
   */
  boolean REAL_token_p_old(String str)
  {
    String DECIMALchars  = ".";
    String INTchars      = "0123456789";

    int   decimalPos;
    int   length = str.length();
    int   index  = 0;

    if ((index < length) && ((str.charAt(index) == '-') || (str.charAt(index) == '+')))
      index++;

    if (index == length)   // Don't accept a single '-' or '+'
      return false;

    decimalPos = str.indexOf(DECIMALchars);     /* Check for decimal.  If none, not a real number. */
    if (decimalPos < 0)
      return(false);

    if (SymbolTools.firstCharNotInSet(index, str, INTchars) != decimalPos)
      return(false);

    if (decimalPos == str.length() - 1)
      return(true);         /* Decimal point followed by no digits is legal in LISP. */

    /* Check decimal digits. */
    index = decimalPos + 1;
    
    return(SymbolTools.firstCharNotInSet(index, str, INTchars) == length);
  }


  boolean INTEGER_token_p(String str)
  /*
   * Does NOT recognize an isolated '+' or '-' as an integer.
   */
  {
    String INTchars = "0123456789";

    int   length = str.length();
    int   index  = 0;

    try {
      if ((index < length) && ((str.charAt(index) == '-') || (str.charAt(index) == '+')))
        index++;
    }
    catch (StringIndexOutOfBoundsException e) {
      System.err.println("Hey, got a bad string index in 'INTEGER_token_p'! on string '" + str + "'"); 
    }

    if (index == length)   // Don't accept a single '-' or '+'
      return false;

    return(SymbolTools.firstCharNotInSet(index, str, INTchars) == length);
  }


  boolean STRING_token_p(String str)
  {
    int       length = str.length();
    boolean   value;

    value = false;

    try {
      value = ((length >= 2)
	       && (str.charAt(0)        == DOUBLE_QUOTE)
	       && (str.charAt(length-1) == DOUBLE_QUOTE));
    }
    catch (StringIndexOutOfBoundsException e) {
      System.err.println("Hey, got a bad string index in 'NIL_token_p'!"); }

    return value;
  }


	boolean T_token_p(String str) { return ("T".equalsIgnoreCase(str)); }
	boolean NIL_token_p(String str) { return("NIL".equalsIgnoreCase(str)); }
	boolean SYMBOL_token_p(String str) { return(str.length() >= 1); }



  // ----  Test functions  ----------------------------------

  public void    test_parser(String s)
  {
    System.out.print("The string \"" + s + "\" ");

    if (T_token_p(s))
      System.out.println("is T.");
    else if (NIL_token_p(s))
      System.out.println("is NIL.");
    else if (INTEGER_token_p(s))
      System.out.println("is an integer.");
    else if (REAL_token_p(s))
      System.out.println("is a double.");
    else if (STRING_token_p(s))
      System.out.println("is a string.");
    else if (SYMBOL_token_p(s))
      System.out.println("is a symbol.");
    else
      System.out.println("is not recognized.");
  }
  /**
   * Returns true if the input expression has balanced parentheses
   * @param input a String
   * @return true if it has balanced parentheses
   */
  public static boolean hasBalancedParentheses(Lisp lisp, LispValue input)
  {
    return hasBalancedParentheses(lisp, input.toString());
  }

  /**
   * Returns true if the input expression has balanced parentheses
   * @param input a String
   * @return true if it has balanced parentheses
   */
  public static boolean hasBalancedParentheses(Lisp lisp, String input)
  {
    LispValue result = NIL;

    if (f_myParser == null)
      f_myParser = new LispParser(lisp, input);
    else
      f_myParser.setInputString(input);

    try {
      while (true)
        result = f_myParser.read();

    } catch (EOFException eofe) {
      if (eofe.getMessage().toLowerCase().startsWith("premature"))
      {
        System.err.println("Unbalanced parentheses in input.  Last form read was " + result);
        return false;
      }
      else
        return true;
    }
  }
}