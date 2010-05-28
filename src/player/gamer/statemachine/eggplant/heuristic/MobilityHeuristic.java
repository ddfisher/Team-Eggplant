package player.gamer.statemachine.eggplant.heuristic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import player.gamer.statemachine.eggplant.misc.Log;
import player.gamer.statemachine.eggplant.misc.TimeUpException;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class MobilityHeuristic implements Heuristic {

	private long[] totalBranchingFactor;
	private int[] samples;
	private int numPlayers;
	private int depthLimit;
	private int avgGoal;
	protected final MobilityType type;
	private final int BRANCH_QUO = 3;
	private final int AVG_DEFAULT = 50;
	private long varTime, varRuns;
	private final boolean TIME = true;

	public MobilityHeuristic(MobilityType type, int numPlayers) {
		this(type, numPlayers, (type == MobilityType.VAR_STEP) ? Math.max(2, numPlayers) : 1);
	}

	public MobilityHeuristic(MobilityType type, int numPlayers, int deptLimit) {
		this.type = type;
		this.numPlayers = numPlayers;
		this.depthLimit = deptLimit;
		this.avgGoal = AVG_DEFAULT;

		int n = 0;
		switch (type) {
		case VAR_STEP: n = 1;
		default: n = numPlayers;
		}
			
		totalBranchingFactor = new long[n];
		samples = new int[n];
	}
	
	public void setAvgGoal(int goal) {
		this.avgGoal = goal;
	}
	
	public int avgBranchingFactor(int index) {
		if (samples[index] == 0) return -1;
		return (int) (totalBranchingFactor[index] / samples[index]);
	}
	
	public int samplesLimit() {
		return Math.max(5, avgBranchingFactor(0) / BRANCH_QUO);
	}

	protected class BranchingData {
		public BranchingData(int samples, int total) {
			this.samples = samples;
			this.total = total;
		}
		int samples;
		int total;
	}

	// -1: failure; caller to handle
	protected int judgeMobility(double mobility, int index) {
		int abf = avgBranchingFactor(index);
		if (samples[index] < 1 || abf < 1) return avgGoal;
		return judge(mobility, avgBranchingFactor(index));
	}
	
	protected int judgeRelevantMobility(double mobility) {
		/*System.out.println((this instanceof OpponentMobilityHeuristic ? "opponent " : "player ") + "mobility is " + mobility + " vs. avg " 
				+ (double) totalBranchingFactor[0] / samples[0]);*/
		return judgeMobility(mobility, 0);
	}
	
	/*
	 * Uses a scaled logistic function to judge mobility against the average on
	 * a scale of 0 to 100. With an advantage of (averageBranching / 3) moves
	 * between the current and average mobility, the evaluator function will
	 * produce a result of 73.
	 */
	private int judge(double mobility, double avg) {
		return (int) Math
		.round((1.0 / (1.0 + Math.exp(-1.0 / (avg / 3.0) * (mobility - avg)))) * 100.0);
	}
	
	protected int evalVarStep(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int properDepth, int evalDepth, long timeout)
	throws MoveDefinitionException, TransitionDefinitionException, TimeUpException {
		/*long st;
		if (TIME) st = System.currentTimeMillis();*/
		BranchingData data = getFirstRelevantBranchingData(machine, state, role, evalDepth, timeout, samplesLimit());
		double avg = 0;
		//Log.println('t', "	samples taken: " + data.samples);
		if (data.samples > 0) avg = data.total / (double) data.samples;
		else return (alpha + beta) / 2;
		int ev = judgeRelevantMobility(avg);
		/*if (TIME) {
			varTime += (System.currentTimeMillis() - st);
			varRuns++;
			Log.println('t', "Var mobility stats: time " + varTime + ", runs " 
					+ varRuns + ", avg " + (double)varTime/varRuns + ", result " + avg);
		}*/
		return (ev >= 0) ? ev : (alpha + beta) / 2;
	}

	protected int evalNStep(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int properDepth, int evalDepth)
	throws MoveDefinitionException, TransitionDefinitionException {
		BranchingData data = getBranchingData(machine, state, role, alpha, beta, evalDepth, false);
		double avg;
		if (data.samples > 0) avg = data.total / (double) data.samples;
		else return (alpha + beta) / 2;
		int ev = judgeMobility(avg, getIndex(properDepth));
		return (ev >= 0) ? ev : (alpha + beta) / 2;
	}

	protected int evalOneStep(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int properDepth)
	throws MoveDefinitionException, TransitionDefinitionException {
		int ev = judgeMobility(machine.getLegalMoves(state, role).size(), getIndex(properDepth));
		//System.out.println("res " + (ev > 0 ? ev : (alpha + beta) / 2) + " for bf " + machine.getLegalMoves(state, role).size() + " against avg " + avgBranchingFactor(getIndex(properDepth)));
		return (ev >= 0) ? ev : (alpha + beta) / 2;
	}

	// Counts the branching factor the current and next depth turns if countCurrent is true,
	// and over the moves at the nth turn down if countCurrent is false.
	// N.B. Setting countCurrent to true will usually cause counting over both op-turns and noop-turns.
	private BranchingData getBranchingData(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth, boolean countCurrent)
	throws MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state)) return new BranchingData(0, 0);
		if (depth == 0) return new BranchingData(1, machine.getLegalMoves(state, role).size());
		List<List<Move>> joints = machine.getLegalJointMoves(state);
		int samples = 0, total = 0;
		for (List<Move> joint : joints) {
			MachineState nextState = machine.getNextState(state, joint);
			BranchingData data = getBranchingData(machine, nextState, role, 
					alpha, beta, depth - 1, countCurrent);
			samples += data.samples;
			total += data.total;
			if (countCurrent) {
				samples += 1;
				total += machine.getLegalMoves(nextState, role).size();
			}
		}
		return new BranchingData(samples, total);
	}
	
	class StateGroup {
		StateGroup(MachineState state, int depth) {
			this.state = state;
			this.depth = depth;
		}
		public MachineState state;
		public int depth;
	}
	
	private BranchingData getFirstRelevantBranchingData(StateMachine machine, MachineState state, Role role, int depthLeft, long timeout, int limit) 
	throws MoveDefinitionException, TransitionDefinitionException {
		int samples = 0, total = 0;
		Queue<StateGroup> gQueue = new LinkedList<StateGroup>();
		gQueue.add(new StateGroup(state, depthLeft));
		while (!gQueue.isEmpty() && samples < limit) {
			if (System.currentTimeMillis() > timeout) break;
			StateGroup g = gQueue.poll();
			if (machine.isTerminal(state)) continue;
			List<Move> moves = machine.getLegalMoves(g.state, role);
			if (relevant(moves)) {
				samples++;
				total += moves.size();
				if (samples >= limit) break;
				continue;
			} else if (g.depth > 0) {
				List<List<Move>> joints = machine.getLegalJointMoves(g.state);
				int s = gQueue.size();
				for (List<Move> joint : joints) {
					if (s >= (limit - samples)) continue;
					if (System.currentTimeMillis() > timeout) break;
					MachineState nextState = machine.getNextState(g.state, joint);
					gQueue.add(new StateGroup(nextState, g.depth - 1));
				}
			}
		}
		return new BranchingData(samples, total);
	}
	
	protected boolean relevant(int moves) { return moves > 1; }
	
	protected boolean relevant(List<Move> moves) {
		return moves.size() > 1;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth, int absDepth, long timeout) 
	throws TimeUpException {
		int properDepth = absDepth + depth;
		try {
			switch (type) {
			case N_STEP:
				return evalNStep(machine, state, role, alpha, beta, properDepth, depthLimit);
			case VAR_STEP:
				return evalVarStep(machine, state, role, alpha, beta, properDepth, depthLimit, timeout);
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
	public void update(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth, int absDepth) 
	throws MoveDefinitionException {
		updateAverage(machine.getLegalMoves(state, role).size(), depth + absDepth);	
	}

	protected void updateAverage(int branchingFactor, int properDepth) {
		int index = 0;
		if (type != MobilityType.VAR_STEP) index = getIndex(properDepth);
		else if (branchingFactor <= 1) return;
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
