package edu.cwru.sepia.agent.planner;

// import com.sun.xml.internal.bind.v2.TODO;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class HarvestGold implements StripsAction {
    private Position position;
    private int id;

    public HarvestGold(Position position, int id){
        this.position = position;
        this.id = id;
    }
    public boolean preconditionsMet(GameState state) {
        return !state.getPeasant().isCarrying() &&
                state.getResources().get(position.hashCode()).getAmount() >= 100;
    }

    public GameState apply(GameState state) {
        GameState.Peasant newPeasant = new GameState.Peasant(position, true, state.getPeasant().getAmountCarrying() + 100);
        newPeasant.setTypeOfResource("gold");
        Stack<StripsAction> newStack = new Stack<>();
        newStack.addAll(state.getStripsActionStack());
        newStack.push(this);
        // not sure if this needs to be cloned or not
        HashMap<Integer, GameState.MyResourceView> newResources = new HashMap<>(state.getResources());
        GameState.MyResourceView minedGoldMine = newResources.get(position.hashCode());
        minedGoldMine.setAmount(minedGoldMine.getAmount() - 100);
        GameState nextGameState = new GameState(state, newPeasant, newResources, newStack);
        nextGameState.setParent(state);
//        TODO: set cost
        return nextGameState;
    }

    public int getId(){
        return id;
    }

    public String toString(){
        System.out.println("Harvest Gold");
        System.out.println(position.toString());
        System.out.println(id);
        return "///////////////";
    }
}
