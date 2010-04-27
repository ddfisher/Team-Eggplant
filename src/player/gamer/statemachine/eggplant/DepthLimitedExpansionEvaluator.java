package player.gamer.statemachine.eggplant;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class DepthLimitedExpansionEvaluator implements ExpansionEvaluator {
  
  private int limit;
  
  public DepthLimitedExpansionEvaluator(int depthLimit) {
    limit = depthLimit;
  }
  
  @Override
  public boolean eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
    return depth <= limit;
  }
  
}
