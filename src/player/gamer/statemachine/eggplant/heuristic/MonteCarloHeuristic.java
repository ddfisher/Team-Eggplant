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

	private int numTrials;
	private int maxDepth;
	private float goalSum;
	private int goalObservations;

	public MonteCarloHeuristic(int trials) {
		this(trials, 50);
	}
	
	public MonteCarloHeuristic(int trials, double avgGoal) {
		this(trials, Integer.MAX_VALUE, avgGoal);
	}
	
	public MonteCarloHeuristic(int trials, int maxDepth, double avgGoal) {
		this.numTrials = trials;
		this.maxDepth = maxDepth;
		this.goalSum = (float) avgGoal;
		this.goalObservations = 1;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long timeout) 
	throws TimeUpException {
		if (machine instanceof BooleanPropNetStateMachine) {
			long sum = ((BooleanPropNetStateMachine)machine).multiMonte(state, numTrials);
			goalSum += sum;
			goalObservations += numTrials;
			int avg = (int) (goalSum / goalObservations);
			return avg;
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

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		//noop
	}
}
