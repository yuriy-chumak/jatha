/**
 * Jatha - a Common LISP-compatible LISP library in Java.
 * Copyright (C) 1997-2008 Micheal Scott Hewett
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

package org.jatha;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jatha.exception.*;
import org.jatha.compile.LispCompiler;
import org.jatha.dynatype.*;
import org.jatha.machine.SECDMachine;
import org.jatha.read.LispParser;
import org.jatha.util.SymbolTable;


// * @date    Thu Feb  6 09:24:18 1997
/**
 * Jatha is an Applet supporting a subset of Common LISP,
 * with extensions to support some features of Java
 * such as networking and graphical interfaces.
 * <p>
 * Usage: java org.jatha.Jatha [-nodisplay] [-help]
 * </p>
 * @author  Micheal S. Hewett    hewett@cs.stanford.edu
 *
 */
public class Lisp
{
	private static boolean DEBUG = false;

	private String VERSION_NAME     = "Jatha";
	private int    VERSION_MAJOR    = 3;
	private int    VERSION_MINOR    = 0;
	private int    VERSION_MICRO    = 1;
	private String VERSION_TYPE     = "";
	private String VERSION_DATE     = "1 Feb 2014";
	private String VERSION_URL      = "https://github.com/yuriy-chumak/jatha";
  
	private boolean useConsole = true;


	/**
	 * PARSER is a pointer to the main parser
	 * used by Jatha.  Others may be instantiated to
	 * deal with String or Stream input.
	 *
	 * @see org.jatha.read.LispParser
	 *
	 */
	public LispParser    PARSER; // todo: change to READ
	
	/**
	 * COMPILER is a pointer to a LispCompiler.
	 *
	 * @see org.jatha.compile.LispCompiler
	 */
	public LispCompiler  COMPILER; // todo: change to EVAL
	
  // * @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // * @date    Thu Feb  6 09:26:00 1997
  /**
   * PACKAGE is a pointer to the current package (*package*).
   * Its SYMTAB is always the curent SYMTAB of Jatha.
   *
   * @see org.jatha.dynatype.LispPackage
   */
//public LispPackage PACKAGE;
//public LispSymbol    PACKAGE_SYMBOL;  // ptr to *package*
  
//public LispPackage KEYWORD;
  public LispPackage SYSTEM;

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:26:00 1997
  /**
   * SYMTAB is a pointer to the namespace used by LISP.
   * Needed for initialization of the parser.  It is
   * always the SYMTAB of the current PACKAGE;
   *
   * @see org.jatha.dynatype.LispPackage
   */
    public SymbolTable   SYMTAB; //TODO: fix so that this is ALWAYS correct, in some way.

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:26:00 1997
  /**
   * MACHINE is a pointer to the primary SECD abstract machine
   * used for executing compiled LISP code.
   *
   * @see org.jatha.machine.SECDMachine
   */
  public SECDMachine   MACHINE;

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:26:00 1997
  /**
   * SYSTEM_INFO is a pointer to the Runtime object
   * for this Applet.
   *
   * @see java.lang.Runtime
   */
  public final Runtime  SYSTEM_INFO  = Runtime.getRuntime();

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:26:00 1997
  /**
   * JATHA is a pointer to the Applet.
   */

  public static int APROPOS_TAB = 30;

  // The '.' to represent a cons cell.
  public LispSymbol DOT;

  // The list/symbol NIL.
  public static final LispList NIL = StandardLispValue.NIL;
  // The symbol T
  public LispConstant T;

	// These are used in macros
	public LispSymbol MACRO;
	public LispSymbol PRIMITIVE;
  
	public LispSymbol QUOTE;
	public LispSymbol BACKQUOTE;
	
  public LispSymbol CONS;
  public LispSymbol LIST;
  public LispSymbol APPEND;
  public LispSymbol COMMA_FN;
  public LispSymbol COMMA_ATSIGN_FN;
  public LispSymbol COMMA_DOT_FN;
  

  public LispValue COLON;
  public LispValue NEWLINE;
  public LispValue SPACE;

  // Used in CONCATENATE
  public LispSymbol STRING;

  // Used in the compiler
  public LispInteger ZERO;
  public LispInteger ONE;
  public LispInteger TWO;

  // Math constants
  public LispNumber PI;
  public LispNumber E;

