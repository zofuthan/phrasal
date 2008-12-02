package mt.translationtreebank;

import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.tregex.*;
import edu.stanford.nlp.stats.*;
import java.io.*;
import java.util.*;

public class ExperimentUtils {
  static final int TOPICALITY_SENT_WINDOW_SIZE = 2;

  static TreePattern dec = TreePattern.compile("DEC < 的");
  static TreePattern deg = TreePattern.compile("DEG < 的");
  static TreePattern de = TreePattern.compile("DEG|DEC < 的");
  static TreePattern va1 = TreePattern.compile("CP <, (IP <- (VP <: VA)) <- (DEC < 的)");
  static TreePattern va2 = TreePattern.compile("CP <, (IP <- (VP <, (ADVP $+ (VP <: VA)))) <- (DEC < 的)");
  static TreePattern adjpdeg = TreePattern.compile("DNP <, ADJP <- (DEG < 的)");
  static TreePattern qpdeg = TreePattern.compile("DNP <, QP <- (DEG < 的)");
  static TreePattern nppndeg = TreePattern.compile("DNP <, (NP < PN) <- (DEG < 的)");
  
  static void ReverseSublist(List<String> list, int start, int end) {
    if (start < 0 || start >= list.size() ||
        end < 0 || end >= list.size() ||
        start > end) {
      //System.err.println("Warning: No reverse");
      return;
    }

    while(start < end) {
      Swap(list, start, end);
      start++;
      end--;
    }
  }

  private static void Swap(List<String> list, int p1, int p2) {
    String tmp = list.get(p1);
    list.set(p1, list.get(p2));
    list.set(p2, tmp);
  }

  static Set<String> treeToSetWords(Tree tree) {
    Sentence<Word> sent = tree.yield();
    Set<String> sow = new HashSet<String>();
    for (Word w : sent) {
      sow.add(w.value());
    }
    return sow;
  }

  static Set<String> mergeAllSets(Queue<Set<String>> q) {
    Set<String> sow = new HashSet<String>();
    for (Set<String> set : q) {
      sow.addAll(set);
    }
    return sow;
  }

  static Pair<Integer, Integer> getNPwithDERangeFromIdx(Tree tree, int deIdx) {
    Tree preT = Trees.getPreTerminal(tree, deIdx);
    Tree DNPorCP = preT.parent(tree);
    Tree theNP = DNPorCP.parent(tree);
    int leftE = Trees.leftEdge(theNP, tree);
    int rightE = Trees.rightEdge(theNP, tree)-1;
    Pair<Integer,Integer> range = new Pair<Integer,Integer>(leftE, rightE);
    return range;
  }

  static String getNPwithDE_rootLabel(Tree tree, int deIdx) {
    Tree preT = Trees.getPreTerminal(tree, deIdx);
    Tree DNPorCP = preT.parent(tree);
    Tree theNP = DNPorCP.parent(tree);
    return theNP.label().value();
  }


  static List<Integer> getDEIndices(List<HasWord> sent) {
    List<Integer> des = new ArrayList<Integer>();
    for(int i = 0; i < sent.size(); i++) {
      if (sent.get(i).word().equals("的")) {
        des.add(i);
      }
    }
    return des;
  }

  static List<Integer> getMarkedDEIndices(List<HasWord> sent) {
    List<Integer> des = new ArrayList<Integer>();
    for(int i = 0; i < sent.size(); i++) {
      if (sent.get(i).word().startsWith("的_")) {
        des.add(i);
      }
    }
    return des;
  }

  /**
   * From a given tree, get the DE position by checking immediate children of the tree.
   * if there are multiple ones, the last one will be chosen
   */
  static int getDEIndex(Tree t) {
    Tree[] children = t.children();
    int deIdx = -1;
    for (Tree c : children) {
      Sentence<Word> words = c.yield();
      String lastW = words.get(words.size()-1).word();
      if (lastW.equals("的")) {
        if (deIdx != -1) {
          //System.err.println("multi-DEs: ");
          //t.pennPrint(System.err);
        } else 
          deIdx = Trees.rightEdge(c, t)-1;
      }
    }
    //System.err.println("DEIDX="+deIdx+"\t"+t.toString());
    return deIdx;
  }


