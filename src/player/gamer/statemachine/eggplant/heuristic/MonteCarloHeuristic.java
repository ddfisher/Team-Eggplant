package player.gamer.statemachine.eggplant.heuristic;

import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;

public class MonteCarloHeuristic implements Heuristic {
	private final int MIN_REASONABLE_TRIALS = 10;
	private final float targetTime = 0.03f;
	private final float targetDepth = 10.0f;
	private final int testProbes = 100;
	private int numTrials, targetTrials;
	private int avgGoal;
	private float weight; //currently unused

	public MonteCarloHeuristic(int trials) {
		this(trials, 50);
	}

	public MonteCarloHeuristic(int trials, int avgGoal) {
		this.targetTrials = trials;
		this.avgGoal = avgGoal;
		this.weight = 1;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long timeout)
			throws TimeUpException {
		if (numTrials != 0) {
			if (machine instanceof BooleanPropNetStateMachine) {
				long sum = ((BooleanPropNetStateMachine) machine).multiMonte(state, numTrials);
				return (int) ( (sum + avgGoal) / (numTrials+1));
			}
			int successfulTrials = 0;
			int sum = 0;
			for (int trial = 0; trial < numTrials; trial++) {
				try {
					MachineState currState = state;
					while (!machine.isTerminal(currState)) {
						currState = machine.getRandomNextState(currState);
						// currState = machine.getNextStateDestructively(currState,
						// machine.getRandomJointMove(currState));
					}
					sum += machine.getGoal(currState, role);
					successfulTrials++;
				} catch (GoalDefinitionException ex) {
					ex.printStackTrace();
					continue;
				} catch (MoveDefinitionException ex) {
					ex.printStackTrace();
					continue;
				} catch (TransitionDefinitionException ex) {
					ex.printStackTrace();
					continue;
				}
			}
			if (successfulTrials == 0) {
				return 0;
			}
			return sum / successfulTrials;
		}
		return avgGoal;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		if (machine instanceof BooleanPropNetStateMachine) {
			int totalDepth = 0;
			int[] probeDepth = new int[1];
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < testProbes; i++) {
				((BooleanPropNetStateMachine) machine).monteCarlo(state, probeDepth);
				totalDepth += probeDepth[0];
			}
			long endTime = System.currentTimeMillis();
			float avgDepth = totalDepth / (float) testProbes;
			float avgTime = (endTime - startTime) / (float) testProbes;
			
			numTrials = Math.round(targetTime/avgTime * targetTrials); //TODO: find more intelligent function
			weight = numTrials/avgDepth;
			System.out.println("+++++++++++++++ Average time: " + avgTime + "\tAverage depth: " + avgDepth + "\tNum Trials: " + numTrials);
			if (numTrials < MIN_REASONABLE_TRIALS)
				numTrials = 0;
		} else {
			numTrials = 0;
		}
	}
}
