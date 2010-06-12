package apps.player;

import player.gamer.Gamer;
import java.io.IOException;
import player.proxy.ProxyGamePlayer;
import player.gamer.statemachine.eggplant.EggplantPrimaryGamer;
import player.gamer.statemachine.reflex.random.RandomGamer;

public final class ProxiedPlayerRunner
{
    public static void main(String[] args) throws IOException
    {
        Class<? extends Gamer> toLaunch = EggplantPrimaryGamer.class;
        ProxyGamePlayer player = new ProxyGamePlayer(9147, toLaunch);
        player.start();
    }
}
