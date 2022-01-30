package edu.cwru.sepia.agent.minimax;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {
    //Represent number of players
    private final int numberOfPlayers;

    /**
     * Constructor
     * @param playernum
     * @param args
     */
    public MinimaxAlphaBeta(int playernum, String[] args) {
        super(playernum);
        if (args.length < 1) {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }
        numberOfPlayers = Integer.parseInt(args[0]);
    }


    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }
    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = null;
        try {
            bestChild = alphaBetaSearch(new GameStateChild(newstate),
                    numberOfPlayers,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Next Action: ");
        bestChild.state.toString();
        System.out.println();
        return bestChild.action;
    }
    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.exit(0);
    }
    @Override
    public void savePlayerData(OutputStream os) {
    }
    @Override
    public void loadPlayerData(InputStream is) {
    }
    /**
     * You will implement this.
     * <p>
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     * <p>
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node  The action and    state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta  The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta) throws IOException {
        // follows page 8 of lecture 8 very closely
        if (depth == 0) return node;
        // ordered list of children
        List<GameStateChild> returnList = orderChildrenWithHeuristics(node.state.getChildren());
        // finds best option for max or min node
        double compareValue = node.state.getIsFootmanTurn() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        double tempValue;
        GameStateChild returnChild = null;
        GameStateChild tempChild;
        double childAlpha = alpha;
        double childBeta = beta;

        // goes through all children
        for (GameStateChild c : returnList){

            // uses recursion
            tempChild = alphaBetaSearch(c, depth - 1, childAlpha, childBeta);
            tempValue = tempChild.state.getUtility();

            if (depth == numberOfPlayers){
                tempChild.state.toString();
            }

            if (node.state.getIsFootmanTurn() && tempValue > compareValue ||
                    !node.state.getIsFootmanTurn() && tempValue < compareValue){
                compareValue = tempValue;
                returnChild = c;
            }

            // updates alpha and beta or prunes
            if (node.state.getIsFootmanTurn() && tempValue > childBeta ||
                    !node.state.getIsFootmanTurn() && tempValue < childAlpha) {
                break;
            } else if (node.state.getIsFootmanTurn()) {
                childAlpha = Math.max(childAlpha, tempValue);
            } else {
                childBeta = Math.min(childBeta, tempValue);
            }
        }

        return returnChild;
    }

    /**
     * ordering heuristic used in orderChildrenWithHeuristics
     * We do it this way because we don't want to run the whole getutility method
     * that would take too much time
     * but we use a valuable portion of the getutility function so that we can prune
     * a decent amount of states
     * This is a valuable portion of the getutility function because the distance from each footman
     * to its closest archer shows roughly how good a state is
     * the closer a footman is to an archer the better because they can only attack when they're adjacent
     * @param state
     * @return
     */
    private double orderingHeuristic(GameState state) {
        return 0;
        /*int sumDistFromFootToClosestArch = 0;

        for (GameState.MyUnitView footUV : state.getFootmanUnitViews()) {
            int distFromFootToClosestArch = Integer.MAX_VALUE;
            for (GameState.MyUnitView archUV : state.getArcherUnitViews()) {
                int tempDistFromFootToArch = Math.abs(archUV.getXPosition() - footUV.getXPosition())
                        + Math.abs(archUV.getYPosition() - footUV.getYPosition());
                if (tempDistFromFootToArch < distFromFootToClosestArch) {
                    distFromFootToClosestArch = tempDistFromFootToArch;
                }
            }
            sumDistFromFootToClosestArch += distFromFootToClosestArch;
        }
        return state.getIsFootmanTurn() ? sumDistFromFootToClosestArch : -sumDistFromFootToClosestArch;*/
    }
    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
        children.sort((a, b) -> Double.compare(orderingHeuristic(b.state), orderingHeuristic(a.state)));
        return children;
    }
}
