package util.propnet.architecture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
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
public final class BooleanPropNet extends PropNet {
	/** References to every Proposition in the PropNet. */
	private final Proposition[] propIndex;
	/** References to every Proposition in the PropNet. */
	private final Map<Proposition, Integer> propMap;
	/** References to every BaseProposition in the PropNet, indexed by name. */
	private final Map<GdlTerm, Integer> basePropMap;
	/** References to every InputProposition in the PropNet, indexed by name. */
	private final Map<GdlTerm, Integer> inputPropMap;
	/**
	 * References to every LegalProposition in the PropNet, indexed by player
	 * name.
	 */
	private final Map<Role, int[]> legalPropMap;
	/**
	 * A map between legal and input propositions. The map contains mappings in
	 * both directions
	 */
	private final int[] legalInputMap;
	/**
	 * References to every GoalProposition in the PropNet, indexed by player
	 * name.
	 */
	private final Map<Role, int[][]> goalPropMap;
	/** A reference to the single, unique, InitProposition. */
	private final int initIndex;
	/** A reference to the first BaseProposition. */
	private final int basePropStart;
	/** A reference to the first InputProposition. */
	private final int inputPropStart;
	/** A reference to the first InternalProposition. */
	private final int internalPropStart;
	/** A reference to the single, unique, TerminalProposition. */
	private final int terminalIndex;
	

	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 * 
	 * @param components
	 *            A list of Components.
	 */
	public BooleanPropNet(List<Role> roles, Set<Component> components) {
		super(roles, components);
		propMap = new HashMap<Proposition, Integer>();
		basePropMap = new HashMap<GdlTerm, Integer>();
		inputPropMap = new HashMap<GdlTerm, Integer>();
		Set<Proposition> allPropositions = new HashSet<Proposition>();
		List<Proposition> allBasePropositionsList = new LinkedList<Proposition>();
		List<Proposition> allInputPropositionsList = new LinkedList<Proposition>();
		Proposition initProposition = null, terminalProposition = null;

		Map<Role, Set<Proposition>> tempLegalPropositions = new HashMap<Role, Set<Proposition>>();
		Map<Role, Set<Proposition>> tempGoalPropositions = new HashMap<Role, Set<Proposition>>();

		// One pass: count up all the propositions
		for (Component component : components) {
			if (component instanceof Proposition) {
				Proposition proposition = (Proposition) component;
				if (proposition.getInputs().size() != 0) { // over-zealously prune spurious nodes
					allPropositions.add(proposition);
				}
				if (proposition.getInputs().size() == 1) {
					Component input = proposition.getSingleInput();
					if (input instanceof Transition) {
						allBasePropositionsList.add(proposition);
					}
				}
				if (proposition.getName() instanceof GdlConstant) {
					GdlConstant constant = (GdlConstant) proposition.getName();
					if (constant.getValue().equals("INIT")) {
						initProposition = proposition;
					}
					else if (constant.getValue().equals("terminal")) {
						terminalProposition = proposition;
					}
				}
				else if (proposition.getName() instanceof GdlFunction) {
					GdlFunction function = (GdlFunction) proposition.getName();
					if (function.getName().getValue().equals("does")) {
						allInputPropositionsList.add(proposition);
						allPropositions.add(proposition); // exception to spurious nodes
					}
					else if (function.getName().getValue().equals("legal")) {
						GdlConstant name = (GdlConstant) function.get(0);
						GdlProposition prop = (GdlProposition) name.toSentence();
						Role r = new PropNetRole(prop);
						if (!tempLegalPropositions.containsKey(r)) {
							tempLegalPropositions.put(r, new HashSet<Proposition>());
						}
						tempLegalPropositions.get(r).add(proposition);
					}
					else if (function.getName().getValue().equals("goal")) {
						GdlConstant name = (GdlConstant) function.get(0);
						GdlProposition prop = (GdlProposition) name.toSentence();
						Role r = new PropNetRole(prop);
						if (!tempGoalPropositions.containsKey(r)) {
							tempGoalPropositions.put(r, new HashSet<Proposition>());
						}
						tempGoalPropositions.get(r).add(proposition);
					}
				}
			}
		}
		propIndex = new Proposition[1 + allPropositions.size()]; // 1 for init, which is special case
		// Setup init
		int index = 0;
		propIndex[index] = initProposition;
		initIndex = index;
		propMap.put(initProposition, initIndex);
		index++;
		
		// Setup base props
		basePropStart = index;
		for (Proposition baseProp : allBasePropositionsList) {
			propIndex[index] = baseProp;
			propMap.put(baseProp, index);
			basePropMap.put(baseProp.getName(), index);
			allPropositions.remove(baseProp);
			index++;
		}
		
		// Setup input props
		inputPropStart = index;
		for (Proposition inputProp : allInputPropositionsList) {
			propIndex[index] = inputProp;
			propMap.put(inputProp, index);
			inputPropMap.put(inputProp.getName(), index);
			allPropositions.remove(inputProp);
			index++;
		}
		
		// Setup internal props
		internalPropStart = index;
		for (Proposition internalProp : allPropositions) {
			propIndex[index] = internalProp;
			propMap.put(internalProp, index);
			index++;
		}
		
		assert (index == propIndex.length);
		// Initialize terminal
		terminalIndex = propMap.get(terminalProposition);
		
		// Initialize legal propositions
		legalPropMap = new HashMap<Role, int[]>();
		for (Role role : tempLegalPropositions.keySet()) {
			Set<Proposition> legalProps = tempLegalPropositions.get(role);
			int[] legalPropsArray = new int[legalProps.size()];
			index = 0;
			for (Proposition legalProp : legalProps) {
				legalPropsArray[index] = propMap.get(legalProp);
				index++;
			}
			legalPropMap.put(role, legalPropsArray);
		}
		
		// Initialize goal propositions
		goalPropMap = new HashMap<Role, int[][]>();
		for (Role role : tempGoalPropositions.keySet()) {
			Set<Proposition> goalProps = tempGoalPropositions.get(role);
			int[][] goalPropsArray = new int[goalProps.size()][2];
			index = 0;
			for (Proposition goalProp : goalProps) {
				goalPropsArray[index][0] = propMap.get(goalProp);
				GdlRelation relation = (GdlRelation) goalProp.getName().toSentence();
				GdlConstant constant = (GdlConstant) relation.get(1);
				goalPropsArray[index][1] = Integer.parseInt(constant.toString());
				index++;
			}
			goalPropMap.put(role, goalPropsArray);
		}
		
		legalInputMap = new int[propIndex.length];
		Arrays.fill(legalInputMap, -1);
		for (int inputProp = inputPropStart; inputProp < internalPropStart; inputProp++) {
			List<GdlTerm> inputPropBody = ((GdlFunction) propIndex[inputProp].getName()).getBody();
			for (int[] legalProps : legalPropMap.values()) {
				for (int legalPropIndex = 0; legalPropIndex < legalProps.length; legalPropIndex++) {
					List<GdlTerm> legalPropBody = ((GdlFunction) propIndex[legalProps[legalPropIndex]].getName()).getBody();
					if (legalPropBody.equals(inputPropBody)) {
						legalInputMap[legalProps[legalPropIndex]] = inputProp;
						legalInputMap[inputProp] = legalProps[legalPropIndex];
					}
				}
			}
		}
	}

