package util.propnet.factory;

import java.util.List;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlRule;
import util.logging.GamerLogger;
import util.propnet.architecture.PropNet;
import util.propnet.factory.converter.PropNetConverter;
import util.propnet.factory.flattener.PropNetAnnotatedFlattener;
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
	public static PropNet create(List<Gdl> description)
	{
        try {
        	PropNetAnnotatedFlattener annotated = new PropNetAnnotatedFlattener(description);
        	List<GdlRule> flatDescription;
        	if (annotated.noAnnotations()) {
        		flatDescription = new PropNetFlattener(description).flatten();
        	}
        	else {
        		flatDescription = annotated.flatten();
        	}
            GamerLogger.log("StateMachine", "Converting...");
            return new PropNetConverter().convert(flatDescription);
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
            return null;
        }
	}
}