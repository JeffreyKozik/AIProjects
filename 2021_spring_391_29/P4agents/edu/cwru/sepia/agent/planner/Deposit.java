package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.Unit;
import java.util.*;

/**
 * Class Representing Deposit StripsAction
 * Implements the StripsAction Interface
 */
public class Deposit implements StripsAction {
    //Represents the location of Townhall
    private Position position = null;
    private int id;

    /**
     *
     * @param x
     * @param y
     */
    public Deposit(int x, int y, int id) {
        this.position = new Position(x, y);
        this.id = id;
    }

    /**
     * Method that checks the Preconditions of this StripsAction
     * @param state GameState to check if action is applicable
     * @return
     */
    @Override
    public boolean preconditionsMet(GameState state){
        return state.getPeasant().isCarrying();
    }

    /**
     * Method that applies this action on the GameState
     * @param state State to apply action to
     * @return
     */
    @Override
    public GameState apply (GameState state){
        GameState.Peasant newPeasant = new GameState.Peasant(position, false, 0);
        newPeasant.setTypeOfResource(null);
        // not sure when we have to clone or not
        Stack<StripsAction> newStack = new Stack<>();
        newStack.addAll(state.getStripsActionStack());
        newStack.push(this);

        GameState nextGameState = new GameState(state, newPeasant, state.getResources(), newStack);
        if(state.getPeasant().getTypeOfResource() == "wood"){
            nextGameState.setRequiredWood(nextGameState.getRequiredWood() - 100);
        }
        else if(state.getPeasant().getTypeOfResource() == "gold"){
            nextGameState.setRequiredGold(nextGameState.getRequiredGold() - 100);
        }

//         TODO: set cost
        return nextGameState;
    }

    public int getId(){
        return id;
    }

    public String toString(){
        System.out.println("Deposit");
        System.out.println(position.toString());
        System.out.println(id);
        return "///////////////";
    }
}

