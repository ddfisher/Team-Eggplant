package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public interface ExpansionEvaluator {
  
  public boolean eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth);
  
}
