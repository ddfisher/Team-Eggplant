package player.gamer.statemachine.eggplant.misc;

public class Log {
  // Set this string only in Log, to avoid messy code
  private static final String flags = "if";
  
  /*
   * Flag codes:
   *   a = AlphaBeta debug 
   *   e = Endgame book
   *   f = findFarthestLoss (desperate measures) 
   *   i = iterative deepening
   *   o = opening book
   *   p = prop net
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