	/**
	 * Getter methods
	 */
	
	public Set<Component> getComponents() {
		return components;
	}
	public Proposition[] getPropIndex() {
		return propIndex;
	}
	public Map<Proposition, Integer> getPropMap() {
		return propMap;
	}
	public Map<GdlTerm, Integer> getBasePropMap() {
		return basePropMap;
	}
	public Map<GdlTerm, Integer> getInputPropMap() {
		return inputPropMap;
	}
	public Map<Role, int[]> getLegalPropMap() {
		return legalPropMap;
	}
	public int[] getLegalInputMap() {
		return legalInputMap;
	}
	public Map<Role, int[][]> getGoalPropMap() {
		return goalPropMap;
	}
	public int getInitIndex() {
		return initIndex;
	}
	public int getBasePropStart() {
		return basePropStart;
	}
	public int getInputPropStart() {
		return inputPropStart;
	}
	public int getInternalPropStart() {
		return internalPropStart;
	}
	public int getTerminalIndex() {
		return terminalIndex;
	}
	
	
	/**
	 * Returns a representation of the PropNet in .dot format.
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("digraph propNet\n{\n");
		for (Component component : components) {
			sb.append("\t" + component.toString() + "\n");
		}
		sb.append("}");

		return sb.toString();
	}

	/**
	 * Outputs the propnet in .dot format to a particular file. This can be
	 * viewed with tools like Graphviz and ZGRViewer.
	 * 
	 * @param filename
	 *            the name of the file to output to
	 */
	public void renderToFile(String filename) {
		try {
			File f = new File(filename);
			FileOutputStream fos = new FileOutputStream(f);
			OutputStreamWriter fout = new OutputStreamWriter(fos, "UTF-8");
			fout.write(toString());
			fout.close();
			fos.close();
		} catch (Exception e) {
			GamerLogger.logStackTrace("StateMachine", e);
		}
	}
}