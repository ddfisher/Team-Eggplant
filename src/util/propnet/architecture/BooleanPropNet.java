package util.propnet.architecture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import player.gamer.statemachine.eggplant.misc.Log;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlTerm;
import util.logging.GamerLogger;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
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
public final class BooleanPropNet {
	
	/** References to every component in the PropNet. */
	private Set<Component> components;
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
	
	public static final int GOAL_SCALE_FACTOR = 1;

	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 * 
	 * @param components
	 *            A list of Components.
	 */
	public BooleanPropNet(PropNet pnet) {
		this(pnet.getComponents());
	}
	
	public BooleanPropNet(Set<Component> components) {
		this.components = components;
		propMap = new HashMap<Proposition, Integer>();
		basePropMap = new HashMap<GdlTerm, Integer>();
		inputPropMap = new HashMap<GdlTerm, Integer>();
		Set<Proposition> allPropositions = new HashSet<Proposition>();
		List<Proposition> allBasePropositionsList = new LinkedList<Proposition>();
		List<Proposition> allInputPropositionsList = new LinkedList<Proposition>();
		Proposition initProposition = null, terminalProposition = null;
		
		Map<Role, Set<Proposition>> tempLegalPropositions = new HashMap<Role, Set<Proposition>>();
		Map<Role, Set<Proposition>> tempGoalPropositions = new HashMap<Role, Set<Proposition>>();
		
		int numFiltered, totalNumFiltered = 0;
		Set<Component> filteredComponents = new HashSet<Component>(components);

		boolean hasFiltered;
		
		do {
			hasFiltered = false;
			
			
			
			totalNumFiltered += numFiltered = condenseIdenticalOutputs(components, filteredComponents);
			if (numFiltered > 0) {
				Log.println('t', "Filtered " + numFiltered + " identical outputs");
				hasFiltered = true;
				components = new HashSet<Component>(filteredComponents);
			}
			
			totalNumFiltered += numFiltered = condenseSingleAndOr(components, filteredComponents);
			if (numFiltered > 0) {
				Log.println('t', "Filtered " + numFiltered + " single and/or");
				hasFiltered = true;
				components = new HashSet<Component>(filteredComponents);
			}
			
			totalNumFiltered += numFiltered = condenseBaseProps(components, filteredComponents);
			if (numFiltered > 0) {
				Log.println('t', "Filtered " + numFiltered + " base props");
				hasFiltered = true;
				components = new HashSet<Component>(filteredComponents);
			}
			
			totalNumFiltered += numFiltered = condenseConstants(components, filteredComponents);
			if (numFiltered > 0) {
				Log.println('t', "Filtered " + numFiltered + " constants");
				hasFiltered = true;
				components = new HashSet<Component>(filteredComponents);
			}
			
			numFiltered = pruneSpuriousConnections(components, filteredComponents);
			if (numFiltered > 0) {
				Log.println('t', "Pruned " + numFiltered + " connections");
				hasFiltered = true;
				components = new HashSet<Component>(filteredComponents);
			}
			
		} while (hasFiltered);
		
		// Update all components field 
		this.components = filteredComponents;
		
		// One pass: count up all the propositions
		for (Component component : filteredComponents) {
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
					String constantName = ((GdlConstant) proposition.getName()).getValue();
					if (constantName.equals("INIT")) {
						initProposition = proposition;
						allPropositions.add(initProposition);
					}
					else if (constantName.equals("terminal")) {
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

		components = new HashSet<Component>(filteredComponents);
		
		this.components = components = filteredComponents;
		
		propIndex = new Proposition[allPropositions.size()]; // 1 for init, which is special case

		Log.println('t', "Filtered " + totalNumFiltered + " / " + (allPropositions.size() + totalNumFiltered) + " props = " + ( 100.0 * totalNumFiltered / (allPropositions.size() + totalNumFiltered) )  + "%; now " + (allPropositions.size() ) + " props, " + this.components.size() + " components remaining");
		// Setup init
		int index = 0;
		propIndex[index] = initProposition;
		initIndex = index;
		propMap.put(initProposition, initIndex);
		allPropositions.remove(initProposition);
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
		
		// Setup internal props with ordering given by topological sort from terminal
		// Allows for spatial locality optimization in the propgation methods
		internalPropStart = index;
		List<Proposition> preferredOrdering = new LinkedList<Proposition>();
		preferredOrdering.add(terminalProposition);
		preferredOrdering.addAll(allPropositions);
		List<Proposition> ordering = getOrdering(allPropositions, preferredOrdering);
		for (Proposition internalProp : ordering) {
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
			List<Proposition> sortedGoalProps = new ArrayList<Proposition>(goalProps);
			Collections.sort(sortedGoalProps, new Comparator<Proposition>() {
				public int compare(Proposition a, Proposition b) {
					return getGoalValue(a) - getGoalValue(b);
				}
			});
			for (Proposition goalProp : sortedGoalProps) {
				goalPropsArray[index][0] = propMap.get(goalProp);
				goalPropsArray[index][1] = getGoalValue(goalProp);
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
	
	private int getGoalValue(Proposition goalProp) {
		GdlRelation relation = (GdlRelation) goalProp.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return GOAL_SCALE_FACTOR * Integer.parseInt(constant.toString());
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
	
	public List<Proposition> getOrdering(Set<Proposition> propositions, List<Proposition> preferredOrdering) {
		LinkedList<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		HashSet<Component> components = new HashSet<Component>(this.components);
				
		// Remove all base propositions from components 
		for (int i = initIndex; i < inputPropStart; i++) {
			components.remove(propIndex[i]);
		}
		
		// TODO: Optimize this. Not efficient at all
		Iterator<Component> componentIterator = components.iterator();
		while (componentIterator.hasNext()) {
			Component component = componentIterator.next();
			if ((component instanceof Proposition) && component.getInputs().size() < 1) {
				componentIterator.remove();
			}
		}
		
		// Use DFS to compute topological sort of propositions
		
		for (int i = 0; i < preferredOrdering.size(); i++) {
			Proposition currComponent = preferredOrdering.get(i);
			if (propositions.contains(currComponent)) {
				topologicalSort(currComponent, order, propositions, components);
			}
		}
		return order;
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

	public static void topologicalSort(Component currComponent, List<Proposition> order,
			Set<Proposition> propositions, Set<Component> components) {
		for (Component input : currComponent.getInputs()) {
			if (components.contains(input)) { // not yet visited
				topologicalSort(input, order, propositions, components);
			}
		}
		if (currComponent instanceof Proposition) {
			order.add((Proposition) currComponent);
			propositions.remove(currComponent);
		}
		components.remove(currComponent);
	}
	
	private boolean isSpecialNode(Proposition prop) {
		// Make sure we don't remove a special node
		if (prop.getName() instanceof GdlConstant) {
			String constantName = ((GdlConstant) prop.getName()).getValue();
			if (constantName.equals("INIT") || constantName.equals("terminal")) {
				return true;
			}
		}
		else if (prop.getName() instanceof GdlFunction) {
			String functionName = ((GdlFunction) prop.getName()).getName().getValue();
			if (functionName.equals("does") || functionName.equals("legal") || functionName.equals("goal")) {
				return true;
			}
		}
		return false;
	}
	
	private int removeIslands(Component connector, Set<Component> filteredComponents) {
		assert connector.getOutputs().size() == 0;
		int count = 0;
		for (Component prop : connector.getInputs()) {
			prop.getOutputs().remove(connector);
			if (!isSpecialNode((Proposition)prop) && prop.getOutputs().size() == 0) { // continue processing
				filteredComponents.remove(prop);
				count += removeIslands(prop.getSingleInput(), filteredComponents); 
			}
		}
		count++;
		filteredComponents.remove(connector);
		return count;
	}
	
	private int condenseIdenticalOutputs(Set<Component> components, Set<Component> filteredComponents) {
		int numFiltered = 0;
		for (Component component : components) {
			if (component instanceof Proposition) {
				Proposition prop = (Proposition) component;
				Set<Component> outputs = prop.getOutputs();
				List<Component> nots = new ArrayList<Component>();
				List<Component> ands = new ArrayList<Component>();
				List<Component> ors = new ArrayList<Component>();
				
				for (Component output : outputs) {
					if (output instanceof Not) {
						nots.add(output);
					}
					else if (output instanceof And) {
						ands.add(output);
					}
					else if (output instanceof Or) {
						ors.add(output);
					}
				}
				if (nots.size() >= 2) {
					// Combine all Nots
					Component chosenNot = nots.get(0);
					Proposition chosenNotProp = (Proposition) chosenNot.getSingleOutput();
					for (int i = 1; i < nots.size(); i++) {
						Component discardNot = nots.get(i);
						Proposition discardNotProp = (Proposition) discardNot.getSingleOutput();
						
						// Make sure we don't remove a special node
						if (isSpecialNode(discardNotProp)) {
							continue;
						}
						
						// Remove top connection
						outputs.remove(discardNot);
						discardNot.getInputs().remove(prop);
						
						// Shift bottom connection
						for (Component discardNotPropOutput : discardNotProp.getOutputs()) {
							discardNotPropOutput.getInputs().remove(discardNotProp);
							chosenNotProp.addOutput(discardNotPropOutput);
							discardNotPropOutput.addInput(chosenNotProp);
						}
						discardNotProp.getOutputs().clear();
						
						filteredComponents.remove(discardNot);
						filteredComponents.remove(discardNotProp);
						numFiltered++;
					}
				}
				if (ands.size() >= 2) {
					for (int i = 0; i < ands.size(); i++) {
						Set<Component> sameInputAnds = new HashSet<Component>();
						for (int j = i + 1; j < ands.size(); j++) {
							if (ands.get(i).getInputs().equals(ands.get(j).getInputs())) {
								sameInputAnds.add(ands.remove(j));
								j--;
							}
						}
						if (sameInputAnds.size() == 0) {
							continue;
						}
						
						Component chosenAnd = ands.get(i);
						Proposition chosenAndProp = (Proposition) chosenAnd.getSingleOutput();
						
						for (Component discardAnd : sameInputAnds) {
							Proposition discardAndProp = (Proposition) discardAnd.getSingleOutput();
							
							if (isSpecialNode(discardAndProp)) {
								continue; 
							}
							
							// Remove top connections
							for (Component discardAndInput : discardAnd.getInputs()) {
								discardAndInput.getOutputs().remove(discardAnd);
							}
							discardAnd.getInputs().clear();
							
							// Shift bottom connection
							for (Component discardAndPropOutput : discardAndProp.getOutputs()) {
								discardAndPropOutput.getInputs().remove(discardAndProp);
								chosenAndProp.addOutput(discardAndPropOutput);
								discardAndPropOutput.addInput(chosenAndProp);
							}
							discardAndProp.getOutputs().clear();
							
							filteredComponents.remove(discardAnd);
							filteredComponents.remove(discardAndProp);
							numFiltered++;
						}
							
					}
				}
				if (ors.size() >= 2) {
					for (int i = 0; i < ors.size(); i++) {
						Set<Component> sameInputOrs = new HashSet<Component>();
						for (int j = i + 1; j < ors.size(); j++) {
							if (ors.get(i).getInputs().equals(ors.get(j).getInputs())) {
								sameInputOrs.add(ors.remove(j));
								j--;
							}
						}
						if (sameInputOrs.size() == 0) {
							continue;
						}
						
						Component chosenOr = ors.get(i);
						Proposition chosenOrProp = (Proposition) chosenOr.getSingleOutput();
						
						for (Component discardOr : sameInputOrs) {

							Proposition discardOrProp = (Proposition) discardOr.getSingleOutput();
							
							if (isSpecialNode(discardOrProp)) {
								continue; 
							}
							
							// Remove top connections
							for (Component discardOrInput : discardOr.getInputs()) {
								discardOrInput.getOutputs().remove(discardOr);
							}
							discardOr.getInputs().clear();
							
							// Shift bottom connection
							for (Component discardOrPropOutput : discardOrProp.getOutputs()) {
								discardOrPropOutput.getInputs().remove(discardOrProp);
								chosenOrProp.addOutput(discardOrPropOutput);
								discardOrPropOutput.addInput(chosenOrProp);
							}
							discardOrProp.getOutputs().clear();
							
							filteredComponents.remove(discardOr);
							filteredComponents.remove(discardOrProp);
							numFiltered++;
						}
					}
				}
			}
		}
		return numFiltered;
	}
	
	private int condenseSingleAndOr(Set<Component> components, Set<Component> filteredComponents) {
		int numFiltered = 0;
		// First pass: condense all single input, single output Or and And		
loop:	for (Component component : components) {
			if ((component instanceof Or || component instanceof And) &&
					component.getInputs().size() == 1 && component.getOutputs().size() == 1) { 
				Proposition above = (Proposition) component.getSingleInput();
				Proposition below = (Proposition) component.getSingleOutput();
				
				// Make sure below is not a special node

				if (isSpecialNode(below)) {
					continue loop;
				}
				
				// Make sure below does not lead to a transition (only internal nodes can propagate)

				Set<Component> belowOutputs = below.getOutputs();
				for (Component connector : belowOutputs) {
					if (connector instanceof Transition) {
						continue loop;
					}
				}
				
				Log.println('s', "Before filtering #" + numFiltered + ": " + above + " " + below);
				Log.println('s', "Debug #" + numFiltered + ": " + above.getInputs() + " " + above.getOutputs() + " " + below.getOutputs());
				
				// Rewire the connections: all of below's outputs become above's outputs
				Set<Component> aboveOutputs = above.getOutputs();
				Log.println('s', "Filtering #" + numFiltered + ": " + aboveOutputs + "; " + belowOutputs);
				for (Component connector : belowOutputs) {
					Log.println('s', "Processing " + connector);
					aboveOutputs.add(connector);
					connector.addInput(above);
					connector.getInputs().remove(below);
				}
				aboveOutputs.remove(component);
				// At this point, component points to below points to belowOutputs; can be removed
				filteredComponents.remove(component);
				filteredComponents.remove(below);
				Log.println('s', "Filtering #" + numFiltered + ": " + aboveOutputs + "; " + belowOutputs);
				Log.println('s', "After filtering #" + numFiltered + ": " + above.getInputs() + " " + above.getOutputs());
				numFiltered++;
			}
		}
		return numFiltered;
	}
	
	private int condenseBaseProps(Set<Component> components, Set<Component> filteredComponents) {
		// Collapse base propositions if possible
		int numFiltered = 0;
		for (Component component : components) {
			if (component instanceof Proposition) {
				Proposition prop = (Proposition) component;
				if (prop.getInputs().size() == 1 && prop.getSingleInput() instanceof Transition) { // is a base prop
					if (prop.getOutputs().size() == 1 && prop.getSingleOutput() instanceof Or) {
						Component or = prop.getSingleOutput();
						boolean collapsable = true;
						for (Component orInput : or.getInputs()) {
							if (!(orInput.getOutputs().size() == 1 && orInput.getInputs().size() == 1 && orInput.getSingleInput() instanceof Transition)) {
								collapsable = false;
								break;
							}
						}
						if (collapsable) {
							Proposition chosenBaseProp = prop;
							Set<Component> orInputs = new HashSet<Component>(or.getInputs());
							for (Component baseProp : orInputs) {
								if (baseProp == chosenBaseProp) {
									continue;
								}
								// Remove baseProp -> or connection
								baseProp.getOutputs().remove(or);
								or.getInputs().remove(baseProp);

								Component transition = baseProp.getSingleInput();
								Proposition aboveTransitionProp = (Proposition) transition.getSingleInput();

								aboveTransitionProp.getOutputs().remove(transition);
								transition.getInputs().remove(aboveTransitionProp);

								aboveTransitionProp.addOutput(or);
								or.addInput(aboveTransitionProp);

								filteredComponents.remove(transition);
								filteredComponents.remove(baseProp);

								numFiltered++;
							}
							Proposition newBaseProp = (Proposition) or.getSingleOutput();
							Component transition = chosenBaseProp.getSingleInput();
							Proposition aboveTransitionProp = (Proposition) transition.getSingleInput();

							aboveTransitionProp.getOutputs().remove(transition);
							transition.getInputs().remove(aboveTransitionProp);

							aboveTransitionProp.addOutput(or);
							or.addInput(aboveTransitionProp);

							chosenBaseProp.getOutputs().remove(or);
							or.getInputs().remove(chosenBaseProp);

							or.getOutputs().remove(newBaseProp);
							newBaseProp.getInputs().remove(or);

							transition.getOutputs().remove(chosenBaseProp);
							chosenBaseProp.getInputs().remove(transition);

							assert chosenBaseProp.getInputs().size() == 0;
							assert chosenBaseProp.getOutputs().size() == 0;
							assert transition.getInputs().size() == 0;
							assert transition.getOutputs().size() == 0;
							assert or.getOutputs().size() == 0;
							assert newBaseProp.getInputs().size() == 0;

							// Rewire!
							newBaseProp.addInput(transition);
							transition.addOutput(newBaseProp);

							transition.addInput(chosenBaseProp);
							chosenBaseProp.addOutput(transition);

							chosenBaseProp.addInput(or);
							or.addOutput(chosenBaseProp);
						}
					}
				}
			}
		}
		return numFiltered;
	}
	

	private int condenseConstants(Set<Component> components, Set<Component> filteredComponents) {
		// Constant propagation
		int numFiltered = 0;
		
		for (Component component : components) {
			if (component instanceof Constant) {
				boolean constantValue = component.getValue();
				Proposition constantProp = (Proposition) component.getSingleOutput();
				Set<Component> toSever = new HashSet<Component>();
				Set<Component> toTrue = new HashSet<Component>();
				Set<Component> toFalse = new HashSet<Component>();
				for (Component output : constantProp.getOutputs()) {
					if ((output instanceof And || output instanceof Or) && output.getInputs().size() == 1) {
						if (constantValue) {
							toTrue.add(output);
						}
						else {
							toFalse.add(output);
						}
					}
					else if (output instanceof And) {
						if (constantValue) {
							toSever.add(output);
						}
						else {
							toFalse.add(output);
						}
					}
					else if (output instanceof Or) {
						if (constantValue) {
							toTrue.add(output);
						}
						else {
							toSever.add(output);
						}
					}
					else if (output instanceof Not) {
						if (constantValue) {
							toFalse.add(output);
						}
						else {
							toTrue.add(output);
						}
					}
				}
				
				for (Component connectorSevered : toSever) {
					connectorSevered.getInputs().remove(constantProp);
					constantProp.getOutputs().remove(connectorSevered);
				}
				for (Component connectorTrued : toTrue) {
					
					Proposition propTrued = (Proposition) connectorTrued.getSingleOutput();
					
					propTrued.getInputs().remove(connectorTrued);
					connectorTrued.getOutputs().remove(propTrued);
					
					Component trueComponent = new Constant(true);
					trueComponent.addOutput(propTrued);
					propTrued.addInput(trueComponent);
					
					filteredComponents.add(trueComponent);
					
					connectorTrued.getInputs().remove(constantProp);
					constantProp.getOutputs().remove(connectorTrued);
					
					numFiltered += removeIslands(connectorTrued, filteredComponents);
				}
				for (Component connectorFalsed : toFalse) {
					Proposition propFalsed = (Proposition) connectorFalsed.getSingleOutput();
					
					propFalsed.getInputs().remove(connectorFalsed);
					connectorFalsed.getOutputs().remove(propFalsed);
					
					Component falseComponent = new Constant(false);
					falseComponent.addOutput(propFalsed);
					propFalsed.addInput(falseComponent);
					
					filteredComponents.add(falseComponent);
					
					connectorFalsed.getInputs().remove(constantProp);
					constantProp.getOutputs().remove(connectorFalsed);
					
					numFiltered += removeIslands(connectorFalsed, filteredComponents);
				}					
				
				if (!isSpecialNode(constantProp) && constantProp.getOutputs().size() == 0) {
					filteredComponents.remove(constantProp);
					filteredComponents.remove(component);
					numFiltered++;
				}
			}
		}
		return numFiltered;
	}
	
	private int pruneSpuriousConnections(Set<Component> components, Set<Component> filteredComponents) {
		int numPruned = 0;
		for (Component component : components) {
			boolean pruned = false;
			// Check for dangling inputs
			Iterator<Component> inputIterator = component.getInputs().iterator();
			while (inputIterator.hasNext()) {
				Component input = inputIterator.next();
				if (!filteredComponents.contains(input)) {
					inputIterator.remove();
					pruned = true;
				}
			}
			
			// Check for dangling inputs
			Iterator<Component> outputIterator = component.getOutputs().iterator();
			while (outputIterator.hasNext()) {
				Component output = outputIterator.next();
				if (!filteredComponents.contains(output)) {
					outputIterator.remove();
					pruned = true;
				}
			}
			if (pruned) {
				numPruned++;
			}
		}
		return numPruned;
	}
	
	private void sever(Component upstream, Component downstream) {
		upstream.getOutputs().remove(downstream);
		downstream.getInputs().remove(upstream);
	}
}