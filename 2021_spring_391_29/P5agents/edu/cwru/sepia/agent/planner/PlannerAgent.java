package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.AstarAgent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.*;
import java.util.*;

/**
 * Created by Devin on 3/15/15.
 */
public class PlannerAgent extends Agent {

    final int requiredWood;
    final int requiredGold;
    final boolean buildPeasants;

    // Your PEAgent implementation. This prevents you from having to parse the text file representation of your plan.
    PEAgent peAgent;

    public PlannerAgent(int playernum, String[] params) {
        super(playernum);

        if (params.length < 3) {
            System.err.println("You must specify the required wood and gold amounts and whether peasants should be built");
        }

        requiredWood = Integer.parseInt(params[0]);
        requiredGold = Integer.parseInt(params[1]);
        buildPeasants = Boolean.parseBoolean(params[2]);


        System.out.println("required wood: " + requiredWood + " required gold: " + requiredGold + " build Peasants: " + buildPeasants);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        System.out.println("initial step");
        Stack<StripsAction> plan = AstarSearch(new GameState(stateView, playernum, requiredGold, requiredWood, buildPeasants));
        System.out.println("PlannerAgent > initialStep > plan from AstarSearch " + plan.size());
        // write the plan to a text file
        savePlan(plan);

        // Instantiates the PEAgent with the specified plan.
        peAgent = new PEAgent(playernum, plan);

        return peAgent.initialStep(stateView, historyView);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        if (peAgent == null) {
            System.err.println("Planning failed. No PEAgent initialized.");
            return null;
        }
        return peAgent.middleStep(stateView, historyView);
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
        System.exit(0);
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }

    /**
     * Perform an A* search of the game graph. This should return your plan as a stack of actions. This is essentially
     * the same as your first assignment. The implementations should be very similar. The difference being that your
     * nodes are now GameState objects not MapLocation objects.
     *
     * @param startState The state which is being planned from
     * @return The plan or null if no plan is found.
     */
    private Stack<StripsAction> AstarSearch(GameState startState) {
        //System.out.println("In AstarSearch");
        //HashMap to store the closed list to make search operation constant
        HashMap<Integer, GameState> closedList = new HashMap<>();
        //Priority Queue to store the open list to get the min MapLocation in constant time
        PriorityQueue<GameState> openList = new PriorityQueue<>(GameState::compareTo);
        // Add current GameState to openList
        openList.add(startState);
        int counter = 0;
        //Loop to iterate through open list till it gets empty
        while (openList.size() > 0) {
            counter++;
//            if (counter == 100) System.exit(0);
            //System.out.println("In AstarSearch > While open list not empty");
            //System.out.println("action " + counter);
            //System.out.println(openList.peek());
            //Local variable to store the neighbour with min cost
            GameState currentGameState = openList.poll();
            //check for Townhall
            if (currentGameState.isGoal()) {
                Stack<StripsAction> reverse = new Stack<>();
                Stack<StripsAction> current = currentGameState.getStripsActionStack();
                //System.out.println("THE ANSWER IS HERE");
                while (!current.isEmpty()) {
                    reverse.push(current.pop());
                }
//                Stack<StripsAction> reverseClone = (Stack) reverse.clone();
//                while(!reverseClone.isEmpty()){
//                    System.out.println(reverseClone.pop());
//                }
                return reverse;
            }
            // add currentGameState to closed list
            closedList.put(currentGameState.hashCode(), currentGameState);
//            System.out.println();
//            System.out.println(currentGameState.toString());
            //Local variable to store the children of the current GameState
            List<GameState> childrenOfCurrentGameState = currentGameState.generateChildren();
            //System.out.println("CHIldren" + childrenOfCurrentGameState);
            //for(GameState g : childrenOfCurrentGameState){ System.out.println("NEXTG" + g.toString()); }
            // Loop to iterate over currentGameState's children
            for (GameState nextGameState : childrenOfCurrentGameState) {
                //System.out.println("Child" + nextGameState.getStripsActionStack().peek().toString());
                //If the children hasn't been seen, the sameGameState stores null otherwise it stores the GameState from the HashMap
                GameState sameGameState = closedList.get(nextGameState.hashCode());
                // the children is in the closedList
                if (sameGameState != null) {
                    // If the children has been seen before update the parent and the cost (if it's "cheaper") otherwise add it to the open list
                    if (sameGameState.getCost() > nextGameState.getCost()) {
                        sameGameState.setCost(nextGameState.getCost());
                        sameGameState.setParent(nextGameState.getParent());
                    }
                }
                //otherwise put the neighbor to OpenList
                else {
                    openList.add(nextGameState);
                }
            }
        }
        // return an empty path if no path to TownHall
        System.out.println("No path found");
        return new Stack<>();
    }

    /**
     * This has been provided for you. Each strips action is converted to a string with the toString method. This means
     * each class implementing the StripsAction interface should override toString. Your strips actions should have a
     * form matching your included Strips definition writeup. That is <action name>(<param1>, ...). So for instance the
     * move action might have the form of Move(peasantID, X, Y) and when grounded and written to the file
     * Move(1, 10, 15).
     *
     * @param plan Stack of Strips Actions that are written to the text file.
     */
    private void savePlan(Stack<StripsAction> plan) {
        if (plan == null) {
            System.err.println("Cannot save null plan");
            return;
        }

        File outputDir = new File("saves");
        outputDir.mkdirs();

        File outputFile = new File(outputDir, "plan.txt");

        PrintWriter outputWriter = null;
        try {
            outputFile.createNewFile();

            outputWriter = new PrintWriter(outputFile.getAbsolutePath());

            Stack<StripsAction> tempPlan = (Stack<StripsAction>) plan.clone();
            while(!tempPlan.isEmpty()) {
                outputWriter.println(tempPlan.pop().toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputWriter != null)
                outputWriter.close();
        }
    }
}
