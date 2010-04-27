package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class MobilityHeuristicEvaluator implements HeuristicEvaluator, MobilityTracker {

	private double averageBranching;
	private int samples = 0;
	private int depthLimit = 1;
	private final MobilityType type;
	
	public MobilityHeuristicEvaluator(MobilityType type) {
		this.type = type;
	}
	
	public MobilityHeuristicEvaluator() {
		this(MobilityType.ONE_STEP);
	}
	
	private class BranchingData {
		public BranchingData(int samples, int total) {
			this.samples = samples;
			this.total = total;
		}
		int samples;
		int total;
	}
	
	/* Uses a scaled logistic function to judge mobility against the average on a scale of 0 to 100.
	 * With an advantage of (averageBranching / 4) moves between the current and average mobility, the
	 * evaluator function will produce a result of 73.
	 */
	private int judgeMobility(double mobility) {
		//System.out.println("avg br " + averageBranching + ", samples " + samples + ", mob " + mobility);
		//System.out.println("result " + 1 / (1 + Math.exp(-1.0/(averageBranching/4) * (mobility-averageBranching))) * 100);
		return (int) Math.round((1 / (double)(1 + Math.exp(-1.0/(averageBranching / 4) * 
				(mobility - averageBranching)))) * 100);
	}
	
	private int evalNStep(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) 
	throws MoveDefinitionException, TransitionDefinitionException {
		BranchingData data = getBranchingData(machine, state, role, alpha, beta, depth);
		double avg = data.total / (double) data.samples;
		return judgeMobility(avg);
	}
	
	private int evalOneStep(StateMachine machine, MachineState state, Role role, int alpha, int beta) 
	throws MoveDefinitionException, TransitionDefinitionException {
		return evalNStep(machine, state, role, alpha, beta, 0);
	}
	
	// Counts the branching factor for the given player over the current and the next depth turns.
	// NB This implementation counts both play-turns and noop-turns, but to do otherwise seems difficult,
	// and patterns will still emerge from the branching factors of the play-turns. 
	private BranchingData getBranchingData(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth) throws MoveDefinitionException, TransitionDefinitionException {
		if (depth == 0) {
			List<Move> moves = machine.getLegalMoves(state, role);
			return new BranchingData(1, moves.size());
		}
		List<List<Move>> joints = machine.getLegalJointMoves(state);
		int samples = 0, total = 0;
		for (List<Move> joint : joints) {
			MachineState nextState = machine.getNextState(state, joint);
			BranchingData data = getBranchingData(machine, nextState, role, alpha, beta, depth-1);
			samples += data.samples;
			total += data.total;
		}
		return new BranchingData(samples, total);
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
		try {
			switch (type) {
			case N_STEP: return evalNStep(machine, state, role, alpha, beta, depthLimit);
			default: return evalOneStep(machine, state, role, alpha, beta);
			}
		} catch (MoveDefinitionException m) {
			m.printStackTrace();
		} catch (TransitionDefinitionException t) {
			t.printStackTrace();
		}
		return (alpha + beta) / 2;
	}
	
	public void updateAverage(int factor) {
		averageBranching = samples / (double)(samples + 1) * averageBranching + factor / (double)(samples + 1);
		samples++;
	}
	
	public int depthLimit() { return depthLimit; }
	
	public void setDepthLimit(int depthLimit) {
		if (depthLimit > 0) this.depthLimit = depthLimit;
	}

}
