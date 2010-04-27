package player.gamer.statemachine.eggplant;

import java.util.Random;

import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class MonteCarloHeuristicEvaluator implements HeuristicEvaluator {
  
  private int numTrials;
  
  public MonteCarloHeuristicEvaluator(int trials) {
    this.numTrials = trials;
  }
  
  @Override
  public int eval(StateMachine machine, MachineState state, Role role, int alpha, int beta, int depth) {
    int successfulTrials = 0;
    int sum = 0;
    for (int trial = 0; trial < numTrials; trial++) {
      try {
        MachineState currState = state;
        while (!machine.isTerminal(currState)) {
          currState = machine.getRandomNextState(currState);
        }
        sum += machine.getGoal(currState, role);
        successfulTrials++;
      }
      catch (GoalDefinitionException ex) {
        continue;
      }
      catch (MoveDefinitionException ex) {
        continue;
      }
      catch (TransitionDefinitionException ex) {
        continue;
      }
    }
    if (successfulTrials == 0) {
      return 0;
    }
    return sum / successfulTrials;
  }
}
