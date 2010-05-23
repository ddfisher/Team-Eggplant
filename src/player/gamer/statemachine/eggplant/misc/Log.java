package player.gamer.statemachine.eggplant.misc;

public class Log {
  // Set this string only in Log, to avoid messy code
  private static final String flags = "ifh";
  
  /*
   * Flag codes:
   *   a = AlphaBeta debug 
   *   c = Code generation
   *   e = Endgame book
   *   f = findFarthestLoss (desperate measures) 
   *   g = Factoring debug
   *   h = Factoring
   *   i = iterative deepening
   *   m = Code generation, Monte carlo
   *   o = opening book
   *   p = prop net
   *   r = Rearrangment of ordering in boolean propnet
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
