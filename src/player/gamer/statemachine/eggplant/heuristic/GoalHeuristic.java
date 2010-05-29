package player.gamer.statemachine.eggplant.heuristic;

import java.util.Arrays;

import player.gamer.statemachine.eggplant.misc.Log;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.propnet.architecture.BooleanPropNet;
import util.statemachine.BooleanMachineState;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.propnet.BooleanPropNetStateMachine;

public class GoalHeuristic implements Heuristic {
	private final float[][][] significance;
	private final int[] goalValues;
	private final int numGoals;
	private final int numBaseProps;	
	
	public GoalHeuristic(BooleanPropNetStateMachine machine, int role) {
		float[][][][] significanceRef = new float[1][][][];
		int[][] goalValuesRef = new int[1][];
		machine.populateGoalHeuristicArrays(role, significanceRef, goalValuesRef);
		this.significance = significanceRef[0];
		this.goalValues = goalValuesRef[0];
		this.numGoals = this.goalValues.length;
		this.numBaseProps = this.significance[0].length;
		
		// Normalize
		for (int goal = 0; goal < numGoals; goal++) {
			float sum = 0;
			for (int baseProp = 0; baseProp < numBaseProps; baseProp++) {
				sum += Math.max(significance[goal][baseProp][0], significance[goal][baseProp][1]); 
			}
			/* if (goal == numGoals - 1)
				Log.println('x', "At initializing goal " + goalValues[goal] + ": with sum " + sum);
				
			TreeMap<String, String> sortingMap = new TreeMap<String, String>();
			*/
			for (int baseProp = 0; baseProp < numBaseProps; baseProp++) {
				significance[goal][baseProp][0] /= sum;
				significance[goal][baseProp][1] /= sum;
				/*if (goal == numGoals - 1)
					sortingMap.put(machine.propIndex[baseProp + 1].toString(), machine.propIndex[baseProp + 1] + " = (" + significance[goal][baseProp][0] + "," + significance[goal][baseProp][1] + "), originally " + "(" + significance[goal][baseProp][0] * sum + "," + significance[goal][baseProp][1] * sum + ")");
					*/
			}
			/*
			if (goal == numGoals - 1) {
				for (String prop : sortingMap.keySet()) {
					Log.println('x', sortingMap.get(prop));
				}
			}
				Log.println('x', "");
			*/			
		}
	}
	
	@Override
	public int eval(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth, long timeout)
			throws MoveDefinitionException, TimeUpException {
		if (machine instanceof BooleanPropNetStateMachine && state instanceof BooleanMachineState) {
			boolean[] baseProps = ((BooleanMachineState)state).getBooleanContents();
			float[] goalSignificance = new float[numGoals];
			float sum = 0;
			for (int goal = 0; goal < numGoals; goal++) {
				for (int baseProp = 0; baseProp < numBaseProps; baseProp++) {
					goalSignificance[goal] += baseProps[baseProp] ? (significance[goal][baseProp][0]) : (significance[goal][baseProp][1]);
				}
				sum += goalSignificance[goal];
			}
			// Calculated normalized weighted goal proximity
			float goalProximity = 0;
			for (int goal = 0; goal < numGoals; goal++) {
				goalProximity += goalValues[goal] * goalSignificance[goal] / sum;
			}
			if (timeout == 0)
			  Log.println('x', "Goal proximity " + goalProximity + " = " + Arrays.toString(goalSignificance));
			int ret = (int) Math.round(goalProximity);
			if (ret >= BooleanPropNet.GOAL_SCALE_FACTOR * 100) ret = BooleanPropNet.GOAL_SCALE_FACTOR * 100 - 1;
			if (ret <= 0) ret = 1;
			return ret;
		}
		else {
			return 0;
		}
	}
	@Override
	public void update(StateMachine machine, MachineState state, Role role,
			int alpha, int beta, int depth, int absDepth)
			throws MoveDefinitionException {
		// TODO Auto-generated method stub
		
	}
	
}
