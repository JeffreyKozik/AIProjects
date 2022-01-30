package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class HarvestWood implements StripsAction {
    private Position startPosition;
    private Position endPosition;
    private final int k;
    private final int id;
    private GameState state;
    private List<Integer> whichPeasantsAct = null;

    public HarvestWood(Position startPosition, Position endPosition, int k, int id){
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.k = k;
        this.id = id;


    }
    public boolean preconditionsMet(GameState state) {
        this.state = state;


        ArrayList<Integer> peasantsThatAct = new ArrayList<Integer>();
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            if ((peasant.getPosition().isAdjacent(startPosition) ||
                    peasant.getPosition().equals(startPosition)) &&
                    !peasant.isCarrying()) {
                peasantsThatAct.add(peasant.getPeasantID());
                if(peasantsThatAct.size() == k){
                    this.whichPeasantsAct =  peasantsThatAct;
                    break;
                }
            }
        }



        int numberOfPeasantsThatMeetPreconditions = 0;
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            if((peasant.getPosition().isAdjacent(startPosition) || peasant.getPosition().equals(startPosition))&& !peasant.isCarrying()){
                numberOfPeasantsThatMeetPreconditions++;
            }
        }
        return numberOfPeasantsThatMeetPreconditions >= k &&
                state.getResources().get(endPosition.hashCode()).getAmount() >= k * 100;
    }

    public GameState apply(GameState state) {
        this.state = state;
        int numberOfPeasantsThatMeetPreconditions = 0;
        HashMap<Integer, GameState.MyResourceView> newResources = new HashMap<>(state.getResources());
        HashMap<Integer, GameState.Peasant> newPeasants = state.getPeasants();
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            //System.out.println("in wood for loop");
            if ((peasant.getPosition().isAdjacent(startPosition) || peasant.getPosition().equals(startPosition)) && !peasant.isCarrying()) {
                numberOfPeasantsThatMeetPreconditions++;
                //System.out.println(numberOfPeasantsThatMeetPreconditions);

                GameState.Peasant newPeasant = new GameState.Peasant(endPosition, true, peasant.getAmountCarrying() + 100, peasant.getPeasantID());
                newPeasant.setTypeOfResource("wood");
                newPeasants.put(peasant.getPeasantID(), newPeasant);

                GameState.MyResourceView chompedForest = newResources.get(endPosition.hashCode());
                chompedForest.setAmount(chompedForest.getAmount() - 100);

                if (numberOfPeasantsThatMeetPreconditions == k) {
                    // not sure when we have to clone or not
                    Stack<StripsAction> newStack = new Stack<>();
                    newStack.addAll(state.getStripsActionStack());
                    newStack.push(this);

                    GameState nextGameState = new GameState(state, newPeasants, newResources, newStack);
                    nextGameState.setParent(state);
                    return nextGameState;
                }
            }
        }
        return null;
    }

    public List<Integer> getWhichPeasantsAct(){
        return whichPeasantsAct;
    }

    public List<Integer> setWhichPeasantsAct(Integer i){
        List<Integer> peasantsThatAct = getWhichPeasantsAct();
        peasantsThatAct.remove(new Integer(i));
        System.out.println("peasantsThatAct" + peasantsThatAct);
        return peasantsThatAct;
    }

    public int getId(){
        return id;
    }

    public String toString(){
        System.out.println("Harvest Wood");
        System.out.println(endPosition.toString());
        System.out.println(id);
        if(state != null && state.getResources() != null && state.getResources().get(id)!= null) {
            System.out.println("////////dddddddddddddd" + state.getResources().get(id).getAmount());
        }
        System.out.println("k" + k);
        return "///////////////";
    }
}
