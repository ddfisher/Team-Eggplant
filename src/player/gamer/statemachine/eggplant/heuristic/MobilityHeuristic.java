package player.gamer.statemachine.eggplant.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class MobilityHeuristic implements Heuristic {

	private long[] totalBranchingFactor;
	private int[] samples;
	private int numPlayers;
	private int depthLimit;
	protected final MobilityType type;

	public MobilityHeuristic(MobilityType type, int numPlayers) {
		this(type, numPlayers, 1);
	}

	public MobilityHeuristic(MobilityType type, int numPlayers, int deptLimit) {
		this.type = type;
		this.numPlayers = numPlayers;
		this.depthLimit = deptLimit;

		totalBranchingFactor = new long[numPlayers];
		samples = new int[numPlayers];
	}

	private class BranchingData {
		public BranchingData(int samples, int total) {
			this.samples = samples;
			this.total = total;
		}

		int samples;
		int total;
	}

	/*
	 * Uses a scaled logistic function to judge mobility against the average on
	 * a scale of 0 to 100. With an advantage of (averageBranching / 4) moves
	 * between the current and average mobility, the evaluator function will
	 * produce a result of 73.
	 */
	protected int judgeMobility(double mobility, int index) {
		double averageBranching = (double) totalBranchingFactor[index] / samples[index];
		// System.out.println("avg br " + averageBranching + ", samples " +
		// samples + ", mob " + mobility);
		// System.out.println("result " + 1 / (1 +
		// Math.exp(-1.0/(averageBranching/3) * (mobility-averageBranching))) *
		// 100);
		return (int) Math
				.round((1 / (double) (1 + Math.exp(-1.0 / (averageBranching / 3) * (mobility - averageBranching)))) * 100);
	}

	protected int evalNStep(StateMachine machine, MachineState state, Role role, int alpha, int beta, int properDepth, int evalDepth)
			throws MoveDefinitionException, TransitionDefinitionException {
		BranchingData data = getBranchingData(machine, state, role, alpha, beta, evalDepth);
		double avg = data.total / (double) data.samples;
		return judgeMobility(avg, getIndex(properDepth));
	}

	protected int evalOneStep(StateMachine machine, MachineState state, Role role, int alpha, int beta, int properDepth)
			throws MoveDefinitionException, TransitionDefinitionException {
		return evalNStep(machine, state, role, alpha, beta, properDepth, 0);
	}

	// Counts the branching factor for the given player over the current and the
	// next depth turns.
	// NB This implementation counts both play-turns and noop-turns, but to do
	// otherwise seems difficult,
	// and patterns will still emerge from the branching factors of the
	// play-turns.
	private BranchingData getBranchingData(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth)
			throws MoveDefinitionException, TransitionDefinitionException {
		if (depth == 0) {
			List<Move> moves = machine.getLegalMoves(state, role);
			return new BranchingData(1, moves.size());
		}
		List<List<Move>> joints = machine.getLegalJointMoves(state);
		int samples = 0, total = 0;
		for (List<Move> joint : joints) {
			MachineState nextState = machine.getNextState(state, joint);
			BranchingData data = getBranchingData(machine, nextState, role, alpha, beta, depth - 1);
			samples += data.samples;
			total += data.total;
		}
		return new BranchingData(samples, total);
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		int properDepth = absDepth + depth;
		try {
			switch (type) {
			case N_STEP:
				return evalNStep(machine, state, role, alpha, beta, properDepth, depthLimit);
			default:
				return evalOneStep(machine, state, role, alpha, beta, properDepth);
			}
		} catch (MoveDefinitionException m) {
			m.printStackTrace();
		} catch (TransitionDefinitionException t) {
			t.printStackTrace();
		}
		return (alpha + beta) / 2;
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) {
		try {
			updateAverage(machine.getLegalMoves(state, role).size(), depth + absDepth);
		} catch (MoveDefinitionException e) { /* meh */
		}
	}

	protected void updateAverage(int branchingFactor, int properDepth) {
		int index = getIndex(properDepth);
		totalBranchingFactor[index] += branchingFactor;
		samples[index]++;
	}

	protected int getIndex(long properDepth) {
		return (int) (properDepth % numPlayers);
	}

	public int getDepthLimit() {
		return depthLimit;
	}

}