  public static Tree maskIrrelevantDEs(Tree tree, int deInTree) {
    Tree newTree = tree.deeperCopy();

    List<Tree> leaves = newTree.getLeaves();

    List<HasWord> words = new ArrayList<HasWord>();
    //for (Tree leaf : leaves) {
    if (!"的".equals(leaves.get(deInTree).value())) {
      newTree.pennPrint(System.err);
      System.err.println("deInTree = "+deInTree);
      System.err.println("leaves.get(deInTree).value()="+leaves.get(deInTree).value());
      throw new RuntimeException("deInTree should be a DE");
    }

    for (int idx = 0; idx < leaves.size(); idx++) {
      //if (idx==deInTree) continue;
      Tree leaf = leaves.get(idx);

      if (idx!=deInTree && "的".equals(leaf.value())) {
        words.add(new Word("X"));
      } else {
        words.add(new Word(leaf.value()));
      }
    }
    
    for (int i = 0; i < leaves.size(); i++) {
      leaves.get(i).setValue(words.get(i).word());
    }
    return newTree;
  }
  
  static boolean hasVApattern(Tree t) {
    TreeMatcher va1M = va1.matcher(t);
    TreeMatcher va2M = va2.matcher(t);
    return (va1M.find() || va2M.find());
  }

  static boolean hasADJPpattern(Tree t) {
    TreeMatcher adjpdegM = adjpdeg.matcher(t);
    return adjpdegM.find();
  }
  
  static boolean hasQPpattern(Tree t) {
    TreeMatcher qpdegM = qpdeg.matcher(t);
    return qpdegM.find();
  }

  static boolean hasNPPNpattern(Tree t) {
    TreeMatcher nppndegM = nppndeg.matcher(t);
    return nppndegM.find();
  }
  
  static boolean hasDEC(Tree npT, Tree wholeT, int deIdx) {
    return hasDE(npT, wholeT, deIdx, "DEC");
  }

  static boolean hasDEG(Tree npT, Tree wholeT, int deIdx) {
    return hasDE(npT, wholeT, deIdx, "DEG");
  }

  private static boolean hasDE(Tree npT, Tree wholeT, int deIdx, String dePat) {
    Sentence<TaggedWord> tws = wholeT.taggedYield();
    TaggedWord tw = tws.get(deIdx);
    if (tw.tag().startsWith("DE")) {
      if (tw.tag().equals(dePat)) return true;
      else return false;
    } else {
      System.err.println(tw + " (" + deIdx + ") in " + tws + " is not a DE");
      return false;
    }
  }

  private static boolean hasDEC(Tree t) {
    TreeMatcher decM = dec.matcher(t);
    return decM.find();
  }

  private static boolean hasDEG(Tree t) {
    TreeMatcher degM = deg.matcher(t);
    return degM.find();
  }

  static int countDE(Tree t) {
    TreeMatcher deM = de.matcher(t);
    int deCount = 0;
    while(deM.find()) {
      deCount++;
    }
    return deCount;
  }

  static List<Pair<String, String>>[] readFinalCategories(String categoryFile, String npFile, String fileidFile, String npidFile) throws IOException{
    String content = StringUtils.slurpFileNoExceptions(categoryFile);
    String[] categories = content.split("\\n");

    content = StringUtils.slurpFileNoExceptions(npFile);
    String[] nps = content.split("\\n");

    content = StringUtils.slurpFileNoExceptions(fileidFile);
    String[] fileids = content.split("\\n");

    content = StringUtils.slurpFileNoExceptions(npidFile);
    String[] npids = content.split("\\n");

    //List<Pair<String, String>>[][] result = new List[326][];
    List<Pair<String, String>>[] result = new List[326];
    int[] maxNP = new int[326];

    if (categories.length != nps.length ||
        nps.length != fileids.length ||
        fileids.length != npids.length)
      throw new RuntimeException("should have 4 equal length files");

    /*
    for(int i = 0; i < categories.length; i++) {
      int fileid = Integer.parseInt(fileids[i]);
      int npid = Integer.parseInt(npids[i]);
      if (maxNP[fileid] < npid) {
        maxNP[fileid] = npid;
      }
    }
    
    for(int i = 1; i <= 325; i++) {
      result[i] = new List[maxNP[i]+1];
      for(int j = 1; j <= maxNP[fileid]; j++) 
        result[i][j] = new ArrayList<Pair<String, String>>();
    }
    */
    for(int i = 1; i <= 325; i++) {
      result[i] = new ArrayList<Pair<String, String>>();
    }

    for(int i = 0; i < categories.length; i++) {
      Pair<String, String> pair = new Pair<String, String>(categories[i], nps[i]);
      int fileid = Integer.parseInt(fileids[i]);
      int npid = Integer.parseInt(npids[i]);
      //result[fileid][npid].add(pair);
      result[fileid].add(pair);
    }
    return result;
  }