  private void initializeConstants()
  {
    try
    {
      if (SYMTAB == null) {
        System.err.println("In LispValue, symtab is null!");
        throw new Exception("In LispValue init, symtab is null!");
      }
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

    SYSTEM = new StandardLispPackage(this, makeString("SYSTEM"));

    // NIL is special case - not a symbol but require be in system symbol table(?)
//	NIL.setPackage(f_systemPackage);
    
    // create initial builtin symbols
    symbol("DOT",       DOT = new StandardLispSymbol(this, "."));
    symbol("QUOTE",     QUOTE = new StandardLispSymbol(this, "QUOTE"));
    symbol("BACKQUOTE", BACKQUOTE = new StandardLispSymbol(this, "BACKQUOTE"));
    symbol("LIST",      LIST = new StandardLispSymbol(this, "LIST"));
    symbol("APPEND",    APPEND = new StandardLispSymbol(this, "APPEND"));
    symbol("CONS",      CONS = new StandardLispSymbol(this, "CONS"));
    
    symbol("COMMA",        COMMA_FN        = new StandardLispKeyword(this, "COMMA"));
    symbol("COMMA-ATSIGN", COMMA_ATSIGN_FN = new StandardLispKeyword(this, "COMMA-ATSIGN"));
    symbol("COMMA-DOT",    COMMA_DOT_FN    = new StandardLispKeyword(this, "COMMA-DOT"));

    symbol("STRING",    STRING = new StandardLispSymbol(this, "STRING"));
    
    T = new StandardLispConstant(this, "T");
    intern(makeString("T"), T);
    T.setf_symbol_value(T);

    ZERO = new StandardLispInteger(this, 0);
    ONE  = new StandardLispInteger(this, 1);
    TWO  = new StandardLispInteger(this, 2);

    E    = new StandardLispReal(this, StrictMath.E);
    PI   = new StandardLispReal(this, StrictMath.PI);

    COLON   = new StandardLispCharacter(this, ':');
    NEWLINE = new StandardLispCharacter(this, '\n');
    SPACE   = new StandardLispCharacter(this, ' ');

    MACRO     = keyword("MACRO");
    PRIMITIVE = keyword("PRIMITIVE");
  }

  // Re-initializes the above symbols, after a PACKAGE is available.
  public void initConstants2()
  {
    if (SYMTAB == null)
    {
      System.err.println("In LispValue.init(), symtab is null!");
      System.exit(1);
    }

    if (SYSTEM == null)
    {
      System.err.println("In LispValue.init(), package is null!");
      System.exit(1);
    }

  }


/* ------------------  PRIVATE VARIABLES   ------------------------------ */


  LispValue prompt, userPrompt;
  LispValue packages = null;

  public static long MAX_LIST_LENGTH_VALUE = 100000;
  public static long PRINT_LENGTH_VALUE = 512;
  public static long PRINT_LEVEL_VALUE  = 15;  // is this big enough?  Emacs uses 4 and 512 for level and length

/* ------------------  CONSTRUCTORS   ------------------------------ */


  /**
   * Create a new Jatha that does not use the GUI, does use the console for I/O and does not display help.
   */
  public Lisp()
  {
	  super();
	  
	  init();
  }
/* ------------------  NON-LISP methods   ------------------------------ */

  /**
   * Returns the entire version string.
   * @return a string containing the entire description of Algernon.
   */
  public String getVersionString()
  {
    return getVersionName() + " " +
           getVersionMajor() + "." + getVersionMinor() + "." + getVersionMicro() + getVersionType() + ", " +
           getVersionDate() + ", contact: " + getVersionURL()  ;
  }

  /**
   * Returns the program name, e.g. Algernon.
   */
  public String getVersionName()    { return VERSION_NAME;    }

  /**
   * Returns the date of this version as a string: "nn MONTH yyyy".
   */
  public String getVersionDate()    { return VERSION_DATE;    }

  /**
   * Returns a URL where you can find info about Algernon.
   */
  public String getVersionURL() { return VERSION_URL; }

  /**
   * Returns the type of release: "production", "beta" or "alpha".
   */
  public String getVersionType()    { return VERSION_TYPE;    }

  /**
   * Returns the major version number, that is, 1 in version 1.2.3.
   */
  public int getVersionMajor()      { return VERSION_MAJOR;   }

  /**
   * Returns the minor version number, that is, 2 in version 1.2.3.
   */
  public int getVersionMinor()      { return VERSION_MINOR;   }

  /**
   * Returns the micro version number, that is, 3 in version 1.2.3.
   */
  public int getVersionMicro()      { return VERSION_MICRO;   }

  void showHelp()
  {
    System.out.println("\njava org.jatha.Jatha  [-help] [-nodisplay]\n");
    System.out.println("  This is a small Common LISP compatible LISP environment.");
    System.out.println("  Use the  -nodisplay  option to suppress GUI features.");
    System.out.println("");
    System.exit(0);
  }

  public void init()
  {
    // EVAL must be before SYMTAB.
    SYMTAB  = new SymbolTable(this);

    initializeConstants();

    // Create the rest of the packages
    packages = makeList(SYSTEM);

    initConstants2();

    COMPILER     = new LispCompiler(this);
    MACHINE      = new SECDMachine(this);
    PARSER       = new LispParser(this, new InputStreamReader(System.in));


    // Need to allow *TOP-LEVEL-PROMPT* to change this.
    prompt = makeString("Jatha> ");

    // Registers LISP primitive functions.  Should only be called once.
    COMPILER.init();

    // Load any files in the /init directory  (mh) 11 May 2005
    loadInitFiles();
  }


  /**
   * Loads files in the /init directory in Jatha's jar file.
   * They must be named "01.lisp", "02.lisp", etc.  Numbers must
   * be sequential starting from "01".
   */
  protected void loadInitFiles()
  {
    NumberFormat fileNF = new DecimalFormat("00");
    String filePrefix = "init/";
    String fileSuffix = ".lisp";

    if (DEBUG) {
        System.out.println("Loading init files.");
    }

    int fileNumber  = 1;
    int fileCounter = 0;

    while (true)
    {
      String baseFilename = fileNF.format(fileNumber++) + fileSuffix;
      String filename = filePrefix + baseFilename;
      try {
        LispValue result = loadFromJar(filename);
        if (result == T)
        {
            if (DEBUG) {
                System.out.println("  loaded " + baseFilename);
            }
          fileCounter++;
        }

        else if (result == NIL)  // No such file
          break;

        else
        {
            if(useConsole) {
                System.err.println("  error loading " + filename + ", " + result);
            }
        }
      } catch (Exception e) {
        System.err.println("Jatha.loadInitFiles: " + e.getMessage());
        break;
      }
    }

    if (DEBUG) {
        System.out.println("Loaded " + fileCounter + " file(s).");
    }
  }



  /**
   * Loads a file from the container holding this class.
   * The container is normally a JAR file.
   * Uses getResource to create a stream, then calls load(Reader).
   * @param filename The file to be loaded, without an initial "/".  Will be converted to a Java String using toStringSimple.
   * @param jarFile The URL of the jar file from which to load the resource.
   * @return T if the file was successfully loaded, NIL if the file doesn't exist and a String containing an error message otherwise.
   */
  public LispValue loadFromJar(LispValue filename, LispValue jarFile)
  {
    return loadFromJar(filename.toStringSimple(), jarFile.toStringSimple());
  }

  /**
   * Loads a file from the container holding this class.
   * The container is normally a JAR file.
   * Uses getResource to create a stream, then calls load(Reader).
   * @param filename The file to be loaded, WITHOUT an initial "/".
   * @param jarFileString The name of the jar file to load the file from.
   * @return T if the file was successfully loaded, NIL if the file doesn't exist and a String containing an error message otherwise.
   */
  public LispValue loadFromJar(String filename, String jarFileString)
  {
    if (DEBUG)
      System.out.println("  Jatha.loadFromJar: looking for " +
                         filename + " in " + jarFileString);

    try {
      JarFile jarFile = new JarFile(jarFileString);
      JarEntry je = jarFile.getJarEntry(filename);
      if (je == null)
        return NIL;

      LispValue result = load(new InputStreamReader(jarFile.getInputStream(je)));
      jarFile.close();
      return result;
    } catch (IOException ioe) {
      return makeString(ioe.getMessage());
    } catch (SecurityException se) {
      return makeString(se.getMessage());
    } catch (CompilerException ce) {
      return makeString(ce.getMessage());
    } catch (Exception e) {
      return makeString(e.getMessage());
    }
  }


  /**
   * Loads a file from the container holding this class.
   * The container is normally a JAR file.
   * Uses getResource to create a stream, then calls load(Reader).
   * @param filename The file to be loaded, without an initial "/".  Will be converted to a Java String using toStringSimple.
   * @return T if the file was successfully loaded, NIL if the file doesn't exist and a String containing an error message otherwise.
   */
  public LispValue loadFromJar(LispValue filename)
  {
    return loadFromJar(filename.toStringSimple());
  }


  /**
   * Loads a file from the container holding this class.
   * The container is normally a JAR file.
   * Uses getResource to create a stream, then calls load(Reader).
   * @param filename The file to be loaded, WITHOUT an initial "/".
   * @return T if the file was successfully loaded, NIL if the file doesn't exist and a String containing an error message otherwise.
   */
  public LispValue loadFromJar(String filename)
  {
    if (DEBUG)
      System.out.println("  Jatha.loadFromJar: looking for " + filename + " in the jar file.");

    InputStream resourceStream =
        getClass().getClassLoader().getResourceAsStream(filename);

    if (resourceStream == null)
      return NIL;

    else
      try {
        load(new InputStreamReader(resourceStream));
        return T;
      } catch (Exception e) {
    	  e.printStackTrace();
        return makeString(e.getMessage());
      }
  }

  /**
   * Evaluates a LISP expression in a Java string, such as "(* 5 7)".
   * To evaluate an expression with variables, there are several options:
   * <pre>
   *   eval("(let ((x 7)) (* 5 x)))");
   *   eval("(progn (setq x 7) (* 5 x))");
   * </pre>
   * Or use separate eval statements:
   * <pre>
   *   eval("setq x 7");
   *   eval("(* 5 x)");
   * </pre>
   */
  public LispValue eval(String expr)
  {
    LispValue input = NIL;

    // READ
    try {
      PARSER.setInputString(expr);
      input = PARSER.parse();
      return eval(input);
    } catch (EOFException e) {
      System.err.println("Incomplete input.");
      return NIL;
    }
  }


  /**
   * Standard LISP eval function.
   * @param inValue a parsed LISP expression, such as the output from Jatha.parse().
   * @see #parse(String)
   */
  public LispValue eval(LispValue inValue)
  {
    return eval(inValue, NIL);
  }

  /**
   * Standard LISP eval function.
   * @param inValue a parsed LISp expression such as the output from Jatha.parse()
   * @param vars a nested list of global variables and values, such as (((a . 3) (b . 5)) ((c . 10)))
   * @see #parse(String)
  */
  public LispValue eval(LispValue inValue, final LispValue vars)
  {
    LispValue code, value;

    final LispValue varNames = parseVarNames_new(vars);
    final LispValue varValues = parseVarValues_new(vars);

    try {
      // compile
      code  = COMPILER.compile(MACHINE, inValue, varNames);

      // eval
      value = MACHINE.Execute(code, varValues);
    }
    catch (LispUndefinedFunctionException ufe) {
      System.err.println("ERROR: " + ufe.getMessage());
      return makeString(ufe.getMessage());
    } catch (CompilerException ce) {
      System.err.println("ERROR: " + ce);
      return makeString(ce.toString());
    } catch (LispException le) {
      System.err.println("ERROR: " + le.getMessage());
      le.printStackTrace();
      return makeString(le.getMessage());
    }
    catch (Exception e) {
      System.err.println("Unknown error: " + e.getMessage());
      e.printStackTrace();
      return makeString(e.getMessage());
    }

    return value;
  }

  /**
   * Expects a list with this format (((A 13) (C 7))((X "Zeta"))) and returns a list with this format ((A C)(X))
   */
  /*
  private LispValue parseVarNames(final LispValue vars) {
    LispValue outp = NIL;
    if (vars.basic_null())
      return outp;

    for(final Iterator<LispValue> iter = vars.iterator();iter.hasNext();) {
      final LispValue current = iter.next();
      LispValue inner = NIL;
      for(final Iterator<LispValue> iter2 = current.iterator();iter2.hasNext();) {
        final LispValue currInt = iter2.next();
        inner = makeCons(currInt.car(),inner);
      }
      outp = makeCons(inner.nreverse(),outp);
    }
    return outp.nreverse();
  }
*/
  
  /**
   * Not sure why parseVarNames has such a complicated structure.
   * This one expects variables of the form ((A . 7) (B . 13) (C . (foo)))
   * the CAR of each pair is the variable and the CDR of each pair is the value.
   */
  private LispValue parseVarNames_new(final LispValue vars)
  {
    LispValue outp = NIL;
    if (vars == NIL)
      return outp;

    for (final Iterator<LispValue> iter = vars.iterator(); iter.hasNext();)
    {
      final LispValue current = iter.next();
      outp = makeCons(this.car(current), outp);
    }
    return outp.nreverse();
  }


  /**
   * Not sure why parseVarNames has such a complicated structure.
   * This one expects variables of the form ((A . 7) (B . 13) (C . (foo)))
   * the CAR of each pair is the variable and the CDR of each pair is the value.
   */
  private LispValue parseVarValues_new(final LispValue vars)
  {
    LispValue outp = NIL;
    if (vars == NIL)
      return outp;

    for (final Iterator<LispValue> iter = vars.iterator(); iter.hasNext();)
    {
      final LispValue current = iter.next();
      outp = makeCons(this.cdr(current), outp);
    }
    return outp.nreverse();
  }


  /**
   * Expects a list with this format (((A 13) (C 7))((X "Zeta"))) and returns a list with this format ((13 7)("Zeta"))
   */
  /*
  private LispValue parseVarValues(final LispValue vars) {
    LispValue outp = NIL;
    if (vars.basic_null())
      return outp;

    for(final Iterator<LispValue> iter = vars.iterator();iter.hasNext();) {
      final LispValue current = iter.next();
      LispValue inner = NIL;
      for(final Iterator<LispValue> iter2 = current.iterator();iter2.hasNext();) {
        final LispValue currInt = iter2.next();
        inner = makeCons(currInt.cdr(),inner);
      }
      outp = makeCons(inner.nreverse(),outp);
    }
    return outp.nreverse();
  }
*/


  void readEvalPrintLoop()
  {
    LispValue input, code, value, myprompt;
    boolean   validInput;
//    LispValue oldPackageSymbolValue = PACKAGE;

    // Need to allow *TOP-LEVEL-PROMPT* to change this.
    myprompt = makeString("Jatha >");

    System.out.println("Run (EXIT) to stop.");

    input = NIL;

    while (true)
    {
      System.out.println();
      myprompt.princ();
      System.out.flush();

      // READ
      validInput = true;
      try { input = PARSER.parse(); }
      catch (EOFException e) {
        validInput = false;
        System.err.println("Incomplete input.");
      }

      if (validInput)
      {
        try {
          code  = COMPILER.compile(MACHINE, input, NIL);  // No globals for now
        } catch (Exception e) {
          System.out.println("Unable to compile " + input + "\n  " + e);
          continue;
        }

        // EVAL

        try
        {
          value = MACHINE.Execute(code, NIL);
        } catch (Exception e2) {
          System.out.println("Unable to evaluate " + input + "\n  " + e2);
          continue;
        }

        // PRINT
        value.prin1();
      }
    }
  }


  /**
   * Returns the LISP compiler used by this instance of Jatha.
   */
  public LispCompiler getCompiler()
  {
    return COMPILER;
  }


  /**
   * Returns the LISP Parser used by this instance of Jatha.
   */
  public LispParser getParser()
  {
    return PARSER;
  }

  /**
   * Returns the Symbol Table used by this instance of Jatha.
   */
  public SymbolTable getSymbolTable()
  {
    return SYMTAB;
  }

  /**
   * Parses a string and returns the first form in the string.
   * <br>caseSensitivity:
   * <ul>
   *   <li>LispParser.UPCASE (the default)</li>
   *   <li>LispParser.DOWNCASE</li>
   *   <li>LispParser.PRESERVE</li>
   * </ul>
   */
  public LispValue parse(String s, int caseSensitivity)
    throws EOFException
  {
    return new LispParser(this, s, caseSensitivity).parse();
  }


  /**
   * Parses a string and returns the first form in the string.
   */
  public LispValue parse(String s)
    throws EOFException
  {
    return parse(s, LispParser.UPCASE);
  }

  /**
   * Loads the contents of a Reader (stream).
   * Useful for loading from a jar file.
   * Contributed by Stephen Starkey.
   */
  public LispValue load(Reader in) throws CompilerException
  {
    return load(in, false);
  }

  /**
   * Loads the contents of a Reader (stream).
   * Useful for loading from a jar file.
   * Contributed by Stephen Starkey.
   */
  public LispValue load(Reader in, boolean verbose)
      throws CompilerException
  {
    // System.err.println("Loading: verbose is " + verbose);

    BufferedReader buff = new BufferedReader(in);

    LispParser fileparser = new LispParser(this, buff);
    LispValue  input, code;
    boolean    atLeastOneResult = false;

    // Read and Eval stream until EOF.
    try {
      while (true)
      {
        input = fileparser.parse();

        code  = COMPILER.compile(MACHINE, input, NIL);

        LispValue value = MACHINE.Execute(code, NIL);
        atLeastOneResult = true;

        if (verbose)
        {
          System.out.println(value.toString());
        }
      }
    } catch (IOException ioe) {
      try {
        in.close();
      } catch (IOException e2) {
        return T;
      }
      catch (Exception ex)
      {
    	final Exception ex2 = ex;
    	throw new LispValueNotAConsException();
      }
    }

    if (atLeastOneResult)
        return T;
      else
        return NIL;
  }


  /** Loads a file.
   * Argument is guaranteed to be a LispString.
   */
  public LispValue load(LispValue filenameVal)
  {
    String filename = ((LispString) filenameVal).getValue();

    try {
      return load(new FileReader(filename));
    } catch (FileNotFoundException e) {
      System.err.println(";; *** File not found: " + filename);
      return NIL;
    } catch (CompilerException ce) {
      System.err.println("Error while reading file " + filename + ":\n" + ce.toString());
    } catch (Exception e) {
      System.err.println("Error closing file " + filename);
      return T;
    }
    return NIL;
  }


  /**
   * Creates a reader from the input string and passes it to load(Reader).
   * Verbose is false.
   */
  public LispValue load(String string)
  {
    return load(string, false);
  }

  /**
   * Creates a reader from the input string and passes it to load(Reader).
   */
  public LispValue load(String string, boolean verbose)
  {
    try {
      return load(new StringReader(string), verbose);
    } catch (CompilerException ce) {
      System.err.println("Error in input: " + ce.toString());
    } catch (Exception e) {
      System.err.println("Error handling input string: " + e.getMessage());
      e.printStackTrace();
      return T;
    }
    return NIL;
  }


  // ----------  LISP-related methods  -----------------


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Fri May  9 22:30:22 1997
  /**
   * Looks up the package on the packages list.
   * Input should be a string, symbol or package.  All
   * names and nicknames are searched.
   *
   * @param packageName a LISP string or keyword
   * @return LispValue the package, or NIL
   */
/*  public LispValue findPackage(LispValue packageName)
  {
    if (packageName instanceof LispPackage)
      return packageName;

    if (packageName instanceof LispSymbol)
      packageName = ((LispSymbol)packageName).symbol_name();

    return findPackage(((LispString)(packageName)).getValue());
  }

  public LispValue findPackage(String packageNameStr)
  {
    if (packages == null)
      return NIL;

    if ("SYSTEM".equals(packageNameStr))
    	return SYSTEM;
    
    throw new LispValueNotAPackageException(packageNameStr);
  }*/

//  public LispValue allPackages() { return packages; }


  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:31:49 1997
  /**
   * This method prints out information on the amount of
   * memory free in the Java space.  It optionally takes
   * an PrintStream as an argument, but defaults to
   * System.out.
   * @see java.lang.Runtime
   * @return void
   */
  public long free()
  {
    return free(System.out);
  }

  public long free(PrintStream out)
  {
    long free  = SYSTEM_INFO.freeMemory();
    long total = SYSTEM_INFO.totalMemory();

    out.println(";; " + free + "/" + total + "bytes ("
                + (long)(100.0 * ((double)free / (double)total))
                + "%) of memory free.");
    return free;
  }

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:31:49 1997
  /**
   * This method turns Java method tracing on.
   * Right now, this doesn't seem to do anything, but
   * perhaps we need to compile with debugging turned on.
   * @see java.lang.Runtime
   * @param on
   */
  public void javaTrace(boolean on)
  {
    SYSTEM_INFO.traceMethodCalls(on);  // traceInstructions(on) is also available
  }

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:31:49 1997
  /**
   * This method causes the Java runtime to performs a GC.
   * @see java.lang.Runtime
   */
  public void gc()
  {
    if(useConsole) {
        System.out.print("\n;;  GC...");  System.out.flush();
    }
    SYSTEM_INFO.gc();
    if(useConsole) {
        System.out.println("done");     System.out.flush();
    }
  }

  // @author  Micheal S. Hewett    hewett@cs.stanford.edu
  // @date    Thu Feb  6 09:31:49 1997
  /**
   * This method causes the Java runtime to performs
   * a GC.  It calls the runFinalization() method
   * first, in order to reclaim as much memory as
   * possible.
   * @see java.lang.Runtime
   */
  public void gc_full()
  {
    String msg = "\n;;  GC Full...";
    if (useConsole)
    {
      System.out.print(msg);
      System.out.flush();
    }

    System.runFinalization();
    System.gc();
    if (useConsole)
    {
      System.out.println("done");
    }
    free();
  }

  // ----------------  PACKAGE stuff  -----------------------
  /**
   * This is not yet implemented.  Returns the current value of Jatha.PACKAGE.
   * @param args is not used
   * @return Jatha.PACKAGE
   */
  public LispPackage defpackage(LispValue args)
  {
    return SYSTEM;
  }

  // -----  ActionListener interface  ------------------
  // ---------------------  methods formerly in LispValueFactory  ------------------
  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:08:32 1997
	/**
	 * makeCons(a,b) creates a new Cons cell, initialized with
	 * the values a and b as the CAR and CDR respectively.
	 *
	 * @see LispCons
	 * @param theCar
	 * @param theCdr
	 * @return LispValue
	 *
	 */
/*	public LispCons makeCons(LispValue theCar, LispConsOrNil theCdr)
	{
		return new StandardLispCons(this, theCar, theCdr);
	}*/
	public LispCons makeCons(LispValue theCar, LispValue theCdr)
	{
//		assert theCdr instanceof LispConsOrNil;
		return new StandardLispCons(this, theCar, theCdr);
	}
	public LispValue makeBool(boolean predicate)
	{
		return predicate ? T : NIL;
	}


	//* @author  Micheal S. Hewett    hewett@cs.stanford.edu
	//* @date    Thu Feb 20 12:10:00 1997
	/**
	 * Creates a LISP list from the elements of the Collection.
	 * which must be LispValue types.
	 *
	 * @see LispValue
	 *
	 */
	public LispList makeList(Collection<LispValue> elements)
	{
		// Use array so as to iterate from the end to the beginning.
		Object[] elArray = elements.toArray();
		LispList result = NIL;

		for (int i = elArray.length - 1; i >= 0; i--)
			result = new StandardLispCons(this, (LispValue)(elArray[i]), result);

		return result;
	}


	// Removed previous versions of this method that had 1, 2, 3 or 4 parameters.
	// (mh) 22 Feb 2007  also changed return type to LispConsOrNil from LispCons.
	/**
	 * This is a Java 5-compatible version of makeList that
	 * accepts any number of arguments.
	 * Returns NIL if no arguments are passed.
	 * makeList(NIL) returns (NIL) - a list containing NIL.
	 */
	public LispList makeList(LispValue... parts)
	{
		LispList result = NIL;
		for (int i = parts.length-1 ; i >= 0; i--)
			result = new StandardLispCons(this, parts[i], result);
		return result;
	}
  

  /**
   * Each element of the collection should be a LispConsOrNil.
   * The elements will be non-destructively appended to each other.
   * The result is one list.
   * Note that this operation is expensive in terms of storage.
   */

  public LispList makeAppendList(Collection<LispValue> elements)
  {
    if (elements.size() == 0)
      return NIL;

    LispValue result = NIL;
    for (Iterator<LispValue> iterator = elements.iterator(); iterator.hasNext();)
    {
      LispValue o = iterator.next();
      result = result.append(o);
    }

    return (LispList) result;
  }


  /**
   * Each element of the collection should be a LispConsOrNil.
   * The elements will be destructively appended to each other.
   * The result is one list.
   */

  public LispList makeNconcList(Collection<LispValue> elements)
  {
    if (elements.size() == 0)
      return NIL;

    LispValue result = NIL;
    for (Iterator<LispValue> iterator = elements.iterator(); iterator.hasNext();)
    {
      LispValue o = iterator.next();
      result = result.nconc(o);
    }

    return (LispList) result;
  }


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:16:21 1997
  /**
   * Creates a LispInteger type initialized with the value
   * provided and returns it.
   * @see LispInteger
   * @see LispValue
   * @return LispInteger
   *
   */
  public LispInteger makeInteger(Long value)
  {
    return new StandardLispInteger(this, value.longValue());
  }

  public LispInteger makeInteger(long value)
  {
    return new StandardLispInteger(this, value);
  }

  public LispInteger makeInteger(Integer value)
  {
    return new StandardLispInteger(this, value.longValue());
  }

  public LispInteger makeInteger(int value)
  {
    return new StandardLispInteger(this, value);
  }

  public LispInteger makeInteger()
  {
    return new StandardLispInteger(this, 0);
  }

  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Tue May 20 23:09:54 1997
  /**
   * Creates a LispBignum type initialized with the value provided.
   * @see LispBignum
   * @see java.math.BigInteger
   */
  public LispBignum makeBignum(BigInteger value)
  {
    return new StandardLispBignum(this, value);
  }

  public LispBignum makeBignum(LispInteger value)
  {
    return new StandardLispBignum(this, BigInteger.valueOf(value.getLongValue()));
  }

  public LispBignum makeBignum(double value)
  {
    return new StandardLispBignum(this, BigInteger.valueOf((long) value));
  }

  public LispBignum makeBignum(long value)
  {
    return new StandardLispBignum(this, BigInteger.valueOf(value));
  }


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:19:15 1997
  /**
   * Creates an instance of LispReal initialized with
   * the given value.
   * @see LispInteger
   * @see LispValue
   * @return LispReal
   */
  public LispReal makeReal(Double value)
  {
    return new StandardLispReal(this, value.doubleValue());
  }

  public LispReal makeReal(double value)
  {
    return new StandardLispReal(this, value);
  }

  public LispReal makeReal(Float value)
  {
    return new StandardLispReal(this, value.doubleValue());
  }

  public LispReal makeReal(float value)
  {
    return new StandardLispReal(this, value);
  }

  public LispReal makeReal()
  {
    return new StandardLispReal(this, 0.0);
  }


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:20:13 1997
  /**
   * Creates a LispString from a Java string.
   *
   * @see LispString
   * @see LispValue
   * @return LispString
   */
  public LispString makeString(String str)
  {
    return new StandardLispString(this, str);
  }


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:20:57 1997
  /**
   * Creates a LispSymbol from a string or LispString.
   * This method does <b>not</b> intern the symbol.
   *
   * @see LispSymbol
   * @see LispValue
   * @return LispSymbol
   */
  public LispSymbol makeSymbol(String symbolName)
  {
    return new StandardLispSymbol(this, symbolName);
  }

  public LispSymbol makeSymbol(LispString symbolName)
  {
    return new StandardLispSymbol(this, symbolName);
  }


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:20:57 1997
  /**
   * Creates a LispConstant (a type of Symbol whose value
   * can not be changed).  This method does <b>not</b>
   * intern the symbol.
   *
   * @see LispConstant
   * @see LispSymbol
   * @see LispValue
   * @return LispSymbol
   */
  public LispSymbol makeConstant(String symbolName)
  {
    return new StandardLispConstant(this, symbolName);
  }

  public LispSymbol makeConstant(LispString symbolName)
  {
    return new StandardLispConstant(this, symbolName);
  }


  /**
   * Turns a Java object into a LISP object.
   *
   * @param obj
   */
  public LispValue toLisp(Object obj) // TODO: Is this where we use dynatype.LispForeignObject?
  {
    if (obj == null)
      return NIL;

    if (obj instanceof LispValue)
      return (LispValue) obj;

    if (obj instanceof Integer)
      return new StandardLispInteger(this, ((Integer) obj).intValue());

    else if (obj instanceof Long)
      return new StandardLispInteger(this, ((Long) obj).longValue());

    else if (obj instanceof Double)
      return new StandardLispReal(this, ((Double) obj).doubleValue());

    else if (obj instanceof Float)
      return new StandardLispReal(this, ((Float) obj).doubleValue());

    else if (obj instanceof String)
      return new StandardLispString(this, (String) obj);

    try
    {
      return (new LispParser(this, obj.toString(), LispParser.PRESERVE)).parse();
    } catch (Exception e)
    {
      System.err.println("Error in Jatha.toLisp(" + obj + ")");
    }
    return NIL;
  }


  // --- SYSTEM PACKAGE functions  ---


	/**
	 * This functions does not generate LispExceptions and assumes that all
	 * agruments are correct
	 */
	public static LispValue car(LispValue arg)
	{
		return ((LispList)arg).car();
	}
	public static LispValue cdr(LispValue arg) 
	{
		return ((LispList)arg).cdr();
	}
	public static LispValue nth(long i, LispCons arg)
	{
		while (--i > 0)
			arg = (LispCons)arg.cdr();
		return arg.car();
	}
	public static LispValue nth(LispCons ij, LispCons arg)
	{
		long i = ((LispInteger)(car(ij))).getLongValue();
		long j = ((LispInteger)(cdr(ij))).getLongValue();
		
		arg = (LispCons)Lisp.nth(i, arg);
		while (--j > 0)
			arg = (LispCons)arg.cdr();
		
		return arg;
	}
	
	public static LispValue cddr(LispValue arg)
	{
		return cdr(cdr(arg));
	}
	

	public static void main(String[] args)
	{
		if (System.in == null)
			return;
		Lisp lisp = new Lisp();

		LispParser cli = new LispParser(lisp,
				args.length > 0
				?	new StringReader(args[0])
				:	new InputStreamReader(System.in));
		while (true) {
			System.out.print("> ");
			try {
				System.out.println(lisp.eval(cli.read()).toString());
			} catch (EOFException e) {
				return; // done.
			}
		}
	}
	public void exit()
	{
		System.exit(0);
	}
	
	
	
	////////////// EVAL
	public LispSymbol intern(String str)
	{
		return intern(makeString(str));
	}
	
	public LispSymbol symbol(String symbolString)
	{
		return intern(symbolString, SYSTEM);
	}
	public LispSymbol symbol(String symbolString, LispSymbol symbol)
	{
		return symbol(makeString(symbolString), symbol);
	}
	public LispSymbol symbol(LispString symbolString, LispSymbol symbol)
	{
		symbol.setPackage(SYSTEM);
		SYSTEM.addSymbol(symbolString, symbol);
		return symbol;
	}
	
	  public LispSymbol intern(LispString symbolString)
	  {
	    if (COLON.eql(symbolString.basic_elt(0)) != NIL)
	      return keyword((LispString)(symbolString.substring(new StandardLispInteger(this, 1))));
	    else
	      return intern(symbolString, SYSTEM);
	  }


	  public LispSymbol intern(LispString symbolString, LispPackage pkg)
	  {
		  LispSymbol newSymbol;

	    // First, check to see whether one exists already.
	    newSymbol = pkg.getSymbol(symbolString);

	    if (newSymbol != null)    // Already there, don't add it again.
	    {
	      // System.out.println("Package " + pkg + " already owns " + newSymbol);
	      return newSymbol;
	    }
	    else
	    {
	        newSymbol = makeSymbol(symbolString);

	        return intern(symbolString, newSymbol);
	    }
	  }

	  // We need this for the startup when we create NIL and LispValue.T.
	  // Actually, LispValue is always a LispSymbol, but because of NIL's strange
	  // properties, we must make the type be LispValue.
	  public LispSymbol intern(LispString symbolString, LispSymbol symbol)
	  {
	      symbol.setPackage(SYSTEM);
	      SYSTEM.addSymbol(symbolString, symbol);
	      return symbol;
	  }

/*	  // We need this for the startup when we create NIL and LispValue.T.
	  // Actually, LispValue is always a LispSymbol, but because of NIL's strange
	  // properties, we must make the type be LispValue.
	  public LispSymbol intern(LispString symbolString, LispSymbol symbol,
	                          LispPackage pkg)
	  {
	    if (pkg == null)   // uninterned symbol
	      return symbol;
	    else
	    {
	      symbol.setPackage(pkg);
	      pkg.addSymbol(symbolString, symbol);
	      return symbol;
	    }
	  }*/

	  public LispSymbol intern(String str, LispPackage pkg)
	  {
	    return intern(makeString(str), pkg);
	  }
	  
	  public LispSymbol keyword(String str)
	  {
		  return keyword(makeString(str));
	  }
	  public LispSymbol keyword(LispString symbolString)
	  {
		  LispSymbol newSymbol;

		  // First, check to see whether one exists already.
	    newSymbol = SYSTEM.getSymbol(symbolString);

	    if (newSymbol == null)    // Already there, don't add it again.
	    {
	        String newString = symbolString.toStringSimple().toUpperCase();
	        // Symbols must be uppercase
	        newSymbol = new StandardLispKeyword(this, new StandardLispString(this, newString));

	        newSymbol.setPackage(SYSTEM);
	        SYSTEM.addSymbol(symbolString, newSymbol);
	        return newSymbol;
	    }
	    return newSymbol;
	  }

	public LispValue setf_symbol_value(LispValue symbol, LispValue value)
	{
		return symbol.setf_symbol_value(value);
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
			if ((code == null) || (code == NIL))
				return false;
		    
			if (code instanceof LispSymbol)
				if (code.fboundp())
					code = code.symbol_function();
				else
					return false;

			if (code instanceof LispFunction)
				code = ((LispFunction)code).getCode();

			if (code instanceof LispList) {
				LispValue a = code.first();
				LispValue c = code.getLisp().PRIMITIVE;
	        
				if (a == c)
					return true;
			}
			return false;
	  }
	  
}


