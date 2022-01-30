package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class HarvestWood implements StripsAction {
    private Position position;
    private int id;

    public HarvestWood(Position position, int id){
        this.position = position;
        this.id = id;
    }
    public boolean preconditionsMet(GameState state) {
            return !state.getPeasant().isCarrying() &&
                    state.getResources().get(position.hashCode()).getAmount() >= 100;
    }

    public GameState apply(GameState state) {
        GameState.Peasant newPeasant = new GameState.Peasant(position, true, state.getPeasant().getAmountCarrying() + 100);
        newPeasant.setTypeOfResource("wood");
        Stack<StripsAction> newStack = new Stack<>();
        newStack.addAll(state.getStripsActionStack());
        newStack.push(this);
        // not sure if this needs to be cloned or not
        HashMap<Integer, GameState.MyResourceView> newResources = new HashMap<>(state.getResources());
        GameState.MyResourceView chompedForest = newResources.get(position.hashCode());
        chompedForest.setAmount(chompedForest.getAmount() - 100);
        GameState nextGameState = new GameState(state, newPeasant, newResources, newStack);
//         TODO: set cost
        return nextGameState;
    }

    public int getId(){
        return id;
    }

    public String toString(){
        System.out.println("Harvest Wood");
        System.out.println(position.toString());
        System.out.println(id);
        return "///////////////";
    }
}
