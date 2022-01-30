package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.Unit;
import java.util.*;

/**
 * Class Representing Deposit StripsAction
 * Implements the StripsAction Interface
 */
public class Deposit implements StripsAction {
    // maybe I don't need startPosition thing or I have to change it somehow I think

    //Represents the location the peasant is coming from
    private Position startPosition;
    //Represents the location of Townhall
    private Position endPosition;
    // number of peasants
    private int k;
    private int id;
    private GameState state;
    private List<Integer> whichPeasantsAct = null;

    /**
     *
     * @param startPosition
     * @param endPosition
     */
    public Deposit(Position startPosition, Position endPosition, int k, int id) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.k = k;
        this.id = id;

    }

    /**
     * Method that checks the Preconditions of this StripsAction
     * @param state GameState to check if action is applicable
     * @return
     */
    @Override
    public boolean preconditionsMet(GameState state){
        this.state = state;

        ArrayList<Integer> peasantsThatAct = new ArrayList<Integer>();
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            if((peasant.getPosition().isAdjacent(startPosition) ||
                    peasant.getPosition().equals(startPosition)) &&
                    peasant.isCarrying()){
                peasantsThatAct.add(peasant.getPeasantID());
                if(peasantsThatAct.size() == k){
                    this.whichPeasantsAct = peasantsThatAct;
                    break;
                }
            }
        }



        int numberOfPeasantsThatMeetPreconditions = 0;
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            if((peasant.getPosition().isAdjacent(startPosition) || peasant.getPosition().equals(startPosition)) && peasant.isCarrying()){
                numberOfPeasantsThatMeetPreconditions++;
            }
        }
        //System.out.println("deposit preconditions met or not: " + (numberOfPeasantsThatMeetPreconditions >=k));
        return numberOfPeasantsThatMeetPreconditions >= k;
    }

    /**
     * Method that applies this action on the GameState
     * @param state State to apply action to
     * @return
     */
    @Override
    public GameState apply (GameState state){
        //System.out.println("I JUst DePOSITeddddddddddddddddddddddddddddddd");
        int numberOfPeasantsThatMeetPreconditions = 0;
        HashMap<Integer, GameState.Peasant> newPeasants = state.getPeasants();
        for (GameState.Peasant peasant : state.getPeasants().values()) {
            if((peasant.getPosition().isAdjacent(startPosition) || peasant.getPosition().equals(startPosition)) && peasant.isCarrying()){
                numberOfPeasantsThatMeetPreconditions++;

                GameState.Peasant newPeasant = new GameState.Peasant(endPosition, false, 0, peasant.getPeasantID());
                newPeasant.setTypeOfResource(null);
                newPeasants.put(peasant.getPeasantID(), newPeasant);

                if(numberOfPeasantsThatMeetPreconditions == k){
                    //System.out.println("GOT TO KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
                    //System.out.println("Type of resource is " + peasant.getTypeOfResource());
                    // not sure when we have to clone or not
                    Stack<StripsAction> newStack = new Stack<>();
                    newStack.addAll(state.getStripsActionStack());
                    newStack.push(this);

                    GameState nextGameState = new GameState(state, newPeasants, state.getResources(), newStack);
                    if(peasant.getTypeOfResource().equals("wood")){
                        //System.out.println("Just deposited a ton of wood" + k);
                        nextGameState.setRequiredWood(nextGameState.getRequiredWood() - (k * 100));
                    }
                    else if(peasant.getTypeOfResource().equals("gold")){
                        //System.out.println("Just deposited a ton of gold" + k);
                        nextGameState.setRequiredGold(nextGameState.getRequiredGold() - (k * 100));
                    }

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
        System.out.println("Deposit");
        System.out.println(endPosition.toString());
        System.out.println(id);
        System.out.println("k" + k);
        return "///////////////";
    }
}

