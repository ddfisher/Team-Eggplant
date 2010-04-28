package player.gamer.statemachine.eggplant.heuristic;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public interface Heuristic {
	
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth);
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth);
	
}
