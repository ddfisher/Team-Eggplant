package player.gamer.statemachine.eggplant.misc;

public class Log {
  // Set this string only in Log, to avoid messy code
  private static final String flags = "itmqyxl";
  
  /*
   * Flag codes:
   *   a = AlphaBeta debug 
   *   b = Propagation precomputation debug
   *   c = Code generation
   *   d = Non-uniform IDS
   *   e = Endgame book
   *   g = Factoring debug
   *   h = Factoring
   *   i = iterative deepening
   *   l = Latch detection
   *   m = Code generation, Monte carlo
   *   o = opening book
   *   p = prop net debug
   *   q = prop net 
   *   r = Rearrangment of ordering in boolean propnet
   *   s = Filtering in boolean propnet debug
   *   t = Filtering
   *   x = Goal heuristic
   *   y = Threaded computation of prop nets
   *   z = Additional code generation info
   */
  
  // Initialize flags for more efficiency 
  private static final boolean[] flagSet = new boolean[256];
  static {
    for (int i = 0; i < flags.length(); i++) {
      flagSet[flags.charAt(i)] = true;
    }
  }
  
  public static void print(final char flag, final String str) {
    if (flagSet[flag]) {
      System.out.print(str);
    }
  }
  
  public static void println(final char flag, final String str) {
    if (flagSet[flag]) {
      System.out.println(str);
    }
  }
}
