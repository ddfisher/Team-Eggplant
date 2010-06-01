package util.propnet.architecture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlTerm;
import util.logging.GamerLogger;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.statemachine.Role;
import util.statemachine.implementation.propnet.PropNetRole;

/**
 * The PropNet class is designed to represent Propositional Networks.
 * 
 * A propositional network (also known as a "propnet") is a way of representing
 * a game as a logic circuit. States of the game are represented by assignments
 * of TRUE or FALSE to "base" propositions, each of which represents a single
 * fact that can be true about the state of the game. For example, in a game of
 * Tic-Tac-Toe, the fact (cell 1 1 x) indicates that the cell (1,1) has an 'x'
 * in it. That fact would correspond to a base proposition, which would be set
 * to TRUE to indicate that the fact is true in the current state of the game.
 * Likewise, the base corresponding to the fact (cell 1 1 o) would be false,
 * because in that state of the game there isn't an 'o' in the cell (1,1).
 * 
 * A state of the game is uniquely determined by the assignment of truth values
 * to the base propositions in the propositional network. Every assignment of
 * truth values to base propositions corresponds to exactly one unique state of
 * the game.
 * 
 * Given the values of the base propositions, you can use the connections in the
 * network (AND gates, OR gates, NOT gates) to determine the truth values of
 * other propositions. For example, you can determine whether the terminal
 * proposition is true: if that proposition is true, the game is over when it
 * reaches this state. Otherwise, if it is false, the game isn't over. You can
 * also determine the value of the goal propositions, which represent facts like
 * (goal xplayer 100). If that proposition is true, then that fact is true in
 * this state of the game, which means that xplayer has 100 points.
 * 
 * You can also use a propositional network to determine the next state of the
 * game, given the current state and the moves for each player. First, you set
 * the input propositions which correspond to each move to TRUE. Once that has
 * been done, you can determine the truth value of the transitions. Each base
 * proposition has a "transition" component going into it. This transition has
 * the truth value that its base will take on in the next state of the game.
 * 
 * For further information about propositional networks, see:
 * 
 * "Decomposition of Games for Efficient Reasoning" by Eric Schkufza.
 * "Factoring General Games using Propositional Automata" by Evan Cox et al.
 * 
 * @author Sam Schreiber
 */
@SuppressWarnings("serial")
public final class RegularPropNet {

	/** References to every component in the PropNet. */
	private final Set<Component> components;
	/** References to every Proposition in the PropNet. */
	private final Set<Proposition> propositions;
	/** References to every BaseProposition in the PropNet, indexed by name. */
	private final Map<GdlTerm, Proposition> basePropositions;
	/** References to every InputProposition in the PropNet, indexed by name. */
	private final Map<GdlTerm, Proposition> inputPropositions;
	/**
	 * References to every LegalProposition in the PropNet, indexed by player
	 * name.
	 */
	private final Map<Role, Set<Proposition>> legalPropositions;
	/**
	 * References to every GoalProposition in the PropNet, indexed by player
	 * name.
	 */
	private final Map<Role, Set<Proposition>> goalPropositions;
	/** A reference to the single, unique, InitProposition. */
	private final Proposition initProposition;
	/** A reference to the single, unique, TerminalProposition. */
	private final Proposition terminalProposition;

	private final Map<Proposition, Proposition> legalInputMap;

	/** A helper list of all of the roles. */
	private final List<Role> roles;

	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 * 
	 * @param components
	 *            A list of Components.
	 */
	public RegularPropNet(PropNet pnet) {
		this(pnet.getRoles(), pnet.getComponents());
	}
	
	public RegularPropNet(List<Role> roles, Set<Component> components) {
	    this.roles = roles;
		this.components = components;
		this.propositions = recordPropositions();
		this.basePropositions = recordBasePropositions();
		this.inputPropositions = recordInputPropositions();
		this.legalPropositions = recordLegalPropositions();
		this.goalPropositions = recordGoalPropositions();
		this.initProposition = recordInitProposition();
		this.terminalProposition = recordTerminalProposition();
		this.legalInputMap = makeLegalInputMap();
		
		
	}

	public Map<Proposition, Proposition> getLegalInputMap() {
		return legalInputMap;
	}

