package player.gamer.statemachine.eggplant.heuristic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic.BranchingData;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class OpponentMobilityHeuristic extends MobilityHeuristic {

	public OpponentMobilityHeuristic(MobilityType type, int numPlayers) {
		this(type, numPlayers, 1);
	}
	
	public OpponentMobilityHeuristic(MobilityType type, int numPlayers, int deptLimit) {
		super(type, numPlayers, deptLimit);
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long endTime) {
		try {
			double avg = 0;
			switch (type) {
			case VAR_STEP:
				BranchingData data = getRelevantOpponentBranchingData(machine, state, role, 
						getDepthLimit(), endTime, samplesLimit());
				if (data.samples == 0) return (alpha + beta) / 2;
				avg = (double) data.total / data.samples;
				return judgeRelevantMobility(avg);
			default: 
				avg = machine.getLegalJointMoves(state).size() / machine.getLegalMoves(state, role).size();
				return judgeMobility(avg, getIndex(depth + absDepth));
			}
		} catch (MoveDefinitionException e) {
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			e.printStackTrace();
		}
		return (alpha + beta) / 2;
	}
	
	/*private BranchingData getRelevantOpponentBranchingData(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int maxDepth, long endTime, int limit)
	throws MoveDefinitionException, TransitionDefinitionException {
		if (machine.isTerminal(state) || limit == 0) return new BranchingData(0, 0);
		List<Move> moves = machine.getLegalMoves(state, role);
		List<List<Move>> joints = machine.getLegalJointMoves(state);
		int sumMoves = joints.size(), roleMoves = moves.size(), quo = sumMoves / roleMoves;
		if (relevant(quo)) return new BranchingData(1, quo);
		else if (maxDepth == 0) return new BranchingData(0, 0);
		int samples = 0, total = 0;
		List<MachineState> nextStates = new ArrayList<MachineState>();
		for (List<Move> joint : joints) {
			if (System.currentTimeMillis() > endTime) break;
			MachineState nextState = machine.getNextState(state, joint);
			List<Move> nextMoves = machine.getLegalMoves(nextState, role);
			List<List<Move>> jointNextMoves = machine.getLegalJointMoves(nextState);
			if (relevant(nextMoves)) {
				samples++;
				total += jointNextMoves.size() / nextMoves.size();
				if (samples >= limit) break;
			} else {
				nextStates.add(nextState);
			}
		}
		for (MachineState ns : nextStates) {
			if (samples >= limit ||  (System.currentTimeMillis() > endTime)) break;
			BranchingData data = 
				getRelevantOpponentBranchingData(machine, ns, role, alpha, beta, maxDepth - 1, endTime, limit - samples);
			samples += data.samples;
			total += data.total;
		}
		return new BranchingData(samples, total);
	}*/
	
	private BranchingData getRelevantOpponentBranchingData(StateMachine machine, MachineState state, Role role, int depthLeft, long timeout, int limit) 
	throws MoveDefinitionException, TransitionDefinitionException {
		int samples = 0, total = 0;
		Queue<StateGroup> gQueue = new LinkedList<StateGroup>();
		gQueue.add(new StateGroup(state, depthLeft));
		while (!gQueue.isEmpty() && samples < limit) {
			if (System.currentTimeMillis() > timeout) break;
			StateGroup g = gQueue.poll();
			if (machine.isTerminal(state)) continue;
			List<List<Move>> joints = machine.getLegalJointMoves(g.state);
			List<Move> moves = machine.getLegalMoves(g.state, role);
			int sumMoves = joints.size(), roleMoves = moves.size(), quo = sumMoves / roleMoves;
			if (relevant(quo)) {
				samples++;
				total += quo;
				if (samples >= limit) break;
				continue;
			} else if (g.depth > 0) {
				for (List<Move> joint : joints) {
					if (System.currentTimeMillis() > timeout) break;
					MachineState nextState = machine.getNextState(g.state, joint);
					gQueue.add(new StateGroup(nextState, g.depth - 1));
				}
			}
		}
		return new BranchingData(samples, total);
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, 
			int alpha, int beta, int depth, int absDepth) 
	throws MoveDefinitionException {
		int pmSize = machine.getLegalMoves(state, role).size();
		int oppSize = machine.getLegalJointMoves(state).size() / pmSize;
		updateAverage(oppSize, depth + absDepth);
	}

}
