package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 *
 */
public class BuildPeasant implements StripsAction {

    private List<Integer> whichPeasantsAct = new ArrayList<>();
    public BuildPeasant(){
        ArrayList<Integer> peasantsThatAct = new ArrayList<Integer>();
        peasantsThatAct.add(GameState.getTownhallUnitView().getID());
        this.whichPeasantsAct = peasantsThatAct;
    }
    /**
     * Checks the preconditions of the Action
     * Checks whether the Townhall has 400 gold and food are available
     * @param state GameState to check if action is applicable
     * @return
     */
    public boolean preconditionsMet(GameState state){
        return state.getFoodAvailable() > 0 &&
                GameState.requiredGoldAtStart - state.getRequiredGold() >= 400;
    }

    /**
     * Applies the action to the state if the preconditions are met
     * Reduces the 400 gold and 1 food from the Townhall
     * @param state State to apply action to
     * @return
     */
    public GameState apply(GameState state){

        //Adding newly build Peasant to the HashMap
        HashMap<Integer, GameState.Peasant> newPeasants = state.getPeasants();
        GameState.Peasant newPeasant = new GameState.Peasant(new Position(GameState.getTownhallUnitView().getXPosition(),
                                                                            GameState.getTownhallUnitView().getYPosition()),
                                                                            false, 0,
                                                                            state.getNumberOfPeasants() + 1);

        newPeasant.setTypeOfResource(null);

        //Build the new Peasant
        newPeasants.put(state.getNumberOfPeasants() + 1, newPeasant);

        //Adding the new Action to StripsAction
        Stack<StripsAction> newStack = new Stack<>();
        newStack.addAll(state.getStripsActionStack());
        newStack.push(this);

        GameState nextGameState = new GameState(state, newPeasants, state.getResources(), newStack);
        nextGameState.setNumberOfPeasants(state.getNumberOfPeasants() + 1);
        nextGameState.setFoodAvailable(state.getFoodAvailable() - 1);
        nextGameState.setRequiredGold(state.getRequiredGold() + 400);
        return nextGameState;
    }

    public List<Integer> getWhichPeasantsAct(){
        return whichPeasantsAct;
    }


    public List<Integer> setWhichPeasantsAct(Integer i){
        List<Integer> peasantsThatAct = getWhichPeasantsAct();
        peasantsThatAct.remove(new Integer(i));
        return peasantsThatAct;
    }

    public int getId(){
        return (23*0*33433);
    }

    public String toString(){
        System.out.println("Build Peasant");
        return "///////////////";
    }
}