  static List<Pair<String, String>>[] readFinalCategories(String allFile) {
    return readFinalCategories(allFile, true);
  }

  static List<Pair<String, String>>[] readFinalCategories(String allFile, Boolean useReducedCategories) {
    String content = StringUtils.slurpFileNoExceptions(allFile);
    String[] lines = content.split("\\n");

    List<Pair<String, String>>[] result = new List[326];

    for(int i = 1; i <= 325; i++) {
      result[i] = new ArrayList<Pair<String, String>>();
    }

    for(int i = 0; i < lines.length; i++) {
      String[] fields = lines[i].split("\\t");
      if (fields.length != 4) {
        throw new RuntimeException("finalCategories_all.txt should have 4 fields: "+lines[i]);
      }
      String fileidStr = fields[0];
      String npidStr = fields[1];
      String categoriesStr = fields[2];
      String npStr = fields[3];

      if (useReducedCategories)
        categoriesStr = normCategory(categoriesStr);
      else
        categoriesStr = categoriesStr;

      Pair<String, String> pair = new Pair<String, String>(categoriesStr, npStr);
      fileidStr = fileidStr.replaceAll("[^\\d]","");
      int fileid = Integer.parseInt(fileidStr);
      int npid = Integer.parseInt(npidStr);
      //result[fileid][npid].add(pair);
      result[fileid].add(pair);
    }
    return result;
  }

  static String normCategory(String cat) {
    if (cat.equals("B of A")) {
      return "B prep A";
    }
    return cat;
  }

  static String etbDir() {
    String ectbdirname = "/u/nlp/scr/data/ldc/LDC2007T02-EnglishChineseTranslationTreebankV1.0/data/pennTB-style-trees/";
    File ectbdir = new File(ectbdirname);
    if (!ectbdir.exists()) {
      ectbdirname = "C:\\cygwin\\home\\Pichuan Chang\\data\\LDC2007T02-EnglishChineseTranslationTreebankV1.0\\data\\pennTB-style-trees\\";
      ectbdir = new File(ectbdirname);
      if (!ectbdir.exists()) {
        throw new RuntimeException("EnglishChineseTranslationTreebankV1.0 doesn't exist in either of the hard-coded locations.");
      }
    }
    return ectbdirname;
  }

  static String ctbDir() {
    String ctbdirname = "/afs/ir/data/linguistic-data/Chinese-Treebank/6/data/utf8/bracketed/";
    File ctbdir = new File(ctbdirname);
    if (!ctbdir.exists()) {
      ctbdirname = "C:\\cygwin\\home\\Pichuan Chang\\data\\CTB6\\data\\utf8\\bracketed\\";
      ctbdir = new File(ctbdirname);
      if (!ctbdir.exists())
        throw new RuntimeException("CTB6.0 doesn't exist in either of the hard-coded locations.");
    }
    return ctbdirname;
  }

  static String chParsedDir() {
    return "chParsed/";
  }

  static String wordAlignmentDir() {
    // For this to run on both NLP machine and my computer
    String dirname = "/u/nlp/scr/data/ldc/LDC2006E93/GALE-Y1Q4/word_alignment/data/chinese/nw/";
    File dir = new File(dirname);
    if (!dir.exists()) {
      dirname = "C:\\cygwin\\home\\Pichuan Chang\\data\\LDC2006E93\\GALE-Y1Q4\\word_alignment\\data\\chinese\\nw\\";
      dir = new File(dirname);
      if (!dir.exists()) {
        throw new RuntimeException("LDC2006E93 doesn't exist in either of the hard-coded locations.");
      }
    }
    return dirname;
  }

    
  static List<TreePair> readAnnotatedTreePairs() throws IOException {
    //return readAnnotatedTreePairs(true, ctbDir());
    //return readAnnotatedTreePairs(true, null);
    return readAnnotatedTreePairs(true, false);
  }

