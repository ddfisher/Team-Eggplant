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
	private double avgGoal;

	public MonteCarloHeuristic(int trials) {
		this(trials, Integer.MAX_VALUE, 50);
	}
	
	public MonteCarloHeuristic(int trials, double avgGoal) {
		this(trials, Integer.MAX_VALUE, avgGoal);
	}
	
	public MonteCarloHeuristic(int trials, int maxDepth, double avgGoal) {
		this.numTrials = trials;
		this.maxDepth = maxDepth;
		this.avgGoal = avgGoal;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long timeout) 
	throws TimeUpException {
		if (machine instanceof BooleanPropNetStateMachine) {
			int sum = 0;
			int successfulTrials = 0;
			for (int i = 0; i < numTrials; i++) {
				BooleanPropNetStateMachine bool = (BooleanPropNetStateMachine)machine;
				MachineState result = bool.monteCarlo(state, maxDepth);
				if (result != null) {
					try {
						sum += bool.getGoal(result, role);
						successfulTrials++;
					} catch (GoalDefinitionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}
				}
			}
			int avg = (int) ((sum + avgGoal) / (successfulTrials + 1));
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
