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

import java.io.EOFException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.jatha.dynatype.LispValue;
import org.jatha.dynatype.StandardLispCons;
import org.jatha.dynatype.StandardLispConstant;
import org.jatha.dynatype.StandardLispNIL;
import org.jatha.read.LispParser;

public class Tests extends Object
{
	public static void main(String[] args)
	{
		final String TESTS = "tests";
		for (File filename : new File(Tests.class.getClassLoader().getResource(TESTS).toString().substring(5).toString()).listFiles())
		{
			try {
				Reader resourceReader = new InputStreamReader(
						Tests.class.getClassLoader().getResourceAsStream(TESTS + "/" + filename.getName())
				);
				
				System.out.print("Testing " + filename.getName() + " ... ");
				List<String> errors = new ArrayList<String>();
				
				Lisp lisp = new Lisp();// lisp.eval("(defun restart () `restart)"); // сигнал к перезагрузке интерпретатора
				LispParser cli = new LispParser(lisp, resourceReader);
				while (true) {
					// System.io.printnl();
					try {
						LispValue s = cli.read();
						if (s instanceof StandardLispCons && s.toString().equals("(RESTART)")) {
							lisp = new Lisp();
							cli = new LispParser(lisp, resourceReader);
							continue;
						}
						LispValue r = lisp.eval(s);
						
						if ((r instanceof StandardLispConstant && !r.toString().equals("T"))
						 || (r instanceof StandardLispNIL)
						) {
							errors.add(s.toString() + " -> " + r.toString());
						}
					} catch (EOFException e) {
						break;
					}
				}
				if (errors.size() > 0) {
					System.out.println("FAILED!");
					for (String error : errors)
						System.out.println("    " + error);					
					System.out.println("!FAILED");					
				}
				else
					System.out.println("Ok");
/*	        if (result == T)
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
	        }*/
	      } catch (Exception e) {
	        System.err.println("Tests.main: " + e.getMessage());
	        break;
	      }
	    }
/*
	    if (DEBUG) {
	        System.out.println("Loaded " + fileCounter + " file(s).");
	    }
	  }*/
	}
}