  static List<TreePair> readAnnotatedTreePairs(Boolean useReducedCategories) throws IOException {
    return readAnnotatedTreePairs(useReducedCategories, false);
  }
  //static List<TreePair> readAnnotatedTreePairs(Boolean useReducedCategories, String chParsedDir) throws IOException {
  static List<TreePair> readAnnotatedTreePairs(Boolean useReducedCategories, Boolean useNonOracleTrees) throws IOException {
    String wordalignmentDir = wordAlignmentDir();
    String ctbDir = ctbDir();
    String etbDir = etbDir();

    String chParsedDir = null;
    if (useNonOracleTrees) {
      chParsedDir = "projects/mt/src/mt/translationtreebank/data/ctb_parsed/bracketed/";
    }

    List<TranslationAlignment> alignment_list = new ArrayList<TranslationAlignment>();
    ChineseTreeReader ctr = new ChineseTreeReader();
    EnglishTreeReader etr = new EnglishTreeReader();
    ChineseTreeReader chparsedTR = new ChineseTreeReader();

    List<TreePair> treepairs = new ArrayList<TreePair>();
    int numNPwithDE = 0;

    // Open the hand-annotate file
    //String finalCategoriesFile = "C:\\cygwin\\home\\Pichuan Chang\\javanlp\\projects\\mt\\src\\mt\\translationtreebank\\data\\finalCategories_all.txt";
    String finalCategoriesFile = "projects/mt/src/mt/translationtreebank/data/finalCategories_all.txt";
    List<Pair<String, String>>[] finalCategories = readFinalCategories(finalCategoriesFile, useReducedCategories);
    
    for(int fileidx = 1; fileidx <= 325; fileidx++) {
    //for(int fileidx = 1; fileidx <= 50; fileidx++) {
      // Everytime, restart them so that when we get trees,
      // we won't match tree & sentences in different files.
      alignment_list = new ArrayList<TranslationAlignment>();
      ctr = new ChineseTreeReader();
      etr = new EnglishTreeReader();
      chparsedTR = new ChineseTreeReader();


      // (1) Read alignment files
      String aname = String.format("%schtb_%03d.txt", wordalignmentDir, fileidx);
      File file = new File(aname);
      if (file.exists()) {
        //System.err.println("Processing  "+fileidx);
        alignment_list = TranslationAlignment.readFromFile(file);
      } else {
        //System.err.println("Skip "+fileidx);
        continue;
      }

      // (2) Read Chinese Trees
      String ctbname =
        String.format("%schtb_%04d.fid", ctbDir, fileidx);
      ctr.readMoreTrees(ctbname);

      // (3) Read English Trees
      String ename =
        String.format("%schtb_%03d.mrg.gz", etbDir, fileidx);
      etr.readMoreTrees(ename);

      // (4) Read parsed Chinese Trees
      String chparsedname = null;
      //if (chParsedDir!=null) { 
      if (useNonOracleTrees) {
        chparsedname = String.format("%schtb_%04d.fid", chParsedDir, fileidx);
        //System.err.println("Reading "+chparsedname);
        chparsedTR.readMoreTrees(chparsedname);
        //System.err.println("chparsedTR.size="+chparsedTR.size());
      }

      // (4) Going through entries in (1) and check if they exist in (2)
      // (5) Going through entries in (1) and check if they exist in (3)
      // (6) also, if the tests passed, this is going to the final examples
      int taidx = 1;
      List<TreePair> treepairs_inFile = new ArrayList<TreePair>();
      for (TranslationAlignment ta : alignment_list) {
        List<Tree> chTrees = ctr.getTreesWithWords(ta.source_);
        List<Tree> chParsedTrees = null;
        //if (chParsedDir != null) {
        if (useNonOracleTrees) {
          chParsedTrees = chparsedTR.getTreesWithWords(ta.source_);
          //System.err.println("chParsedTrees.size="+chParsedTrees.size());
        } else {
          //System.err.println("chParsedTrees.null");
        }

        if (chTrees.size() == 0) {
          //System.err.printf("i=%d: Can't find tree in CTB.\n", fileidx);
          continue;
          // skip for now
        } else if (chTrees.size() > 1) {
          throw new RuntimeException("i="+fileidx+": Multiple trees.");
        }
        
        List<Tree> enTrees = etr.getTreesWithWords(ta.translation_);
        if (enTrees.size() == 0) {
          //System.err.printf("i=%d: Can't find tree in PTB.\n", fileidx);
          continue;
          // skip for now
        } else if (enTrees.size() > 1) {
          //System.err.printf("i=%d: Multiple trees.\n", fileidx);
        }
        ta = TranslationAlignment.fixAlignmentGridWithChineseTree(ta, chTrees);
        ta = TranslationAlignment.fixAlignmentGridMergingChinese(ta, chTrees);
        ta = TranslationAlignment.fixAlignmentGridWithEnglishTree(ta, enTrees);
        ta = TranslationAlignment.fixAlignmentGridMergingEnglish(ta, enTrees);
        TranslationAlignment.checkTranslationAlignmentAndEnTrees(ta, enTrees);
        TranslationAlignment.checkTranslationAlignmentAndChTrees(ta, chTrees);
        TreePair tp;
        //if (chParsedDir!=null) {
        if (useNonOracleTrees) {
          tp = new TreePair(ta, enTrees, chTrees, chParsedTrees);
        }
        else 
          tp = new TreePair(ta, enTrees, chTrees, chTrees);
        treepairs_inFile.add(tp);
        numNPwithDE += tp.numNPwithDE();
      }
      // Important: Read the categories of each NPwithDEs
      TreePair.annotateNPwithDEs(finalCategories[fileidx], treepairs_inFile);
      treepairs.addAll(treepairs_inFile);
    }
    System.err.println("Total Treepairs = "+treepairs.size());
    System.err.println("numNPwithDE = "+numNPwithDE);
    return treepairs;
  }

