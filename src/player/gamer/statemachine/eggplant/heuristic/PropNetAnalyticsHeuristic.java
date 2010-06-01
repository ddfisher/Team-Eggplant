package player.gamer.statemachine.eggplant.heuristic;

import player.gamer.statemachine.eggplant.misc.Log;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.BooleanMachineState;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;

public class PropNetAnalyticsHeuristic extends WeightedHeuristic {

	private GoalHeuristic goalHeuristic;
	private LatchHeuristic latchHeuristic;
	private GoalHeuristic rootStateGoalHeuristic;
	private int currStateLatchEval;
	private int currStateGoalEval;
	
	public PropNetAnalyticsHeuristic(Heuristic[] heuristics, double[] weights) {
		super(heuristics, weights);
	}

	public PropNetAnalyticsHeuristic(int numPlayers) {
		super(numPlayers);
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		int baselineSum = 0;
		int baselineCount = 0;
		int latchEval = 0, goalEval = 0, rootStateGoalEval = 0;
		if (latchHeuristic != null) {
			if (currStateLatchEval >= 0) { // Goal not already determined
				latchEval = latchHeuristic.eval((BooleanPropNetStateMachine)machine, (BooleanMachineState)state);
				if (latchEval < 0) { // Goal now determined
					return ~latchEval;
				}
				else {
					// Either latchEval == rootStateLatchEval (has not prevented any goals)
					// or latchEval has not; in either case, this becomes the new baselineEval
					baselineSum += latchEval;
					baselineCount++;
				}
			}
		}
		if (goalHeuristic != null) {
			goalEval = goalHeuristic.eval(machine, state, role, alpha, beta, depth, absDepth, timeout);
			rootStateGoalEval = rootStateGoalHeuristic.eval(machine, state, role, alpha, beta, depth, absDepth, timeout);
			baselineSum += goalEval - currStateGoalEval + rootStateGoalEval;
			baselineCount++;
		}
		for (int i = 0; i < heuristics.length; i++){
			baselineSum += heuristics[i].eval(machine, state, role, alpha, beta, depth, absDepth, timeout) * weights[i];
			baselineCount++;
		}
		//Log.println('u', latchEval + " " + goalEval + " " + currStateGoalEval + " " + rootStateGoalEval);
		
		return baselineSum / baselineCount;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth)
			throws MoveDefinitionException {
		if (!(machine instanceof BooleanPropNetStateMachine && state instanceof BooleanMachineState)) {
			latchHeuristic = null;
			goalHeuristic = null;
			rootStateGoalHeuristic = null;
			return;
		}
		BooleanPropNetStateMachine bpnsm = (BooleanPropNetStateMachine)machine;
		if (latchHeuristic == null) {
			latchHeuristic = new LatchHeuristic(bpnsm, bpnsm.getRoleIndices().get(role));
		}
		goalHeuristic = new GoalHeuristic(bpnsm, bpnsm.getRoleIndices().get(role));
		if (rootStateGoalHeuristic == null) {
			rootStateGoalHeuristic = goalHeuristic;
		}
		try {
			currStateLatchEval = latchHeuristic.eval(bpnsm, (BooleanMachineState)state);
			currStateGoalEval = goalHeuristic.eval(bpnsm, state, role, alpha, beta, depth, absDepth, 0);
		} catch (TimeUpException ex) { // Will never happen
			return;
		}		
	}
	
}
