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
  public LispPackage   PACKAGE;
  public LispSymbol    PACKAGE_SYMBOL;  // ptr to *package*

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
  public static LispList NIL = new StandardLispNIL();
  // The symbol T
  public LispConstant T;

	// These are used in macros
	public LispSymbol QUOTE;
	public LispSymbol MACRO;
	public LispSymbol PRIMITIVE;
  
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

  // Types
  // todo: move this into external lisp package
  public LispSymbol ARRAY_TYPE;
  public LispSymbol ATOM_TYPE;
  public LispSymbol BIGNUM_TYPE;
  public LispSymbol BOOLEAN_TYPE;
  public LispSymbol CHARACTER_TYPE;
  public LispSymbol COMPLEX_TYPE;
  public LispSymbol CONS_TYPE;
  public LispSymbol DOUBLE_FLOAT_TYPE;
  public LispSymbol FLOAT_TYPE;
  public LispSymbol FUNCTION_TYPE;
  public LispSymbol HASHTABLE_TYPE;
  public LispSymbol INTEGER_TYPE;
  public LispSymbol MACRO_TYPE;
  public LispSymbol NULL_TYPE;
  public LispSymbol NUMBER_TYPE;
  public LispSymbol PACKAGE_TYPE;
  public LispSymbol PATHNAME_TYPE;
  public LispSymbol REAL_TYPE;
  public LispSymbol STREAM_TYPE;
  public LispSymbol STRING_TYPE;
  public LispSymbol SYMBOL_TYPE;
  public LispSymbol VECTOR_TYPE;

  /**
   * This is used in apropos_print on StandardLispSymbol.
   * Not really for public consumption.
   * @param object a LispSymbol
   * @return true if it is equal to ARRAY_TYPE, ATOM_TYPE, etc.
   */
  public boolean isType(LispValue object)
  {
    return ((object == ARRAY_TYPE)
            || (object == ATOM_TYPE)
            || (object == BIGNUM_TYPE)
            || (object == BOOLEAN_TYPE)
            || (object == CHARACTER_TYPE)
            || (object == COMPLEX_TYPE)
            || (object == CONS_TYPE)
            || (object == DOUBLE_FLOAT_TYPE)
            || (object == FLOAT_TYPE)
            || (object == FUNCTION_TYPE)
            || (object == HASHTABLE_TYPE)
            || (object == INTEGER_TYPE)
            || (object == MACRO_TYPE)
            || (object == NUMBER_TYPE)
            || (object == NULL_TYPE)
            || (object == PACKAGE_TYPE)
            || (object == PATHNAME_TYPE)
            || (object == REAL_TYPE)
            || (object == STREAM_TYPE)
            || (object == STRING_TYPE)
            || (object == SYMBOL_TYPE)
            || (object == VECTOR_TYPE)
            );
  }
    private LispPackage f_systemPackage = null;
    private LispPackage f_keywordPackage = null;

	public LispPackage KEYWORD;
	public LispPackage SYSTEM;

  private void initializeConstants()
  {
    try
    {
      if (SYMTAB == null)
      {
        System.err.println("In LispValue, symtab is null!");
        throw new Exception("In LispValue init, symtab is null!");
      }
    } catch (Exception e)
    {
      System.out.println(e);
      e.printStackTrace();
    }

    f_systemPackage = SYSTEM = new StandardLispPackage(this, makeString("SYSTEM"));
    f_keywordPackage = KEYWORD = new StandardLispPackage(this, makeString("KEYWORD"));

    // NIL is special case - not a symbol but require be in system symbol table(?)
//	NIL.setPackage(f_systemPackage);
    
    DOT = new StandardLispSymbol(this, ".");
    intern(makeString("DOT"), DOT, f_systemPackage);

    QUOTE = internAndExport("QUOTE", f_systemPackage);
    
    BACKQUOTE = new StandardLispSymbol(this, "BACKQUOTE");
    intern(makeString("BACKQUOTE"), BACKQUOTE, f_systemPackage);

    LIST = new StandardLispSymbol(this, "LIST");
    intern(makeString("LIST"), LIST, f_systemPackage);

    APPEND = new StandardLispSymbol(this, "APPEND");
    intern(makeString("APPEND"), APPEND, f_systemPackage);

    CONS = new StandardLispSymbol(this, "CONS");
    intern(makeString("CONS"), CONS, f_systemPackage);

    COMMA_FN        = new StandardLispKeyword(this, "COMMA");
    intern(makeString("COMMA"), COMMA_FN, f_keywordPackage);

    COMMA_ATSIGN_FN = new StandardLispKeyword(this, "COMMA-ATSIGN");
    intern(makeString("COMMA-ATSIGN"), COMMA_ATSIGN_FN, f_keywordPackage);

    COMMA_DOT_FN    = new StandardLispKeyword(this, "COMMA-DOT");
    intern(makeString("COMMA-DOT"), COMMA_DOT_FN, f_keywordPackage);

    T = new StandardLispConstant(this, "T");
    intern(makeString("T"), T, f_systemPackage);
    T.setf_symbol_value(T);

    ZERO = new StandardLispInteger(this, 0);
    ONE  = new StandardLispInteger(this, 1);
    TWO  = new StandardLispInteger(this, 2);

    E    = new StandardLispReal(this, StrictMath.E);
    PI   = new StandardLispReal(this, StrictMath.PI);

    COLON   = new StandardLispCharacter(this, ':');
    NEWLINE = new StandardLispCharacter(this, '\n');
    SPACE   = new StandardLispCharacter(this, ' ');

    STRING = new StandardLispSymbol(this, "STRING");
    intern(makeString("STRING"), STRING, f_systemPackage);


    // Lisp data types  --------------------------------------------

    ARRAY_TYPE = new StandardLispSymbol(this, "ARRAY");
    intern(makeString("ARRAY"), ARRAY_TYPE, f_systemPackage);

    ATOM_TYPE = new StandardLispSymbol(this, "ATOM");
    intern(makeString("ATOM"), ATOM_TYPE, f_systemPackage);

    BIGNUM_TYPE = new StandardLispSymbol(this, "BIGNUM");
    intern(makeString("BIGNUM"), BIGNUM_TYPE, f_systemPackage);

    BOOLEAN_TYPE = new StandardLispSymbol(this, "BOOLEAN");
    intern(makeString("BOOLEAN"), BOOLEAN_TYPE, f_systemPackage);

    CHARACTER_TYPE = new StandardLispSymbol(this, "CHARACTER");
    intern(makeString("CHARACTER"), CHARACTER_TYPE, f_systemPackage);

    COMPLEX_TYPE = new StandardLispSymbol(this, "COMPLEX");
    intern(makeString("COMPLEX"), COMPLEX_TYPE, f_systemPackage);

    CONS_TYPE = new StandardLispSymbol(this, "CONS");
    intern(makeString("CONS"), CONS_TYPE, f_systemPackage);

    DOUBLE_FLOAT_TYPE = new StandardLispSymbol(this, "DOUBLE-FLOAT");
    intern(makeString("DOUBLE-FLOAT"), DOUBLE_FLOAT_TYPE, f_systemPackage);

    FLOAT_TYPE = new StandardLispSymbol(this, "FLOAT");
    intern(makeString("FLOAT"), FLOAT_TYPE, f_systemPackage);

    FUNCTION_TYPE = new StandardLispSymbol(this, "FUNCTION");
    intern(makeString("FUNCTION"), FUNCTION_TYPE, f_systemPackage);

    HASHTABLE_TYPE = new StandardLispSymbol(this, "HASH-TABLE");
    intern(makeString( "TABLE"), HASHTABLE_TYPE, f_systemPackage);

    INTEGER_TYPE = new StandardLispSymbol(this, "INTEGER");
    intern(makeString("INTEGER"), INTEGER_TYPE, f_systemPackage);

    NULL_TYPE = new StandardLispSymbol(this, "NULL");
    intern(makeString("NULL"), NULL_TYPE, f_systemPackage);

    MACRO_TYPE = new StandardLispSymbol(this, "MACRO");
    intern(makeString("MACRO"), NULL_TYPE, f_systemPackage);

    NUMBER_TYPE = new StandardLispSymbol(this, "NUMBER");
    intern(makeString("NUMBER"), NUMBER_TYPE, f_systemPackage);

    PACKAGE_TYPE = new StandardLispSymbol(this, "PACKAGE");
    intern(makeString("PACKAGE"), PACKAGE_TYPE, f_systemPackage);

    PATHNAME_TYPE = new StandardLispSymbol(this, "PATHNAME");
    intern(makeString("PATHNAME"), PATHNAME_TYPE, f_systemPackage);

    REAL_TYPE = new StandardLispSymbol(this, "REAL");
    intern(makeString("REAL"), REAL_TYPE, f_systemPackage);

    STREAM_TYPE = new StandardLispSymbol(this, "STREAM");
    intern(makeString("STREAM"), STREAM_TYPE, f_systemPackage);

    STRING_TYPE = new StandardLispSymbol(this, "STRING");
    intern(makeString("STRING"), STRING_TYPE, f_systemPackage);

    SYMBOL_TYPE = new StandardLispSymbol(this, "SYMBOL");
    intern(makeString("SYMBOL"), SYMBOL_TYPE, f_systemPackage);

    VECTOR_TYPE = new StandardLispSymbol(this, "VECTOR");
    intern(makeString("VECTOR"), VECTOR_TYPE, f_systemPackage);
    
    MACRO = keywordAndExport("MACRO");
    PRIMITIVE = keywordAndExport("PRIMITIVE");

  }

  // Re-initializes the above symbols, after a PACKAGE is available.
  public void initConstants2()
  {
    if (SYMTAB == null)
    {
      System.err.println("In LispValue.init(), symtab is null!");
      System.exit(1);
    }

    if (PACKAGE == null)
    {
      System.err.println("In LispValue.init(), package is null!");
      System.exit(1);
    }

    f_systemPackage.export(DOT);
    f_systemPackage.export(NIL);
    f_systemPackage.export(QUOTE);
    f_systemPackage.export(BACKQUOTE);
    f_systemPackage.export(T);
    f_systemPackage.export(LIST);
    f_systemPackage.export(APPEND);
    f_systemPackage.export(CONS);
    f_keywordPackage.export(COMMA_FN);
    f_keywordPackage.export(COMMA_ATSIGN_FN);
    f_keywordPackage.export(COMMA_DOT_FN);
    f_systemPackage.export(ARRAY_TYPE);
    f_systemPackage.export(ATOM_TYPE);
    f_systemPackage.export(BIGNUM_TYPE);
    f_systemPackage.export(BOOLEAN_TYPE);
    f_systemPackage.export(CHARACTER_TYPE);
    f_systemPackage.export(COMPLEX_TYPE);
    f_systemPackage.export(CONS_TYPE);
    f_systemPackage.export(DOUBLE_FLOAT_TYPE);
    f_systemPackage.export(FLOAT_TYPE);
    f_systemPackage.export(FUNCTION_TYPE);
    f_systemPackage.export(HASHTABLE_TYPE);
    f_systemPackage.export(INTEGER_TYPE);
    f_systemPackage.export(MACRO_TYPE);
    f_systemPackage.export(NULL_TYPE);
    f_systemPackage.export(NUMBER_TYPE);
    f_systemPackage.export(PACKAGE_TYPE);
    f_systemPackage.export(PATHNAME_TYPE);
    f_systemPackage.export(REAL_TYPE);
    f_systemPackage.export(STREAM_TYPE);
    f_systemPackage.export(STRING_TYPE);
    f_systemPackage.export(SYMBOL_TYPE);
    f_systemPackage.export(VECTOR_TYPE);

  }


