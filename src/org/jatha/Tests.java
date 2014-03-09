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

public class Tests extends Object
{
	public static void test(String title, String program, String result)
	{
		System.out.println("Testing: " + title);
		
		Jatha lisp = new Jatha();
		String o = lisp.eval(program).toString();
		if (!result.equals(o))
			System.out.print("FAILED!");
	}
	public static void main(String[] args)
	{
		testAccessorFunctions();
	}
	
	
	private static boolean testAccessorFunctions()
	{
		test("FIRST",
				"(first '(1 2 3 4 5 6 7 8 9 10))",
				"1"
				);
		test("SECOND",
				"(second '(1 2 3 4 5 6 7 8 9 10))",
				"2"
				);
		return false;
	}
	
}