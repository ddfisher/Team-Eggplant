package player.gamer.statemachine.eggplant.heuristic;

import player.gamer.statemachine.eggplant.misc.TimeUpException;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;

public interface Heuristic {
	
	public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth, long timeout) throws MoveDefinitionException, TimeUpException;
	public void update(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth, int absDepth) throws MoveDefinitionException;
	
}
