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
	private int currStateLatchEval;
	private int minGoal;
	private int maxGoal;
	
	public PropNetAnalyticsHeuristic(int minGoal, int maxGoal, Heuristic[] heuristics, double[] weights) {
		super(heuristics, weights);
		this.minGoal = minGoal;
		this.maxGoal = maxGoal;
	}

	public PropNetAnalyticsHeuristic(int minGoal, int maxGoal, int numPlayers) {
		super(numPlayers);
		this.minGoal = minGoal;
		this.maxGoal = maxGoal;
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		double baselineSum = 0;
		double baselineCount = 0;
		int latchEval = 0, goalEval = 0;
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
			baselineSum += goalEval;
			baselineCount++;
		}
		for (int i = 0; i < heuristics.length; i++){
			baselineSum += heuristics[i].eval(machine, state, role, alpha, beta, depth, absDepth, timeout) * weights[i];
			baselineCount += weights[i];
		}
		//Log.println('u', latchEval + " " + goalEval + " " + currStateGoalEval + " " + rootStateGoalEval);
		int ret = (int) (baselineSum / baselineCount);
		if (ret >= maxGoal) {
			ret = maxGoal - 1;
		}
		else if (ret <= minGoal) {
			ret = minGoal + 1;
		}
		return ret;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth)
			throws MoveDefinitionException {
		if (!(machine instanceof BooleanPropNetStateMachine && state instanceof BooleanMachineState)) {
			latchHeuristic = null;
			goalHeuristic = null;
		}
		else {
			BooleanPropNetStateMachine bpnsm = (BooleanPropNetStateMachine)machine;
			if (latchHeuristic == null) {
				latchHeuristic = new LatchHeuristic(bpnsm, bpnsm.getRoleIndices().get(role));
			}
			goalHeuristic = new GoalHeuristic(bpnsm, bpnsm.getRoleIndices().get(role));
			currStateLatchEval = latchHeuristic.eval(bpnsm, (BooleanMachineState)state);
		}
		for (Heuristic heuristic : super.heuristics) {
			heuristic.update(machine, state, role, alpha, beta, depth, absDepth);
		}
	}
	
}
