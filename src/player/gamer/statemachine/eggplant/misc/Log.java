package player.gamer.statemachine.eggplant.misc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

public class Log {
	// Set this string only in Log, to avoid messy code
	private static final String outFlags = "itmqyxlu";
	private static final String logFlags = "itmqyxld";
	

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
	 *   u = Temp debugging
	 *   v = Propagation precomputation
	 *   x = Goal heuristic
	 *   y = Threaded computation of prop nets
	 *   z = Additional code generation info
	 */

	// Initialize flags for more efficiency 
	private static final boolean[] logFlagSet = new boolean[256];
	private static final boolean[] outFlagSet = new boolean[256];
	private static final String logFileName = "log.txt";

	private static PrintStream log;

	static {
		for (int i = 0; i < outFlags.length(); i++) {
			outFlagSet[outFlags.charAt(i)] = true;
		}
		for (int i = 0; i < logFlags.length(); i++) {
			logFlagSet[logFlags.charAt(i)] = true;
		}
		try {
			log = new PrintStream(logFileName);
		} catch (IOException ex) {
			log = System.out;
		}
	}

	public static void print(final char flag, final String str) {
		if (logFlagSet[flag]) {
			log.print("[" + System.currentTimeMillis() + "]: " + str);
		}
		if (outFlagSet[flag]) {
			System.out.print("[" + System.currentTimeMillis() + "]: " + str);
		}
	}

	public static void println(final char flag, final String str) {
		if (logFlagSet[flag]) {
			log.println("[" + System.currentTimeMillis() + "]: " + str);
		}
		if (outFlagSet[flag]) {
			System.out.println("[" + System.currentTimeMillis() + "]: " + str);
		}
	}
}
