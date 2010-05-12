package util.propnet.factory;

import java.util.List;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlRule;
import util.logging.GamerLogger;
import util.propnet.architecture.BooleanPropNet;
import util.propnet.factory.converter.PropNetConverter;
import util.propnet.factory.flattener.PropNetFlattener;

/**
 * The PropNetFactory class defines the creation of PropNets from game
 * descriptions.
 */
public final class PropNetFactory
{
	/**
	 * Creates a PropNet from a game description using the following process:
	 * <ol>
	 * <li>Flattens the game description to remove variables.</li>
	 * <li>Converts the flattened description into an equivalent PropNet.</li>
	 * </ol>
	 * 
	 * @param description
	 *            A game description.
	 * @return An equivalent PropNet.
	 */
	public static BooleanPropNet create(List<Gdl> description)
	{
        try {
            PropNetFlattener pf = new PropNetFlattener(description);
            List<GdlRule> flatDescription = pf.flatten();       
            GamerLogger.log("StateMachine", "Converting...");
            return new PropNetConverter().convert(flatDescription);
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
            return null;
        }
	}
}