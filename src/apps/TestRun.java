package apps;

import java.io.IOException;

import player.GamePlayer;
import player.gamer.statemachine.eggplant.EggplantPrimaryGamer;
import player.gamer.statemachine.eggplant.completesearch.AlphaBetaGamer;
import player.gamer.statemachine.reflex.legal.LegalGamer;
import player.gamer.statemachine.simple.SimpleSearchLightGamer;

public class TestRun {
	private static final int DEFAULT_GAME_PORT = 9147;
	public static void main(String[] args) throws IOException {
		GamePlayer player = new GamePlayer(DEFAULT_GAME_PORT, new EggplantPrimaryGamer());
//		GamePlayer legal1 = new GamePlayer(DEFAULT_GAME_PORT+1, new SimpleSearchLightGamer());
//		GamePlayer legal2 = new GamePlayer(DEFAULT_GAME_PORT+2, new SimpleSearchLightGamer());
		player.start();
//		legal1.start();
//		legal2.start();
		System.out.println("Started Players!");
	}

}
