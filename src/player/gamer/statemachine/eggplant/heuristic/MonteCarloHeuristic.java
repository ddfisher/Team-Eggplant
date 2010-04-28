package player.gamer.statemachine.eggplant.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloHeuristic implements Heuristic {

	private int numTrials;

	public MonteCarloHeuristic(int trials) {
		this.numTrials = trials;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
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
