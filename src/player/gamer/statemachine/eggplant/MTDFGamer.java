package player.gamer.statemachine.eggplant;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.cache.CachedProverStateMachine;
import apps.player.config.ConfigPanel;
import apps.player.detail.DetailPanel;

public class MTDFGamer extends StateMachineGamer {
  private int statesSearched;
  private int leafNodesSearched;
  private int cacheHits, cacheMisses;
  private EggplantConfigPanel config = new EggplantConfigPanel();
  private HashMap<MachineState, CacheValue> keptCache;
  private boolean maximizationToggle;
  // TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
  // the sentences are actually the same objects in memory

  @Override
  public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    // do nothing
    keptCache = new HashMap<MachineState, CacheValue>();
    maximizationToggle = true;
  }

  /* Implements Minimax search, currently ignores clock */
  @Override
  public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    long start = System.currentTimeMillis();

    HashMap<MachineState, CacheValue> cache = null;
    if (config.useCache()) {
      // cache = new HashMap<MachineState, CacheValue>();
      cache = keptCache;
    }

    leafNodesSearched = statesSearched = 0;
    cacheHits = cacheMisses = 0;

    ValuedMove result = mtdf(getStateMachine(), getCurrentState(), getRole(), -1, 101, maximizationToggle, cache);

    maximizationToggle = !maximizationToggle;
    
    long stop = System.currentTimeMillis();
    notifyObservers(new EggplantMoveSelectionEvent(result.move, result.value, stop - start, statesSearched, leafNodesSearched, cacheHits,
        cacheMisses));
    return result.move;
  }

  private ValuedMove mtdf(
      StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      boolean maximizing,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    int upperBound = beta;
    int lowerBound = alpha;
    ValuedMove currMove = new ValuedMove(50, null); // initial guess		
    while (lowerBound < upperBound) {
      int guess = currMove.value;
      if (currMove.value == lowerBound)
        guess++;
      currMove = memoizedAlphaBeta(machine, state, role, guess - 1, guess, maximizing, cache);
      if (currMove.value < guess) {
        upperBound = currMove.value;
      }
      else {
        lowerBound = currMove.value;
      }
    }
    return currMove;
  }

  private ValuedMove memoizedAlphaBeta(
      StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      boolean maximizing,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    if (cache != null) {
      if (cache.containsKey(state)) {
        // Cache stores lower-bound in alpha, upper-bound in beta
        CacheValue cached = cache.get(state);
        if (cached.alpha >= beta) {
          cacheHits++;
          return cached.valuedMove;
        }
        if (cached.beta <= alpha) {
          cacheHits++;
          return cached.valuedMove;
        }
        alpha = Math.max(alpha, cached.alpha);
        beta = Math.min(beta, cached.beta);
      }
      cacheMisses++;
      ValuedMove maxMove = alphaBeta(machine, state, role, alpha, beta, maximizing, cache);
      if (maxMove.value <= alpha) {
        if (!cache.containsKey(state)) {
          cache.put(state, new CacheValue(null, -1, 101));
        }
        cache.get(state).valuedMove = maxMove;
        cache.get(state).beta = maxMove.value;
      }
      else if (maxMove.value > alpha && maxMove.value < beta) {
        if (!cache.containsKey(state)) {
          cache.put(state, new CacheValue(null, -1, 101));
        }
        cache.get(state).valuedMove = maxMove;
        cache.get(state).alpha = maxMove.value;
        cache.get(state).beta = maxMove.value;
      }
      else if (maxMove.value >= beta) {
        if (!cache.containsKey(state)) {
          cache.put(state, new CacheValue(null, -1, 101));
        }
        cache.get(state).valuedMove = maxMove;
        cache.get(state).alpha = maxMove.value;
      }
      return maxMove;
    } else {
      return alphaBeta(machine, state, role, alpha, beta, maximizing, cache);
    }
  }

  private ValuedMove alphaBeta(StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      boolean maximizing,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    statesSearched++;
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      return new ValuedMove(machine.getGoal(state, role), null);
    }

    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      return new ValuedMove(machine.getGoal(state, role), null);
    }
    else {
      ValuedMove bestMove = null;
      List<Move> possibleMoves = machine.getLegalMoves(state, role);
      Collections.shuffle(possibleMoves); // TODO: Remove this line
      if (!maximizing && possibleMoves.size() != 1)
        System.out.println(maximizing + " " + possibleMoves);
      if (maximizing) {
        bestMove = new ValuedMove(-1, null);
        int newAlpha = alpha;
        
        for (Move move : possibleMoves) {
          List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
          Collections.shuffle(jointMoves); // TODO: Remove this line
          
          
          int min = 101;
          int newBeta = beta;
          for (List<Move> jointMove : jointMoves) {
            MachineState nextState = machine.getNextState(state, jointMove);
            int value = memoizedAlphaBeta(machine, nextState, role, newAlpha, newBeta, !maximizing, cache).value;
            if (value < min) {
              min = value;
              if (min <= newAlpha)
                break;
              if (min < newBeta)
                newBeta = min;
            }
          }
          if (min > bestMove.value) {
            bestMove.value = min;
            bestMove.move = move;
            if (bestMove.value >= beta)
              break;
            if (bestMove.value > newAlpha)
              newAlpha = bestMove.value;
          }
        }
      }
      else {
        bestMove = new ValuedMove(101, null);
        int newBeta = beta;
        for (Move move : possibleMoves) {
          List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
          
          Collections.shuffle(jointMoves); // TODO: Remove this line
          
          int max = -1;
          int newAlpha = alpha;
          for (List<Move> jointMove : jointMoves) {
            MachineState nextState = machine.getNextState(state, jointMove);
            int value = memoizedAlphaBeta(machine, nextState, role, newAlpha, newBeta, !maximizing, cache).value;
            if (value > max) {
              max = value;
              if (max >= newBeta)
                break;
              if (max > newAlpha)
                newAlpha = max;
            }
          }
          if (max < bestMove.value) {
            bestMove.value = max;
            bestMove.move = move;
            if (bestMove.value <= alpha)
              break;
            if (bestMove.value < newBeta)
              newBeta = bestMove.value;
          }
        }
      }
      return bestMove;
    }
  }

  @Override
  public StateMachine getInitialStateMachine() {
    return new CachedProverStateMachine();
  }

  @Override
  public String getName() {
    return "MTD(f)";
  }

  @Override
  public DetailPanel getDetailPanel() {
    return new EggplantDetailPanel();
  }

  @Override
  public ConfigPanel getConfigPanel() {
    return config;
  }

}
