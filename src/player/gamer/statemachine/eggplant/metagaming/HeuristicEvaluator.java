package player.gamer.statemachine.eggplant.metagaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import player.gamer.statemachine.eggplant.heuristic.Heuristic;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class HeuristicEvaluator {
	private final int MIN_VALID = 10;
	private int[][][] levels; //trial | turn | heuristic
	private int[] goals;
	private static final Random rand = new Random();

	public void evaluateHeuristic(Heuristic[] heuristics, int trials, StateMachine machine, MachineState state, Role role) {
		levels = new int[trials][][];
		goals = new int[trials];
		for (int i = 0; i < trials; i++) {
			try {
				runTrial(heuristics, machine, state, role, i);
			} catch (GoalDefinitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				i--;
			}
		}
	}

	private void runTrial(Heuristic[] heuristics, StateMachine machine, MachineState state, Role role, int trialNum) throws GoalDefinitionException {
		MachineState currState = state;
		ArrayList<int[]> level = new ArrayList<int[]>();
		while (!machine.isTerminal(currState)) {
			try {
				int[] results = new int[heuristics.length];
				for (int i = 0; i < heuristics.length; i++) {
					results[i] = heuristics[i].eval(machine, currState, role, -1, -1, level.size(), level.size(), System.currentTimeMillis() + 1000);
				}
				level.add(results);
				currState = machine.getRandomNextState(currState);
			} catch (MoveDefinitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TimeUpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransitionDefinitionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		goals[trialNum] = machine.getGoal(currState, role);
		levels[trialNum] = level.toArray(new int[0][0]);
	}
	
	public int[][][] getLevels() {
		return levels;
	}
	
	public int[] getGoals() {
		return goals;
	}
	
	public double[][] getTurnLevels(int turn) {
		double[][] turnLevels = new double[levels.length][];
		int valid = 0;
		for (int i = 0;  i < levels.length; i++) {
			if (turn < levels[i].length) {
				turnLevels[valid++] = toDouble(levels[i][turn]);
			}
		}
		
		if (valid < MIN_VALID)
			return null;
		
		if (valid < levels.length) {
			double[][] validLevels = new double[valid][];
			System.arraycopy(turnLevels, 0, validLevels, 0, validLevels.length);
			return validLevels;
		} else {
			return turnLevels;
		}
	}
	
	public int[] getTurnGoals(int turn) {
		int[] turnGoals = new int[goals.length];
		int valid = 0;
		for (int i = 0;  i < goals.length; i++) {
			if (turn < levels[i].length) {
				turnGoals[valid++] = goals[i]==100 ? 1 : 0;
			}
		}
		
		if (valid < MIN_VALID)
			return null;
		
		if (valid < goals.length) {
			int[] validGoals = new int[valid];
			System.arraycopy(turnGoals, 0, validGoals, 0, validGoals.length);
			return validGoals;
		} else {
			return turnGoals;
		}
	}
	
	private double[] toDouble(int[] arr) {
		double[] newArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			newArr[i] = arr[i];
		}
		return newArr;
	}

	private int[] unbox(List<Integer> l) {
		int[] arr = new int[l.size()];
		int i = 0;
		for (int integer : l)
			arr[i++] = integer;
		return arr;
	}

}