  static void resultSummary(TwoDimensionalCounter<String,String> confusionMatrix) {
    double totalNum = 0;
    double totalDenom = confusionMatrix.totalCount();
    for (String k : confusionMatrix.firstKeySet()) {
      double denom = confusionMatrix.totalCount(k);
      double num = confusionMatrix.getCount(k, k);
      totalNum += num;
      System.out.printf("#[ %s ] = %d |\tAcc:\t%.2f\n", k, (int)denom, 100.0*num/denom);
    }
    System.out.printf("#total = %d |\tAcc:\t%f\n", (int)totalDenom, 100.0*totalNum/totalDenom);    
  }

  public static String coarseCategory(String cat) {
    String normcat;
    if (cat.startsWith("B") || cat.equals("relative clause")) {
      normcat = "swapped";
    } else if (cat.startsWith("A") || cat.equals("no B")) {
      normcat = "ordered";
    } else if (cat.equals("multi-DEs") || cat.equals("other")) {
      normcat = "other";
    } else {
      throw new RuntimeException("Can't find coarse category for " + cat);
    }
    return normcat;
  }

  public static boolean is6class(String cat) {
    if ("no B".equals(cat)) return true;
    return ExperimentUtils.is5class(cat);
  }

  public static boolean is5class(String cat) {
    if ("A 's B".equals(cat) ||
        "A B".equals(cat) ||
        "A prep B".equals(cat) ||
        "B prep A".equals(cat) ||
        "relative clause".equals(cat)) {
      return true;
    }
    if ("no B".equals(cat) ||
        "multi-DEs".equals(cat) ||
        "other".equals(cat)) {
      return false;
    }
    throw new RuntimeException("the category ["+cat+"] is not valid in 'is5class'");
  }

  public static String short5class(String cat) {
    if ("no B".equals(cat)) 
      throw new RuntimeException("the category ["+cat+"] is not valid in 'is5class'");
    return ExperimentUtils.short6class(cat);
  }

  public static String short6class(String cat) {
    if ("A 's B".equals(cat)) return "AsB";
    if ("A B".equals(cat)) return "AB";
    if ("A prep B".equals(cat)) return "AprepB";
    if ("B prep A".equals(cat)) return "BprepA";
    if ("relative clause".equals(cat)) return "relc";
    if ("no B".equals(cat)) return "noB";
    throw new RuntimeException("the category ["+cat+"] is not valid in 'is6class'");
  }
    


  static void resultCoarseSummary(TwoDimensionalCounter<String,String> confusionMatrix) {
    TwoDimensionalCounter<String,String> cc = new TwoDimensionalCounter<String,String> ();
    
    for (Map.Entry<String,ClassicCounter<String>> k : confusionMatrix.entrySet()) {
      String k1 = k.getKey();
      ClassicCounter<String> k2 = k.getValue();
      String normK1 = coarseCategory(k1);
      for (String val : k2) {
        String normval = coarseCategory(val);
        double count = confusionMatrix.getCount(k1, val);
        cc.incrementCount(normK1, normval, count);
      }
    }
    
    resultSummary(cc);
  }

  public static String[] readTrainDevTest(boolean sixclass) {
    String trainDevTestFile;
    if (sixclass)
      trainDevTestFile = "projects/mt/src/mt/translationtreebank/data/TrainDevTest_6class.txt";
    else
      trainDevTestFile = "projects/mt/src/mt/translationtreebank/data/TrainDevTest.txt";
    String content = StringUtils.slurpFileNoExceptions(trainDevTestFile);
    String[] lines = content.split("\\n");
    return lines;
  }
}
