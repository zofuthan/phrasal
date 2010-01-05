package edu.stanford.nlp.mt.train;

import bsh.Interpreter;


/**
 * Interpreter providing support for BeanShell Scripting Language.
 * This class basically only imports all packages found in the
 * MT project plus common API packages.
 *
 * @author Michel Galley
 */
public class BshInterpreter extends bsh.Interpreter
{
  private static final long serialVersionUID = 1L;
	
	Interpreter interpreter;

  public BshInterpreter()
  {
    try {
      eval (
        "import java.util.*;"+
        "import java.util.regex.*;"+
        "import java.io.*;"+
        "import edu.stanford.nlp.mt.*;"+
        "import edu.stanford.nlp.mt.base.*;"+
        "import edu.stanford.nlp.mt.train.*;"+
        "import edu.stanford.nlp.mt.tools.*;"+
        "import edu.stanford.nlp.mt.decoder.*;"+
        "import edu.stanford.nlp.mt.decoder.feat.*;"+
        "import edu.stanford.nlp.mt.decoder.efeat.*;"
      );
    } catch (bsh.EvalError e) {
      throw new IllegalArgumentException ("bsh Interpreter error: "+e);
    }
  }
}