	private Map<Proposition, Proposition> makeLegalInputMap() {
		Map<Proposition, Proposition> legalInputMap = new HashMap<Proposition, Proposition>();
		for (Proposition inputProp : inputPropositions.values()) {
			List<GdlTerm> inputPropBody = ((GdlFunction) inputProp.getName()).getBody();
			for (Set<Proposition> legalProps : legalPropositions.values()) {
				for (Proposition legalProp : legalProps) {
					List<GdlTerm> legalPropBody = ((GdlFunction) legalProp.getName()).getBody();
					if (legalPropBody.equals(inputPropBody)) {
						legalInputMap.put(inputProp, legalProp);
						legalInputMap.put(legalProp, inputProp);
					}
				}
			}
		}
		return legalInputMap;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every BaseProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlTerm, Proposition> getBasePropositions() {
		return basePropositions;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every Component in the PropNet.
	 */
	public Set<Component> getComponents() {
		return components;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every GoalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getGoalPropositions() {
		return goalPropositions;
	}

	/**
	 * Getter method. A reference to the single, unique, InitProposition.
	 * 
	 * @return
	 */
	public Proposition getInitProposition() {
		return initProposition;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every InputProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlTerm, Proposition> getInputPropositions() {
		return inputPropositions;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every LegalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getLegalPropositions() {
		return legalPropositions;
	}

	/**
	 * Getter method.
	 * 
	 * @return References to every Proposition in the PropNet.
	 */
	public Set<Proposition> getPropositions() {
		return propositions;
	}

	/**
	 * Getter method.
	 * 
	 * @return A reference to the single, unique, TerminalProposition.
	 */
	public Proposition getTerminalProposition() {
		return terminalProposition;
	}

	/**
	 * Builds an index over the BasePropositions in the PropNet.
	 * 
	 * @return An index over the BasePropositions in the PropNet.
	 */
	private Map<GdlTerm, Proposition> recordBasePropositions() {
		Map<GdlTerm, Proposition> basePropositions = new HashMap<GdlTerm, Proposition>();
		for (Proposition proposition : propositions) {
			if (proposition.getInputs().size() == 1) {
				Component component = proposition.getSingleInput();
				if (component instanceof Transition) {
					basePropositions.put(proposition.getName(), proposition);
				}
			}
		}

		return basePropositions;
	}

	/**
	 * Builds an index over the GoalPropositions in the PropNet.
	 * 
	 * @return An index over the GoalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordGoalPropositions() {
		Map<Role, Set<Proposition>> goalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions) {
			if (proposition.getName() instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) proposition.getName();
				if (function.getName().getValue().equals("goal")) {
					GdlConstant name = (GdlConstant) function.get(0);
					GdlProposition prop = (GdlProposition) name.toSentence();
					Role r = new PropNetRole(prop);
					if (!goalPropositions.containsKey(r)) {
						goalPropositions.put(r, new HashSet<Proposition>());
					}
					goalPropositions.get(r).add(proposition);
				}
			}
		}

		return goalPropositions;
	}

	/**
	 * Returns a reference to the single, unique, InitProposition.
	 * 
	 * @return A reference to the single, unique, InitProposition.
	 */
	private Proposition recordInitProposition() {
		for (Proposition proposition : propositions) {
			if (proposition.getName() instanceof GdlConstant) {
				GdlConstant constant = (GdlConstant) proposition.getName();
				if (constant.getValue().equals("INIT")) {
					return proposition;
				}
			}
		}

		return null;
	}

	/**
	 * Builds an index over the InputPropositions in the PropNet.
	 * 
	 * @return An index over the InputPropositions in the PropNet.
	 */
	private Map<GdlTerm, Proposition> recordInputPropositions() {
		Map<GdlTerm, Proposition> inputPropositions = new HashMap<GdlTerm, Proposition>();
		for (Proposition proposition : propositions) {
			if (proposition.getName() instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) proposition.getName();
				if (function.getName().getValue().equals("does")) {
					inputPropositions.put(proposition.getName(), proposition);
				}
			}
		}

		return inputPropositions;
	}

	/**
	 * Builds an index over the LegalPropositions in the PropNet.
	 * 
	 * @return An index over the LegalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordLegalPropositions() {
		Map<Role, Set<Proposition>> legalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions) {
			if (proposition.getName() instanceof GdlFunction) {
				GdlFunction function = (GdlFunction) proposition.getName();
				if (function.getName().getValue().equals("legal")) {
					GdlConstant name = (GdlConstant) function.get(0);
					GdlProposition prop = (GdlProposition) name.toSentence();
					Role r = new PropNetRole(prop);
					if (!legalPropositions.containsKey(r)) {
						legalPropositions.put(r, new HashSet<Proposition>());
					}
					legalPropositions.get(r).add(proposition);

				}
			}
		}

		return legalPropositions;
	}

	/**
	 * Builds an index over the Propositions in the PropNet.
	 * 
	 * @return An index over Propositions in the PropNet.
	 */
	private Set<Proposition> recordPropositions() {
		Set<Proposition> propositions = new HashSet<Proposition>();
		for (Component component : components) {
			if (component instanceof Proposition) {
				propositions.add((Proposition) component);
			}
		}

		return propositions;
	}

	/**
	 * Records a reference to the single, unique, TerminalProposition.
	 * 
	 * @return A reference to the single, unqiue, TerminalProposition.
	 */
	private Proposition recordTerminalProposition() {
		for (Proposition proposition : propositions) {
			if (proposition.getName() instanceof GdlConstant) {
				GdlConstant constant = (GdlConstant) proposition.getName();
				if (constant.getValue().equals("terminal")) {
					return proposition;
				}
			}
		}

		return null;
	}
	/**
	 * Returns a representation of the PropNet in .dot format.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("digraph propNet\n{\n");
		for ( Component component : components )
		{
			sb.append("\t" + component.toString() + "\n");
		}
		sb.append("}");

		return sb.toString();
	}
	/**
     * Outputs the propnet in .dot format to a particular file.
     * This can be viewed with tools like Graphviz and ZGRViewer.
     * 
     * @param filename the name of the file to output to
     */
    public void renderToFile(String filename) {
        try {
            File f = new File(filename);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter fout = new OutputStreamWriter(fos, "UTF-8");
            fout.write(toString());
            fout.close();
            fos.close();
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
        }
    }
}