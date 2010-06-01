package player.gamer.statemachine.eggplant.heuristic;

import java.util.HashMap;
import java.util.Map;

import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.propnet.architecture.BooleanPropNet;
import util.statemachine.BooleanMachineState;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;

public class LatchHeuristic implements Heuristic {

	private final Map<Integer, int[]> preventingLatches;
	private final Map<Integer, int[]> determiningLatches;
	private final int[][] goals;
	private final int goalSum;
	
	public LatchHeuristic(BooleanPropNetStateMachine machine, int role) {
		preventingLatches = new HashMap<Integer, int[]>();
		determiningLatches = new HashMap<Integer, int[]>();
		int[][][] goalPropMap = machine.getGoalPropMap();
		this.goals = goalPropMap[role];
		int basePropStart = machine.getBasePropStart();
		int inputPropStart = machine.getInputPropStart();
		
		int sum = 0;
		for (int goal = 0; goal < goals.length; goal++) {
			sum += goals[goal][1];
		}
		this.goalSum = sum;
		
		for (int goal = 0; goal < goals.length; goal++) {
			int goalNum = goals[goal][0];
			Map<Integer, int[]> affectingLatches = machine.getLatchesOn(goalNum);
			for (int latch : affectingLatches.keySet()) {
				if (!(latch >= basePropStart && latch < inputPropStart))
					continue;
				
				int[] latchEffects = affectingLatches.get(latch);
				int[] ar;
				for (int tf = 0; tf < 2; tf++) {
					if (latchEffects[tf] == -1) { // This goal will never become true
						ar = preventingLatches.get(latch - basePropStart);
						if (ar == null) {
							ar = new int[2];
							preventingLatches.put(latch - basePropStart, ar);
						}
						ar[tf] = goal;
					}
					else if (latchEffects[tf] == 1) { // This goal will never become true
						ar = determiningLatches.get(latch - basePropStart);
						if (ar == null) {
							ar = new int[2];
							determiningLatches.put(latch - basePropStart, ar);
						}
						ar[tf] = goal;
					}
				}
			}
		}
	}
	
	public int eval(BooleanPropNetStateMachine machine, BooleanMachineState state) {
		boolean[] baseProps = state.getBooleanContents();
		int[] ar;
		for (int latch : determiningLatches.keySet()) {
			ar = determiningLatches.get(latch);
			if (baseProps[latch] && ar[1] != 0) {
				return ~goals[ar[1]][1];
			}
			if (!baseProps[latch] && ar[0] != 0) {
				return ~goals[ar[0]][1];
			}
		}
		int sum = goalSum;
		int count = goals.length;
		boolean[] goalsPrevented = new boolean[goals.length];
		for (int latch : preventingLatches.keySet()) {
			ar = preventingLatches.get(latch);
			if (baseProps[latch] && ar[1] != 0 && !goalsPrevented[ar[1]]) {
				goalsPrevented[ar[1]] = true;
				sum -= goals[ar[1]][1];
				count--;
			}
			if (!baseProps[latch] && ar[0] != 0 && !goalsPrevented[ar[0]]) {
				goalsPrevented[ar[0]] = true;
				sum -= goals[ar[0]][1];
				count--;
			}
		}
		if (count == 1) { // Goal effectively determined
			return ~sum;
		}
		else {
			return sum / count;
		}
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		if (machine instanceof BooleanPropNetStateMachine && state instanceof BooleanMachineState) {
			boolean[] baseProps = ((BooleanMachineState) state).getBooleanContents();
			int[] ar;
			for (int latch : determiningLatches.keySet()) {
				ar = determiningLatches.get(latch);
				if (baseProps[latch] && ar[1] != 0) {
					return goals[ar[1]][1];
				}
				if (!baseProps[latch] && ar[0] != 0) {
					return goals[ar[0]][1];
				}
			}
			int sum = goalSum;
			int count = goals.length;
			boolean[] goalsPrevented = new boolean[goals.length];
			for (int latch : preventingLatches.keySet()) {
				ar = preventingLatches.get(latch);
				if (baseProps[latch] && ar[1] != 0 && !goalsPrevented[ar[1]]) {
					goalsPrevented[ar[1]] = true;
					sum -= goals[ar[1]][1];
					count--;
				}
				if (!baseProps[latch] && ar[0] != 0 && !goalsPrevented[ar[0]]) {
					goalsPrevented[ar[0]] = true;
					sum -= goals[ar[0]][1];
					count--;
				}
			}
			return sum / count; 
		}
		else {
			return BooleanPropNet.GOAL_SCALE_FACTOR * 50;
		}
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth)
			throws MoveDefinitionException {
		// TODO Auto-generated method stub
		
	}

}
