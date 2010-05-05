package player.gamer.statemachine.eggplant.metagaming;

import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.eggplant.heuristic.MobilityHeuristic;
import player.gamer.statemachine.eggplant.heuristic.MobilityType;
import player.gamer.statemachine.eggplant.misc.CacheValue;
import player.gamer.statemachine.eggplant.misc.DepthLimitException;
import player.gamer.statemachine.eggplant.misc.TimeUpException;
import player.gamer.statemachine.eggplant.misc.ValuedMove;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class EndgameBook {
  private HashMap<MachineState, CacheValue> book;
  private MobilityHeuristic heuristic;

  public EndgameBook(int numPlayers) {
    book = new HashMap<MachineState, CacheValue>();
    heuristic = new MobilityHeuristic(MobilityType.ONE_STEP, numPlayers);
  }

  public void buildEndgameBook(StateMachine machine, MachineState state, Role role, int backup, int targetBranchingFactor, int depthLimit, long endTime) {
    System.out.println("Beginning Construction of Endgame Book");
    int minDepthLimit = depthLimit - 3;
    if (minDepthLimit <= 2)
      minDepthLimit = 2;
    int maxDepthLimit = depthLimit + 3;
    long startTime = System.currentTimeMillis();
    long currTime;
    while (true) {
      try {
        if ((currTime = System.currentTimeMillis()) > endTime)
          throw new TimeUpException();
        if (currTime - startTime > 1000) {
          System.out.println("Endgame book has " + book.size() + " states; searching depth " + depthLimit + " with branching factor " + heuristic.avgBranchingFactor(0));
          startTime = currTime;
        }
        MachineState examineState = findCloseToEndState(machine, state, role, backup);
        if (!book.containsKey(examineState)) {
          cachingAlphaBeta(machine, examineState, role, 0, 100, book, depthLimit, endTime);
          if (heuristic.avgBranchingFactor(0) < targetBranchingFactor && depthLimit < maxDepthLimit) {
            depthLimit++;
          }
          else if (heuristic.avgBranchingFactor(0) > targetBranchingFactor && depthLimit > minDepthLimit) {
            depthLimit--;
          }
        } else {
          // System.out.println("Duplicate end examine");
        }
      } catch (MoveDefinitionException e) {
      } catch (TransitionDefinitionException e) {
      } catch (GoalDefinitionException e) {
      } catch (DepthLimitException e) {
        // System.out.println("End search too deep");
      } catch (TimeUpException e) {
        System.out.println(book.size() + " Endgame States Cached");
        break;
      }
    }
  }

  private MachineState findCloseToEndState(StateMachine machine, MachineState state, Role role, int backup)
  throws MoveDefinitionException, TransitionDefinitionException {
    MachineState[] trail = new MachineState[backup];
    for (int i = 0; i < backup; i++) {
      trail[i] = state;
    }
    
    int index = 0;
    
    MachineState currState = state;
    while (!machine.isTerminal(currState)) {
      currState = machine.getRandomNextState(currState);
      trail[index] = currState;
      if (index == backup - 1)
        index = 0;
      else 
        index++;
    }

    return trail[index];
  }

  public ValuedMove endgameValue(MachineState state) {
    CacheValue value = book.get(state);
    return (value != null) ? value.valuedMove : null;
  }

  private ValuedMove cachingAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta,
      HashMap<MachineState, CacheValue> cache, int depthLimit, long endTime) throws MoveDefinitionException,
      TransitionDefinitionException, GoalDefinitionException, TimeUpException, DepthLimitException {
    if (System.currentTimeMillis() > endTime)
      throw new TimeUpException();
    if (depthLimit <= 0)
      throw new DepthLimitException();
    if (cache.containsKey(state)) {
      CacheValue cached = cache.get(state);
      if (alpha >= cached.alpha && beta <= cached.beta) {
        return cached.valuedMove;
      }
    }
    ValuedMove result = alphaBeta(machine, state, role, alpha, beta, cache, depthLimit, endTime);
    cache.put(state, new CacheValue(result, alpha, beta));
    return result;
  }

  private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta,
      HashMap<MachineState, CacheValue> cache, int depthLimit, long endTime) throws MoveDefinitionException,
      TransitionDefinitionException, GoalDefinitionException, TimeUpException, DepthLimitException {
    if (machine.isTerminal(state)) {
      return new ValuedMove(machine.getGoal(state, role), null);
    }

    ValuedMove maxMove = new ValuedMove(-1, null);
    List<Move> possibleMoves = machine.getLegalMoves(state, role);
    heuristic.update(machine, state, role, alpha, beta, 0, 0);
    for (Move move : possibleMoves) {
      List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
      int min = 100;
      int newBeta = beta;
      for (List<Move> jointMove : jointMoves) {
        MachineState nextState = machine.getNextState(state, jointMove);
        int value = cachingAlphaBeta(machine, nextState, role, alpha, newBeta, cache, depthLimit - 1, endTime).value;
        if (value < min) {
          min = value;
          if (min <= alpha)
            break;
          if (min < newBeta)
            newBeta = min;
        }
      }
      if (min > maxMove.value) {
        maxMove.value = min;
        maxMove.move = move;
        if (maxMove.value >= beta)
          break;
        if (maxMove.value > alpha)
          alpha = maxMove.value;
      }
    }
    return maxMove;
  }

}
