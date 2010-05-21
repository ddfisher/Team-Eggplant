package util.statemachine.implementation.propnet;

/* 
 * NOTE: This class is unusable with BooleanPropNet
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import player.gamer.statemachine.eggplant.misc.Log;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.RegularPropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.CachedPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class RegularPropNetStateMachine extends StateMachine {

	private class Factor {
		public Proposition terminalProp;
		public Map<Role, Set<Proposition>> goalProps;
		public Map<Proposition, Proposition> legalInputMap;
		public Set<Proposition> baseProps;
		public Set<Proposition> inputProps;
		public Set<Component> components;
		//assert(everything will be OK) /*of course, the assert fails*/
		public Set<Proposition> internalProps;
		public Factor(Proposition terminalProp) {
			this.terminalProp = terminalProp;
			this.components = new HashSet<Component>();
			this.goalProps = new HashMap<Role, Set<Proposition>>();
			this.internalProps = new HashSet<Proposition>();
			this.baseProps = new HashSet<Proposition>();
			this.inputProps = new HashSet<Proposition>();
			this.legalInputMap = new HashMap<Proposition, Proposition>();
		}
		
		@Override
		public String toString() {
			int numGoals = 0;
			for(Role r : goalProps.keySet()) {
				numGoals += goalProps.get(r).size();
			}
			return "Factor rooted at " + terminalProp.getName() + " with " + components.size() + " components, " + internalProps.size() + " internal props, " + baseProps.size() + " base props, " + inputProps.size() + " input props, " + numGoals + " goal props";
		
		}
	}
	
	// New instance variables 
	private List<Gdl> description;
	
	
	public RegularPropNetStateMachine[] factor() {
		
		Log.println('g', "Starting factoring with " + this.toString());
		/*
		 for (Proposition prop : basePropositions.values()) {
		 
			System.err.println("Base : " + prop.getName());
		}
		for (Proposition prop : inputPropositions.values()) {
			System.err.println("Input : " + prop.getName());
		}
		*/
		

		RegularPropNetStateMachine referenceMachine = new RegularPropNetStateMachine();
		referenceMachine.initialize(description);
		
		
		
		Map<Proposition, List<Proposition>> lowestLevel = new HashMap<Proposition, List<Proposition>>();
		findLowestLevel(referenceMachine.terminal, lowestLevel, new LinkedList<Proposition>());
		
		List<Factor> factors = new ArrayList<Factor>();
		for (Proposition prop : lowestLevel.keySet()) { // tentative root 
			Factor factor = new Factor(prop);
			factors.add(factor);
			
			// Reverse DFS
			reverseDFS(prop, factor, referenceMachine, 0);
		}
		
		// Merge if necessary
		for (int i = 0; i < factors.size(); i++) {
			for (int j = i + 1; j < factors.size(); j++) {
				HashSet<Proposition> copy = new HashSet<Proposition>(factors.get(i).internalProps);
				copy.retainAll(factors.get(j).internalProps);
				if (copy.size() > 0) { // Should merge
					Log.println('g', "Merging yikes!!!!!1");
					
					// Find most recent ancestor
					List<Proposition> trail1 = lowestLevel.get(factors.get(i));
					List<Proposition> trail2 = lowestLevel.get(factors.get(j));
					while (!trail2.contains(trail1.get(0))) {
						trail1.remove(0);
					}
					Proposition ancestor = trail1.get(0);
					Factor newFactor = new Factor(ancestor);
					
					// Find all factors that have this as an ancestor
					for (int k = 0; k < factors.size(); k++) {
						if (lowestLevel.get(factors.get(k)).contains(ancestor)) {
							newFactor.internalProps.addAll(factors.get(k).internalProps);
							newFactor.components.addAll(factors.get(k).components);
							lowestLevel.remove(factors.get(k));
							factors.remove(k);
							k--;
						}
					}
					trail1.remove(0);
					lowestLevel.put(ancestor, trail1); // Throw this back into the pool of factors
					
					// Hacky way to reset loop
					i = -1;
					break;
				}
			}
		}
		
		// Sanity check: assert that all base and internal propositions in each factor are exclusive
		for (int i = 0; i < factors.size(); i++) {
			for (int j = i + 1; j < factors.size(); j++) {
				HashSet<Proposition> copy = new HashSet<Proposition>(factors.get(i).internalProps);
				copy.retainAll(factors.get(j).internalProps);
				assert(copy.size() == 0);
				copy = new HashSet<Proposition>(factors.get(i).baseProps);
				copy.retainAll(factors.get(j).baseProps);
				assert(copy.size() == 0);
			}
		}
		
		// Set name of each factor's root to terminal 
		for (Factor factor : factors) {
			factor.terminalProp.setName(referenceMachine.terminal.getName());
		}
		
		// TODO By assumption, goals are disjunctively factorable
		
		// Add goals
		for (Role role : referenceMachine.goalPropositions.keySet()) {
			for (Proposition goalProp : referenceMachine.goalPropositions.get(role)) {
				addGoals(goalProp, goalProp, factors, role, new LinkedList<Component>());
			}
		}
		
		// Add legals
		for (Factor factor : factors) {
			for (Proposition inputProp : factor.inputProps) {
				// Find legal corresponding to each goal
				Proposition legalProp = referenceMachine.legalInputMap.get(inputProp);
				reverseDFS(legalProp, factor, referenceMachine, 0);
				factor.legalInputMap.put(legalProp, inputProp);
				factor.legalInputMap.put(inputProp, legalProp);
			}
		}
		
		// TODO Input consolidation
		for (int i = 0; i < factors.size(); i++) {
			Log.println('g', "Factor " + i + " has " + factors.get(i).inputProps.size() + " inputs");
			factors.get(i).legalInputMap = referenceMachine.legalInputMap;
		}
		
		// TODO Init hack around
		
		Log.println('g', "Found factors : " + factors);
		
		Set<Component> alreadyVisited = new HashSet<Component>();
		List<Component> toVisit = new LinkedList<Component>();
		// BFS
		int depth = 0;
		toVisit.add(referenceMachine.init);
		while (true) {
			List<Component> willVisit = new LinkedList<Component>();
			while (!toVisit.isEmpty()) {
				Component curr = toVisit.remove(0);	
				int factor = -1;
				for (int i = 0; i < factors.size(); i++) {
					if (factors.get(i).components.contains(curr)) {
						factor = i;
						break;
					}
				}

				if (!alreadyVisited.contains(curr)) {
					Log.println('g', String.format("%" + (depth + 1) + "s :%2d:" + (curr instanceof Proposition ? ((Proposition) curr).getName() : curr.getClass().getName()) + curr.hashCode() + " with " + curr.getInputs().size() + " inputs, " + curr.getOutputs().size() + " outputs", " ", factor));
				
					alreadyVisited.add(curr);
					for (Component next : curr.getOutputs()) {
						willVisit.add(next);
					}
				}
				else {
					Log.println('g', String.format("%" + (depth + 1) + "s :%2d: visited " + (curr instanceof Proposition ? ((Proposition) curr).getName() : curr.getClass().getName()) + curr.hashCode() + " with " + curr.getInputs().size() + " inputs, " + curr.getOutputs().size() + " outputs", " ", factor));
				}
			}
			if (willVisit.isEmpty()) {
				break;
			}
			toVisit = willVisit;
			depth++;
		}
		
		// Add entire init subtree to all factors
		for (Factor factor : factors) {
			factor.components.addAll(alreadyVisited);
			for (Component comp : alreadyVisited) {
				if (comp instanceof Proposition) {
					factor.internalProps.add((Proposition) comp);
				}
			}
		}
		
		alreadyVisited = new HashSet<Component>();
		toVisit = new LinkedList<Component>();
		// BFS
		depth = 0;
		toVisit.add(referenceMachine.terminal);
		while (true) {
			List<Component> willVisit = new LinkedList<Component>();
			while (!toVisit.isEmpty()) {
				Component curr = toVisit.remove(0);	
				
				int factor = -1;
				for (int i = 0; i < factors.size(); i++) {
					if (factors.get(i).components.contains(curr)) {
						factor = i;
						break;
					}
				}
				
				Log.println('g', String.format("%" + (depth + 1) + "s :%2d:" + (curr instanceof Proposition ? ((Proposition) curr).getName() : curr.getClass().getName()) + " with " + curr.getInputs().size() + " inputs, " + curr.getOutputs().size() + " outputs", " ", factor));
				
				alreadyVisited.add(curr);
				for (Component next : curr.getInputs()) {
					if (!alreadyVisited.contains(next)) {
						willVisit.add(next);
					}
				}
			}
			if (willVisit.isEmpty()) {
				break;
			}
			toVisit = willVisit;
			depth++;
		}
		// Generate the new statemachines
		RegularPropNetStateMachine[] minions = new RegularPropNetStateMachine[factors.size()];
		for (int i = 0; i < factors.size(); i++) {
			minions[i] = new RegularPropNetStateMachine();
			minions[i].initialize(factors.get(i).components, roles);
			minions[i].pnet.renderToFile("D:\\Code\\Stanford\\cs227b_svn\\logs\\test" + i + ".out");			
			Log.println('g', minions[i].toString());
			MachineState initialState = minions[i].getInitialState();
			Log.println('g', "Initial state " + initialState);
		}
		
		return minions;
	}
	
	// Has the potential to search the entire supertree of goalProp
	private void addGoals(Proposition prop, Proposition goalProp, List<Factor> factors, Role role, List<Component> trail) {
		Log.println('g', "exploring " + prop.getName() + " with goal " + goalProp.getName());
		for (Factor factor : factors) {
			if (factor.internalProps.contains(prop)) { // Proposition intersects factored tree; should only happen once
				Log.println('g', "Found goal " + prop.getName());
				if (!factor.goalProps.containsKey(role)) {
					factor.goalProps.put(role, new HashSet<Proposition>());
				}
				factor.goalProps.get(role).add(goalProp);
				for (Component c : trail) {
					if (c instanceof Proposition) {
						factor.internalProps.add((Proposition) c);
					}
					factor.components.add(c);
				}
				return; // Only one factor will contain prop
			}
		}
		if (prop.getInputs().size() == 0) {
			return;
		}
		Component comp = prop.getSingleInput();
		for (Component higherProp : comp.getInputs()) {
			List<Component> trailCopy = new LinkedList<Component>(trail);
			trailCopy.add(prop);
			trailCopy.add(comp);
			addGoals((Proposition) higherProp, goalProp, factors, role, trailCopy);
		}
	}
	
	private void reverseDFS(Proposition prop, Factor factor, RegularPropNetStateMachine referenceMachine, int depth) {
		if (referenceMachine.inputPropositions.containsValue(prop)) {
			Log.println('g', String.format("%" + (depth + 1) + "s%s", " ", "Reached " + prop.getName() + prop.hashCode() + " " + prop.getInputs().size() + " inputs, " + prop.getOutputs().size() + " outputs, " + " input"));
			factor.inputProps.add(prop);
			factor.components.add(prop);
			return;
		}	
		boolean continueSearching = prop.getInputs().size() == 1;
		if (referenceMachine.basePropositions.containsValue(prop)) {
			if (factor.baseProps.add(prop)) { 
				factor.components.add(prop);
			}
			else {
				continueSearching = false;
			}
		}
		else if(!factor.internalProps.contains(prop)) {
			int numMatches = 0;
			for (Proposition p : factor.internalProps) {
				if (p.hashCode() == prop.hashCode()) {
					numMatches++;
				}
			}
			if (numMatches != 0) {
				System.err.println("Problem ");
			}
			factor.internalProps.add(prop);
			factor.components.add(prop);
		}
		else {
			continueSearching = false;
		}
		Log.println('g', String.format("%" + (depth + 1) + "s%s", " ", "Reached " + prop.getName() + prop.hashCode() + " " + prop.getInputs().size() + " inputs, " + prop.getOutputs().size() + " outputs, " + (continueSearching ? "" : " stopping")));
		if (continueSearching) { // Not a spurious proposition
			Component comp = prop.getSingleInput();
			factor.components.add(comp);
			Log.println('g', String.format("%" + (depth + 1) + "s%s", " ", "Expanding " + comp.getClass().getName() + comp.hashCode() + " " + comp.getInputs().size() + " inputs"));
			for (Component input : comp.getInputs()) {
				reverseDFS((Proposition)input, factor, referenceMachine, depth + 1);
			}
		}

	}
	
	private void findLowestLevel(Proposition prop, Map<Proposition, List<Proposition>> level, LinkedList<Proposition> trail) {
		Component comp = prop.getSingleInput();
		if (comp instanceof And) {
			if (comp.getInputs().size() == 1) {
				trail.add(prop);
				findLowestLevel((Proposition)comp.getSingleInput(), level, trail);
				trail.remove(prop);
			} else {
				level.put(prop, new LinkedList<Proposition>(trail));
			}
		} else if (comp instanceof Or) {
			for (Component input : comp.getInputs()) {
				trail.add(prop);
				findLowestLevel((Proposition)input, level, trail);
				trail.remove(prop);
			}
		} else {
			level.put(prop, new LinkedList<Proposition>(trail));
			
		}
	}
	
	
	
	/**
	 * Computes if the state is terminal. Should return the value of the
	 * terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		initBasePropositionsFromState(state);
		propagate();
		if (terminal.getValue()) {
			Log.println('p', "Terminal detected in " + state);
		}
		return terminal.getValue();
	}

	/**
	 * Computes the goal for a role in the current state. Should return the
	 * value of the goal proposition that is true for role. If the number of
	 * goals that are true for role != 1, then you should throw a
	 * GoalDefinitionException
	 */
	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		initBasePropositionsFromState(state);
		Set<Proposition> goals = goalPropositions.get(role);

		boolean goalFound = false;
		int goalValue = -1;
		for (Proposition goal : goals) {
			if (goal.getValue()) {
				if (goalFound) {
					throw new GoalDefinitionException(state, role);
				} else {
					Log.println('p', "Goal found: " + goal.getName().toSentence().get(1));
					goalValue = getGoalValue(goal);
					goalFound = true;
				}
			}
		}

		return goalValue;
	}

	/**
	 * Returns the initial state. The initial state can be computed by only
	 * setting the truth value of the init proposition to true, and then
	 * computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		try {
			HashSet<GdlSentence> trueSentences = new HashSet<GdlSentence>();

			initBasePropositionsFromState(getMachineStateFromSentenceList(new HashSet<GdlSentence>()));

			init.setValue(true);
			propagate();
			init.setValue(false);
			for (GdlTerm propName : basePropositions.keySet()) {
				if (basePropositions.get(propName).getValue()) {
					trueSentences.add(propName.toSentence());
				}
			}
			for (GdlTerm propName : basePropositions.keySet()) {
				if (basePropositions.get(propName).getValue()) {
					trueSentences.add(propName.toSentence());
				}
			}
			MachineState newState = getMachineStateFromSentenceList(trueSentences);
			Log.println('p', "Init with " + newState);
			return newState;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Computes the legal moves for role in state
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
		try {
			List<Move> legalMoves = new LinkedList<Move>();

			Set<Proposition> legals = legalPropositions.get(role);

			// Clear initial moves
			for (Proposition legal : legals) {
				legalInputMap.get(legal).setValue(false);
			}
			initBasePropositionsFromState(state);
			for (Proposition legal : legals) {
				Proposition input = legalInputMap.get(legal);
				input.setValue(true);
				propagateInternalOnly();
				if (legal.getValue()) {
					legalMoves.add(getMoveFromProposition(input));
				}
				input.setValue(false);
			}
			Log.println('p', "Legal moves in " + state + " for " + role + " = " + legalMoves);
			return legalMoves;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		try {
			// Set up the base propositions
			initBasePropositionsFromState(state);

			// Set up the input propositions
			List<GdlTerm> doeses = toDoes(moves);

			for (Proposition anyInput : inputPropositions.values()) {
				anyInput.setValue(false);
			}

			for (GdlTerm does : doeses) {
				Proposition trueInput = inputPropositions.get(does);
				trueInput.setValue(true);
			}

			propagate();
			HashSet<GdlSentence> trueSentences = new HashSet<GdlSentence>();
			for (Proposition prop : basePropositions.values()) {
				if (prop.getValue()) {
					trueSentences.add(prop.getName().toSentence());
				}
			}
			MachineState newState = getMachineStateFromSentenceList(trueSentences);
			Log.println('p', "From " + state + " to " + newState + " via " + moves);
			return newState;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private void initBasePropositionsFromState(MachineState state) {
		// Set up the base propositions
		Set<GdlSentence> initialTrueSentences = state.getContents();
		for (GdlTerm propName : basePropositions.keySet()) {
			basePropositions.get(propName).setValue(
					initialTrueSentences.contains(propName.toSentence()));
		}
		/*
		 * Log.print('p', "Base propositions: ["); for (GdlTerm propName :
		 * basePropositions.keySet()) { Log.print('p', propName + " : " +
		 * basePropositions.get(propName).getValue() + ", "); } Log.println('p',
		 * "]");
		 */
	}
	private void propagateInternalOnly() {
		// All the input propositions are set, update all propositions in order
		for (Proposition prop : ordering) {
			prop.setValue(prop.getSingleInput().getValue());
		}
	}

	private void propagate() {
		propagateInternalOnly();

		// All the internal propositions are updated, update all base
		// propositions
		for (Proposition baseProposition : basePropositions.values()) {
			baseProposition.setValue(baseProposition.getSingleInput().getValue());
		}
	}

	/**
	 * This should compute the topological ordering of propositions. Each
	 * component is either a proposition, logical gate, or transition. Logical
	 * gates and transitions only have propositions as inputs. The base
	 * propositions and input propositions should always be exempt from this
	 * ordering The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from the
	 * Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be
	 *         set.
	 */
	public List<Proposition> getOrdering() {
		LinkedList<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		HashSet<Component> components = new HashSet<Component>(pnet.getComponents());
		// All of the propositions in the prop net
		HashSet<Proposition> propositions = new HashSet<Proposition>(pnet.getPropositions());
		Log.println('p', "Components: " + components);

		// Remove all base propositions
		Collection<Proposition> basePropositionsValues = basePropositions.values();
		propositions.removeAll(basePropositionsValues);
		components.removeAll(basePropositionsValues);
		
		// Remove all propositions with no inputs (covers init, input, and spurious)
		Iterator<Proposition> iterator = propositions.iterator();
		while (iterator.hasNext()) {
			Proposition prop = iterator.next();
			if (prop.getInputs().size() == 0) {
				iterator.remove();
				components.remove(prop);
			}
		}

		// Use DFS to compute topological sort of propositions
		while (!propositions.isEmpty()) {
			Proposition currComponent = propositions.iterator().next();
			topologicalSort(currComponent, order, propositions, components);
		}
		Log.println('p', "Order: " + order);
		return order;
	}

	private void topologicalSort(Component currComponent, LinkedList<Proposition> order,
			HashSet<Proposition> propositions, HashSet<Component> components) {
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

	/** Already implemented for you */
	@Override
	public Move getMoveFromSentence(GdlSentence sentence) {
		return new PropNetMove(sentence);
	}

	/** Already implemented for you */
	@Override
	public MachineState getMachineStateFromSentenceList(Set<GdlSentence> sentenceList) {
		return new PropNetMachineState(sentenceList);
	}

	/** Already implemented for you */
	@Override
	public Role getRoleFromProp(GdlProposition proposition) {
		return new PropNetRole(proposition);
	}

	/** Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/** The underlying proposition network */
	private RegularPropNet pnet;
	/**
	 * An index from GdlTerms to Base Propositions. The truth value of base
	 * propositions determines the state
	 */
	private Map<GdlTerm, Proposition> basePropositions;
	/**
	 * An index from GdlTerms to Input Propositions. Input propositions
	 * correspond to moves a player can take
	 */
	private Map<GdlTerm, Proposition> inputPropositions;
	/**
	 * The terminal proposition. If the terminal proposition's value is true,
	 * the game is over
	 */
	private Proposition terminal;
	/** Maps roles to their legal propositions */
	private Map<Role, Set<Proposition>> legalPropositions;
	/** Maps roles to their goal propositions */
	private Map<Role, Set<Proposition>> goalPropositions;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	/**
	 * Set to true and everything else false, then propagate the truth values to
	 * compute the initial state
	 */
	private Proposition init;
	/** The roles of different players in the game */
	private List<Role> roles;
	/**
	 * A map between legal and input propositions. The map contains mappings in
	 * both directions
	 */
	private Map<Proposition, Proposition> legalInputMap;

	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you can compute the initial state here, if
	 * you want.
	 */
	
	@Override
	public void initialize(List<Gdl> desc) {
		description = desc;
		pnet = (RegularPropNet) CachedPropNetFactory.create(description);
		roles = computeRoles(description);
		initializeFromPropNet(pnet);
		pnet.renderToFile("D:\\Code\\Stanford\\cs227b_svn\\logs\\test.out");
	}
	
	public void initialize(Set<Component> components, List<Role> roles) {
		description = null;
		pnet = new RegularPropNet(roles, components);
		this.roles = roles;
		initializeFromPropNet(pnet);
	}
	
	private void initializeFromPropNet(RegularPropNet pnet) {
		basePropositions = pnet.getBasePropositions();
		inputPropositions = pnet.getInputPropositions();
		terminal = pnet.getTerminalProposition();
		legalPropositions = pnet.getLegalPropositions();
		init = pnet.getInitProposition();
		goalPropositions = pnet.getGoalPropositions();
		legalInputMap = pnet.getLegalInputMap();		
		ordering = getOrdering();
	}

	/* Helper methods */
	/**
	 * The Input propositions are indexed by (does ?player ?action) This
	 * translates a List of Moves (backed by a sentence that is simply ?action)
	 * to GdlTerms that can be used to get Propositions from inputPropositions
	 * and accordingly set their values etc. This is a naive implementation when
	 * coupled with setting input values, feel free to change this for a more
	 * efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves) {
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++) {
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding
	 * Move
	 * 
	 * @param p
	 * @return a PropNetMove
	 */

	public static Move getMoveFromProposition(Proposition p) {
		return new PropNetMove(p.getName().toSentence().get(1).toSentence());
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * 
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */

	private int getGoalValue(Proposition goalProposition) {
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState from the true
	 * BasePropositions. This is correct but slower than more advanced
	 * implementations You need not use this method!
	 * 
	 * @return PropNetMachineState
	 */

	public PropNetMachineState getStateFromBase() {
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : basePropositions.values()) {
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue()) {
				contents.add(p.getName().toSentence());
			}

		}
		return new PropNetMachineState(contents);
	}

	/**
	 * Helper method, used to get compute roles. You should only be using this
	 * for Role indexing (because of compatibility with the GameServer state
	 * machine's roles)
	 * 
	 * @param description
	 * @return the list of roles for the current game. Compatible with the
	 *         GameServer's state machine.
	 */
	private List<Role> computeRoles(List<Gdl> description) {
		List<Role> roles = new ArrayList<Role>();
		for (Gdl gdl : description) {
			if (gdl instanceof GdlRelation) {
				GdlRelation relation = (GdlRelation) gdl;
				if (relation.getName().getValue().equals("role")) {
					roles.add(new PropNetRole((GdlProposition) relation.get(0).toSentence()));
				}
			}
		}
		return roles;
	}
	
	@Override
	public String toString() {
		int numGoals = 0;
		for(Role r : goalPropositions.keySet()) {
			numGoals += goalPropositions.get(r).size();
		}
		return "RPNSM with " + pnet.getComponents().size() + " components, " + basePropositions.values().size() + " base, " + inputPropositions.values().size() + " input, " + (legalInputMap == null ? "null" : legalInputMap.keySet().size()/2) + " legals, " + numGoals + " goals, " + (terminal == null ? "null" : terminal.getName()) + " term, " + (init == null ? "null" : init.getName()) + " init "; 
	}
}