/* ------------------  PRIVATE VARIABLES   ------------------------------ */


  LispValue prompt, userPrompt;
  LispValue packages = null;

  LispSymbol STAR, STARSTAR, STARSTARSTAR;
  LispSymbol LOAD_VERBOSE;

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

    // Have to be careful about initializing this...

    f_systemPackage.setNicknames(makeList(makeString("SYS")));
    f_keywordPackage.setNicknames(makeList(makeString("")));

    PACKAGE = new StandardLispPackage(this, makeString("COMMON-LISP-USER"),makeList(makeString("CL-USER"),makeString("USER")),NIL,SYMTAB);
    final LispPackage clPackage = new StandardLispPackage(this, makeString("COMMON-LISP"),makeList(makeString("CL")));
    PACKAGE.setUses(makeList(((StandardLispPackage)clPackage).getName(),((StandardLispPackage)f_systemPackage).getName()));
    ((StandardLispPackage)clPackage).setUses(makeList(((StandardLispPackage)f_systemPackage).getName()));
    ((StandardLispPackage)f_keywordPackage).setUses(NIL);
    ((StandardLispPackage)f_systemPackage).setUses(NIL);

    // Create the rest of the packages
    packages = makeList(f_systemPackage,clPackage,f_keywordPackage,PACKAGE);

    initConstants2();

    COMPILER     = new LispCompiler(this);
    MACHINE      = new SECDMachine(this);
    PARSER       = new LispParser(this, new InputStreamReader(System.in));


    // Need to allow *TOP-LEVEL-PROMPT* to change this.
    prompt = makeString("Jatha> ");

    STAR         = internAndExport("*", f_systemPackage);
    STARSTAR     = internAndExport("**", f_systemPackage);
    STARSTARSTAR = internAndExport("***", f_systemPackage);

    STAR.setf_symbol_value(NIL);
    STARSTAR.setf_symbol_value(NIL);
    STARSTARSTAR.setf_symbol_value(NIL);

    // Defines global variables, etc.  Should only be called once.
    EVAL_init();

    PACKAGE_SYMBOL = intern("*PACKAGE*");
    PACKAGE_SYMBOL.set_special(true);    // 13 Dec 2005 (mh)

    LOAD_VERBOSE = intern("*LOAD-VERBOSE*");
    LOAD_VERBOSE.setf_symbol_value(NIL);

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

    // useful variable management
    STARSTARSTAR.setf_symbol_value(STARSTAR.symbol_value());
    STARSTAR.setf_symbol_value(STAR.symbol_value());
    STAR.setf_symbol_value(value);

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
    if (vars instanceof LispNil)
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
    if (vars instanceof LispNil)
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
    LispValue oldPackageSymbolValue = PACKAGE_SYMBOL.symbol_value();

    // Need to allow *TOP-LEVEL-PROMPT* to change this.
    myprompt = makeString("Jatha " + PACKAGE_SYMBOL.symbol_value().toString() + "> ");

    STAR.setf_symbol_value(NIL);
    STARSTAR.setf_symbol_value(NIL);
    STARSTARSTAR.setf_symbol_value(NIL);

    System.out.println("Run (EXIT) to stop.");

    input = NIL;

    while (true)
    {
      if (oldPackageSymbolValue != PACKAGE_SYMBOL.symbol_value())
      {
        myprompt = makeString("Jatha " + PACKAGE_SYMBOL.symbol_value().toString() + "> ");
        oldPackageSymbolValue = PACKAGE_SYMBOL.symbol_value();
      }

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

        // useful variable management
        STARSTARSTAR.setf_symbol_value(STARSTAR.symbol_value());
        STARSTAR.setf_symbol_value(STAR.symbol_value());
        STAR.setf_symbol_value(value);

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
    boolean verbose = LOAD_VERBOSE.symbol_value() != NIL;
    return load(in, verbose);
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

    LispPackage oldPackage = (LispPackage)PACKAGE_SYMBOL.symbol_value();
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
    } finally {
      PACKAGE_SYMBOL.setf_symbol_value(oldPackage);
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
  public LispValue findPackage(LispValue packageName)
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

    LispValue     pList = packages;
    LispValue     nickNameList;
    LispPackage   pkg;

    while (pList != NIL)
    {
      pkg = (LispPackage)(this.car(pList));

      // Try to match the package name
      if (packageNameStr.equalsIgnoreCase(pkg.getName().getValue()))
        return pkg;

      // Try to match the nicknames
      nickNameList = pkg.getNicknames();
      while (nickNameList != NIL)
      {
        if (packageNameStr.equalsIgnoreCase(((LispString)(this.car(nickNameList))).getValue()))
          return pkg;
        nickNameList = this.cdr(nickNameList);
      }

      // Try the next package.
      pList = this.cdr(pList);
    }

    return NIL;
  }

  public LispValue allPackages() { return packages; }


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
    return PACKAGE;
  }

    /**
     * Creates a package and returns it. If it already exists, a cerror is reported.
     *
     * @param name the name of the package. may be a string or a symbol
     * @param nickNames a list of nicknames. the content must be strings or symbols
     * @param use a list of package names to use. may be strings or symbols.
     * @return Jatha.PACKAGE
     */
    public LispValue makePackage(final LispValue name, final LispValue nickNames, final LispList use) {
        LispValue firstPkg = findPackage(name);
        if(NIL != firstPkg) {
            throw new LispAlreadyDefinedPackageException(((LispString)name.string()).getValue());
        }
        firstPkg = new StandardLispPackage(this, name, nickNames, use);
        packages = makeCons(firstPkg,packages);
        return firstPkg;
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


  //* @author  Micheal S. Hewett    hewett@cs.stanford.edu
  //* @date    Thu Feb 20 12:20:57 1997
  /**
   * Creates a LispNil (the funny symbol/cons that is the LISP NIL).
   * This method does <b>not</b> intern the symbol.
   *
   * @see LispNil
   * @see LispCons
   * @see LispSymbol
   * @see LispValue
   * @return LispSymbol
   */
  public LispNil makeNIL(String symbolName)
  {
    return new StandardLispNIL(this, symbolName);
  }

  public LispNil makeNIL(LispString symbolName)
  {
    return new StandardLispNIL(this, symbolName);
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
   * This is used by f-backquote when expanding a macro.
   */
  public LispValue combineExprs(LispValue left, LispValue right, LispValue expr)
  {
    if (left.basic_constantp() && (right.basic_constantp()))
      return makeList(QUOTE, expr);
    else if (right instanceof LispNil)
      return makeList(LIST, left);
    else if (right instanceof LispCons && !(this.car(right).equal(LIST) instanceof LispNil))
      return makeList(CONS, left, right);
    else
      return expr;  // ??  (mh) 9 Mar 2008.  The previous "if" had a wayward semi-colon at the end, and thus was not working correctly.  I don't really know what should be returned here.
  }


  /**
   * This is used to expand a macro
   */
  public LispValue backquote(LispValue expr)
  {
    if (expr instanceof LispNil)
      return NIL;
    else if (expr instanceof LispAtom || expr instanceof LispNil)
      return makeList(QUOTE, expr);
    else if (car(expr) == COMMA_FN) // !expr.car().eq(COMMA_FN) instanceof LispNil
      return expr.second();
    else if (car(expr) instanceof LispCons && car(car(expr)) == COMMA_ATSIGN_FN)
      return makeList(APPEND, this.car(expr).second(), backquote(this.cdr(expr)));
    else
      return combineExprs(backquote(this.car(expr)), backquote(this.cdr(expr)), expr);
 }
  

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
	void EVAL_init()
	{
	    setf_symbol_value(intern("*"),                         NIL);
	    setf_symbol_value(intern("**"),                        NIL);
	    setf_symbol_value(intern("***"),                       NIL);
	    setf_symbol_value(intern("*LISP-TRACE*"),              NIL);
	    setf_symbol_value(intern("*COMP-NATIVE-FUNCTIONS*"),   NIL);
	    setf_symbol_value(intern("*COMP-SPECIAL-FUNCTIONS*"),  NIL);

	    // Declare *PACKAGE* as a global variable.
	    setf_symbol_value(intern("*PACKAGE*"),                 PACKAGE);
	    intern("*PACKAGE*").set_special(true);
	}
	  public LispSymbol intern(LispString symbolString)
	  {
	    if (COLON.eql(symbolString.basic_elt(0)) != NIL)
	      return intern((LispString)(symbolString.substring(new StandardLispInteger(this, 1))),
	                    KEYWORD);
	    else
	      return intern(symbolString, PACKAGE);
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
	      if (pkg == KEYWORD)
	      {
	        String newString = symbolString.toStringSimple().toUpperCase();
	        // Symbols must be uppercase
	        newSymbol = new StandardLispKeyword(this, new StandardLispString(this, newString));
	      }
	      else
	        newSymbol = makeSymbol(symbolString);

	      return intern(symbolString, newSymbol, pkg);
	    }
	  }

	  // We need this for the startup when we create f_lisp.NIL and LispValue.T.
	  // Actually, LispValue is always a LispSymbol, but because of NIL's strange
	  // properties, we must make the type be LispValue.
	  public LispSymbol intern(LispString symbolString, LispSymbol symbol)
	  {
	    return intern(symbolString, symbol, PACKAGE);
	  }

	  // We need this for the startup when we create f_lisp.NIL and LispValue.T.
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
	  }

	  public LispSymbol intern(String str)
	  {
	    return intern(makeString(str));
	  }

	  public LispSymbol intern(String str, LispPackage pkg)
	  {
	    return intern(makeString(str), pkg);
	  }
	  public LispSymbol internAndExport(String str, LispPackage pkg)
	  {
		  LispSymbol value = intern(makeString(str), pkg);
		  pkg.export(value);
		  return value;
	  }
	  
	  public LispSymbol keywordAndExport(String str)
	  {
		  LispSymbol value = keyword(makeString(str));
		  KEYWORD.export(value);
		  return value;
	  }
	  public LispSymbol keyword(LispString symbolString)
	  {
		  LispSymbol newSymbol;

		  // First, check to see whether one exists already.
	    newSymbol = KEYWORD.getSymbol(symbolString);

	    if (newSymbol == null)    // Already there, don't add it again.
	    {
	        String newString = symbolString.toStringSimple().toUpperCase();
	        // Symbols must be uppercase
	        newSymbol = new StandardLispKeyword(this, new StandardLispString(this, newString));

	        newSymbol.setPackage(KEYWORD);
	        KEYWORD.addSymbol(symbolString, newSymbol);
	        return newSymbol;
	    }
	    return newSymbol;
	  }

	  public LispValue setf_symbol_value(LispValue symbol, LispValue value)
	  {
	    return symbol.setf_symbol_value(value);
	  }
}


