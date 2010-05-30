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

	private final Map<Integer, Integer> preventingLatches;
	private final Map<Integer, Integer> determiningLatches;
	private final int[][] goals;
	private final int goalSum;
	
	public LatchHeuristic(BooleanPropNetStateMachine machine, int role) {
		preventingLatches = new HashMap<Integer, Integer>();
		determiningLatches = new HashMap<Integer, Integer>();
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
				for (int tf = 0; tf < 2; tf++) {
					if (latchEffects[tf] == -1) { // This goal will never become true
						preventingLatches.put(latch - basePropStart, goal);
					}
					else if (latchEffects[tf] == 1) { // This goal will never become true
						determiningLatches.put(latch - basePropStart, goal);
					}
				}
			}
		}
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		if (machine instanceof BooleanPropNetStateMachine && state instanceof BooleanMachineState) {
			boolean[] baseProps = ((BooleanMachineState) state).getBooleanContents();
			for (int latch : determiningLatches.keySet()) {
				if (baseProps[latch]) {
					return goals[determiningLatches.get(latch)][1];
				}
			}
			int sum = goalSum;
			int count = goals.length;
			boolean[] goalsPrevented = new boolean[goals.length];
			for (int latch : preventingLatches.keySet()) {
				if (baseProps[latch]) {
					goalsPrevented[preventingLatches.get(latch)] = true;
				}
			}
			for (int goal = 0; goal < goalsPrevented.length; goal++) {
				if (goalsPrevented[goal]) {
					sum -= goals[goal][1];
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
