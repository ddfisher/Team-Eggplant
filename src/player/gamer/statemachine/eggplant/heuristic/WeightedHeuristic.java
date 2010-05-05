package player.gamer.statemachine.eggplant.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;

public class WeightedHeuristic implements Heuristic{
	
	private Heuristic[] heuristics;
	private double[] weights;

	public WeightedHeuristic(Heuristic[] heuristics, double[] weights) {
		super();
		this.heuristics = heuristics;
		this.weights = weights;
	}

	public WeightedHeuristic(int numPlayers) {
		heuristics = new Heuristic[2];
		heuristics[0] = new MonteCarloHeuristic(2);
		heuristics[1] =  new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers);
		
		weights = new double[2];
		weights[0] = 0.7;
		weights[1] = 0.3;
	}

	@Override
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) throws MoveDefinitionException {
		float total = 0;
		for (int i = 0; i < heuristics.length; i++){
			total += heuristics[i].eval(machine, state, role, alpha, beta, depth, absDepth) * weights[i];
		}
		
		return Math.round(total);
	}

	@Override
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) throws MoveDefinitionException {
		for (Heuristic heuristic : heuristics) {
			heuristic.update(machine, state, role, alpha, beta, depth, absDepth);
		}
		
	}


}
