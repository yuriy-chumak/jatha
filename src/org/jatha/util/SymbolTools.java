package org.jatha.util;

public class SymbolTools {
	  /** The equivalent of the C function 'strspn'.
	   * Given a string and another string representing a set of characters,
	   * this function scans the string and accepts characters that are
	   * elements of the given set of characters.  It returns the index
	   * of the first element of the string that is not a member of the
	   * set of characters.
	   * For example:
	   *    pos = firstCharNotInSet(0, "hello there, how are you?", "ehlort ");
	   * returns 11.
	   *
	   * If the string does not contain any of the characters in the set,
	   * str.length() is returned.
	   */
	  public static int firstCharNotInSet(int startIndex, String str, String charSet)
	  {
	    int searchIndex = startIndex - 1;  // we add one at the end.
	    int length      = str.length();

	    //    System.out.print("\nSearching \"" + str + "\" for \"" + charSet + "\" from index " + startIndex);
	    try {
	      for (int i = startIndex;
	           ((i < length) && (charSet.indexOf(str.charAt(i)) >= 0));
	           ++i)
	        searchIndex = i;
	    }
	    catch (StringIndexOutOfBoundsException e) {
	      System.err.println("Hey, got a bad string index in 'firstCharNotInSet'!"); 
	    }

	    //    System.out.println("...returning " + searchIndex);
	    return searchIndex + 1;
	  }


}
