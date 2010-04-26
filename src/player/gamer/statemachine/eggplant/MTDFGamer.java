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
  private final long GRACE_PERIOD = 200;
  // TODO: Hashcode is NOT overridden by GDLSentence - this will only check if
  // the sentences are actually the same objects in memory


  @Override
  public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    // do nothing
    keptCache = new HashMap<MachineState, CacheValue>();

    try {
      memoizedAlphaBeta(getStateMachine(), getCurrentState(), getRole(), 0, 100, getCache(), timeout-50);
    } catch(TimeUpException e){}
  }

  /* Implements Minimax search, currently ignores clock */
  @Override
  public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
    long start = System.currentTimeMillis();

    HashMap<MachineState, CacheValue> cache = getCache();

    leafNodesSearched = statesSearched = 0;
    cacheHits = cacheMisses = 0;

    ValuedMove result = null;
    try {
      result = mtdf(getStateMachine(), getCurrentState(), getRole(), 0, 100, cache, timeout - GRACE_PERIOD);
    } catch(TimeUpException e) {
      List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
      Collections.shuffle(moves);
      result = new ValuedMove(-1, moves.get(0));
    }

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
      HashMap<MachineState, CacheValue> cache,
      long endTime)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
    int upperBound = 100;
    int lowerBound = 0;
    ValuedMove currMove = new ValuedMove(50, null); // initial guess		
    while (lowerBound < upperBound) {
      int guess = currMove.value;
      if (currMove.value == lowerBound)
        guess++;
      currMove = memoizedAlphaBeta(machine, state, role, guess - 1, guess, cache, endTime);
      if (currMove.value < guess) {
        upperBound = currMove.value;
      }
      else {
        lowerBound = currMove.value;
      }
    }
    return currMove;
  }

  private ValuedMove memoizedAlphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta,
      HashMap<MachineState, CacheValue> cache, long endTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException,
      TimeUpException{
    if (System.currentTimeMillis() > endTime)
      throw new TimeUpException();
    if (cache != null) {
      if (cache.containsKey(state)) {
        CacheValue cached = cache.get(state);
        if (alpha >= cached.alpha && beta <= cached.beta) {
          cacheHits++;
          return cached.valuedMove;
        } else {
          // Alpha-beta bounds are incompatible
          //                System.out.println("Alpha: " + alpha + "\tBeta: " + beta + "\tCached Alpha: " + cached.alpha + "\tCached Beta: " + cached.beta);
        }
      }
      cacheMisses++;
      ValuedMove result = alphaBeta(machine, state, role, alpha, beta, cache, endTime);
      cache.put(state, new CacheValue(result, alpha, beta));
      return result;
    } else {
      return alphaBeta(machine, state, role, alpha, beta, cache, endTime);
    }
  }

  private ValuedMove alphaBeta(StateMachine machine, MachineState state, Role role, int alpha, int beta, HashMap<MachineState, CacheValue> cache, long endTime)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException, TimeUpException {
    statesSearched++;
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      return new ValuedMove(machine.getGoal(state, role), null);
    }

    ValuedMove maxMove = new ValuedMove(-1, null);
    List<Move> possibleMoves = machine.getLegalMoves(state, role);
    //    Collections.shuffle(possibleMoves); // TODO: Remove this line
    for (Move move : possibleMoves) {
      List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
      //        Collections.shuffle(jointMoves); // TODO: Remove this line
      int min = 100;
      int newBeta = beta;
      for (List<Move> jointMove : jointMoves) {
        MachineState nextState = machine.getNextState(state, jointMove);
        int value = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, cache, endTime).value;
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
  /* Alternate implementation of alpha beta
  private ValuedMove memoizedAlphaBeta(
      StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
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
        if (cached.alpha > alpha)
          alpha = cached.alpha;
        if (cached.beta < beta)
          beta = cached.beta;
      }
      cacheMisses++;

      ValuedMove maxMove = alphaBeta(machine, state, role, alpha, beta, cache);
      if (maxMove.value <= alpha) {
        if (cache.containsKey(state)) {
          cache.get(state).beta = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, -1, maxMove.value));
        }
      }
      else if (maxMove.value > alpha && maxMove.value < beta) {
        if (cache.containsKey(state)) {
          cache.get(state).alpha = cache.get(state).beta = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, maxMove.value, maxMove.value));
        }
      }
      else if (maxMove.value >= beta) {
        if (cache.containsKey(state)) {
          cache.get(state).alpha = maxMove.value;
        }
        else {
          cache.put(state, new CacheValue(maxMove, maxMove.value, 101));
        }
      }
      return maxMove;
    } else {
      return alphaBeta(machine, state, role, alpha, beta, cache);
    }
  }

  private ValuedMove alphaBeta(StateMachine machine,
      MachineState state,
      Role role,
      int alpha,
      int beta,
      HashMap<MachineState, CacheValue> cache)
  throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
    statesSearched++;
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      return new ValuedMove(machine.getGoal(state, role), null);
    }

    ValuedMove maxMove = new ValuedMove(-1, null);
    if (machine.isTerminal(state)) {
      leafNodesSearched++;
      maxMove = new ValuedMove(machine.getGoal(state, role), null);
    }
    else {
      List<Move> possibleMoves = machine.getLegalMoves(state, role);
      //			Collections.shuffle(possibleMoves); // TODO: Remove this line
      for (Move move : possibleMoves) {
        List<List<Move>> jointMoves = machine.getLegalJointMoves(state, role, move);
        //				Collections.shuffle(jointMoves); // TODO: Remove this line
        int min = 101;
        int newBeta = beta;
        for (List<Move> jointMove : jointMoves) {
          MachineState nextState = machine.getNextState(state, jointMove);
          int value = memoizedAlphaBeta(machine, nextState, role, alpha, newBeta, cache).value;
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
    }
    return maxMove;
  }
   */
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

  private HashMap<MachineState, CacheValue> getCache() {
    HashMap<MachineState, CacheValue> cache = null;
    if (config.useCache()) {
      // cache = new HashMap<MachineState, CacheValue>();
      cache = keptCache;
    }
    return cache;
  }
}
