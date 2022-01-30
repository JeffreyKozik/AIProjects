package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.GameMap;
import edu.cwru.sepia.environment.model.state.ResourceNode.*;
import sun.util.resources.cldr.zh.CalendarData_zh_Hans_HK;

import java.util.*;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * Note that SEPIA saves the townhall as a unit. Therefore when you create a GameState instance,
 * you must be able to distinguish the townhall from a peasant. This can be done by getting
 * the name of the unit type from that unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns "townhall" or "peasant"
 *
 * You will also need to distinguish between gold mines and trees.
 * state.getResourceNode(id).getType(): returns the type of the given resource
 *
 * You can compare these types to values in the ResourceNode.Type enum:
 * ResourceNode.Type.GOLD_MINE and ResourceNode.Type.TREE
 *
 * You can check how much of a resource is remaining with the following:
 * state.getResourceNode(id).getAmountRemaining()
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
    private int requiredGold;
    private int requiredWood;
    private State.StateView state;

    //created by us
    public static Unit.UnitView townhallUnitView;
    private double cost;
    private GameState parent;
    private HashMap<Integer, Peasant> peasants = new HashMap<>();
    private Stack<StripsAction> stripsActionStack;
    private HashMap<Integer, MyResourceView> resources = new HashMap<>();

    public static int requiredGoldAtStart;
    public static int requiredWoodAtStart;

    public int getFoodAvailable() {
        return foodAvailable;
    }

    public void setFoodAvailable(int foodAvailable) {
        this.foodAvailable = foodAvailable;
    }

    private int foodAvailable;
    private int gameStateID;
    //Unique ID to represent each new GameState
    private static int numberOfGameStates = 0;
    private int numberOfPeasants;

    public String toString(){
//        System.out.println("action: " + stripsActionStack.peek().toString());
        System.out.println("required wood: " + requiredWood);
        System.out.println("required gold: " + requiredGold);
        int i = 0;
        resources.forEach((k, r) -> System.out.println(r.getId() + r.getType().toString() + ": " + r.getAmount()));
        peasants.forEach((k, p) -> System.out.println(p.getPeasantID() + "peasant" + p.getPosition().toString() + p.getAmountCarrying()));
        return "\n";
    }

    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
        this.requiredGold = requiredGold;
        this.requiredWood = requiredWood;
        requiredGoldAtStart = requiredGold;
        requiredWoodAtStart = requiredWood;
        this.state = state;
        this.cost = 0;
        this.gameStateID = ++numberOfGameStates;
        this.stripsActionStack = new Stack<>();
        this.numberOfPeasants = 1;

        for (int id : state.getAllResourceIds()) {
            ResourceView resource = state.getResourceNode(id);
            Position position = new Position(resource.getXPosition(), resource.getYPosition());
            this.resources.put(position.hashCode(),
                    new MyResourceView(position, resource.getAmountRemaining(), resource.getType(), id));
        }

        int foodProvided = 0;
        for (int id : state.getUnitIds(playernum)) {
            if (state.getUnit(id).getTemplateView().getName().equalsIgnoreCase("townhall")) {
                townhallUnitView = state.getUnit(id);
                foodProvided += state.getUnit(id).getTemplateView().getFoodProvided();
            } else if (state.getUnit(id).getTemplateView().getName().equalsIgnoreCase("peasant")) {
                this.peasants.put(1,
                        new Peasant(new Position(state.getUnit(id).getXPosition(), state.getUnit(id).getYPosition()), false, 0, 1));
            }
        }
        this.foodAvailable = foodProvided - numberOfPeasants;
    }

    /**
     *
     * @param parent
     * @param peasants
     * @param resources
     * @param stripsActionStack
     */
    public GameState(GameState parent, HashMap<Integer, Peasant> peasants, HashMap<Integer, MyResourceView> resources, Stack<StripsAction> stripsActionStack) {
        this.parent = parent;
        this.peasants = new HashMap<>(peasants);
        this.resources = new HashMap<>(resources);
        this.stripsActionStack = stripsActionStack;
        this.numberOfPeasants = peasants.size();

        double costFromParentToNow = 0;
        for (Peasant peasant : parent.peasants.values()) {
            if(peasant.position.euclideanDistance(parent.peasants.get(peasant.peasantID).position) != 0){
                costFromParentToNow = peasant.position.euclideanDistance(parent.peasants.get(peasant.peasantID).position);
                break;
            }
        }
        this.cost = parent.cost + costFromParentToNow;

        this.requiredGold = parent.requiredGold;
        this.requiredWood = parent.requiredWood;

        this.gameStateID = ++numberOfGameStates;
        this.state = parent.state;
        this.foodAvailable = parent.foodAvailable;
    }

    public static class MyResourceView {
        private int amount;
        private Type type;
        private Position position;
        private int id;

        public MyResourceView(Position position, int amount, Type type, int id){
            this.amount = amount;
            this.type = type;
            this.position = position;
            this.id = id;
        }

        public Position getPosition() {
            return position;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public int getId(){
            return this.id;
        }

        public MyResourceView clone() {
            return new MyResourceView(position, amount, type, id);
        }
    }

    public static class Peasant {
        private Position position;
        private boolean isCarrying;
        private int amountCarrying;
        private String typeOfResource;
        private int peasantID;

        public Peasant(Position position, boolean isCarrying, int amountCarrying, int peasantID) {
            this.position = position;
            this.isCarrying = isCarrying;
            this.amountCarrying = amountCarrying;
            this.typeOfResource = null;
            this.peasantID = peasantID;
        }

        public Position getPosition() {
            return this.position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public boolean isCarrying() {
            return isCarrying;
        }

        public void setCarrying(boolean carrying) {
            isCarrying = carrying;
        }

        public int getAmountCarrying() {
            return amountCarrying;
        }

        public void setAmountCarrying(int amountCarrying) {
            this.amountCarrying = amountCarrying;
        }

        public String getTypeOfResource() {
            return typeOfResource;
        }

        public void setTypeOfResource(String typeOfResource) {
            this.typeOfResource = typeOfResource;
        }

        public int getPeasantID(){ return this.peasantID; }

        public Peasant clone() {
            Peasant newPeasant = new Peasant(position, isCarrying, amountCarrying, peasantID);
            newPeasant.typeOfResource = typeOfResource;
            return newPeasant;
        }
    }

    public int getNumberOfPeasants(){
        return this.numberOfPeasants;
    }

    public int setNumberOfPeasants(int numberOfPeasants){
        this.numberOfPeasants = numberOfPeasants;
        return this.numberOfPeasants;
    }

    public static Unit.UnitView getTownhallUnitView() {
        return townhallUnitView;
    }

    /**
     * Helper method to get the parent of this Gamestate
     *
     * @return
     */
    public GameState getParent() {
        return this.parent;
    }

    /**
     * @param parent
     */
    public void setParent(GameState parent) {
        this.parent = parent;
    }

    public Stack<StripsAction> getStripsActionStack() {
        return stripsActionStack;
    }

    public void setStripsActionStack(Stack<StripsAction> stripsActionStack) {
        this.stripsActionStack = stripsActionStack;
    }

    public HashMap<Integer, MyResourceView> getResources() {
        HashMap<Integer, MyResourceView> newMap = new HashMap<>();
        this.resources.forEach((k, v) -> {
            newMap.put(k, v.clone());
        });
        return newMap;
    }

    public HashMap<Integer, Peasant> getPeasants(){
        HashMap<Integer, Peasant> newMap = new HashMap<>();
        this.peasants.forEach((k, v) -> {
            newMap.put(k, v.clone());
        });
        return newMap;
    }

    public int getRequiredGold(){
        return this.requiredGold;
    }

    public void setRequiredGold(int requiredGold){
        this.requiredGold = requiredGold;
    }

    public int getRequiredWood(){
        return this.requiredWood;
    }

    public void setRequiredWood(int requiredWood){
        this.requiredWood = requiredWood;
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return requiredGold == 0 && requiredWood == 0;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        //System.out.println("GENERATING THE CHILDREN");
        // Deposit, Harvest Forest, Harvest Gold
        List<StripsAction> possibleStripsAction = new ArrayList<>();
        List<GameState> childrenOfThisGameState = new ArrayList<>();

        for (MyResourceView resourceView : getResources().values()) {
            //System.out.println("gonig through resource views");
            for (MyResourceView resourceView2 : getResources().values()) {
                //System.out.println("gonig through resource views222222");
                //System.out.println("num of peasants" + getNumberOfPeasants());
                for (int k = 1; k <= getNumberOfPeasants(); k++) {
                    //System.out.println("gonig through number of peasants");
                    if (this.getRequiredGold() > 0 && resourceView2.getType() == ResourceNode.Type.GOLD_MINE)
                        possibleStripsAction.add(new HarvestGold(resourceView.getPosition(), resourceView2.getPosition(), k, resourceView2.getId()));
                    else if (this.getRequiredWood() > 0 && resourceView2.getType() == ResourceNode.Type.TREE)
                        possibleStripsAction.add(new HarvestWood(resourceView.getPosition(), resourceView2.getPosition(), k, resourceView2.getId()));
                }
            }

            // Starting at townhall
            for (int k = 1; k <= getNumberOfPeasants(); k++) {
                if (this.getRequiredGold() > 0 && resourceView.getType() == ResourceNode.Type.GOLD_MINE)
                    possibleStripsAction.add(new HarvestGold(new Position(townhallUnitView.getXPosition(), townhallUnitView.getYPosition()), resourceView.getPosition(), k, resourceView.getId()));
                else if (this.getRequiredWood() > 0 && resourceView.getType() == ResourceNode.Type.TREE)
                    possibleStripsAction.add(new HarvestWood(new Position(townhallUnitView.getXPosition(), townhallUnitView.getYPosition()), resourceView.getPosition(), k, resourceView.getId()));
            }

            // Going back to townhall
            for (int k = 1; k <= getNumberOfPeasants(); k++) {
                possibleStripsAction.add(new Deposit(resourceView.getPosition(), new Position(townhallUnitView.getXPosition(), townhallUnitView.getYPosition()), k, townhallUnitView.getID()));
            }
        }

        /*
        for (MyResourceView resourceView : getResources().values()) {
            if (this.getRequiredGold() > 0 && resourceView.getType() == ResourceNode.Type.GOLD_MINE)
                possibleStripsAction.add(new HarvestGold(resourceView.getPosition(), resourceView.getId()));
            else if (this.getRequiredWood() > 0 && resourceView.getType() == ResourceNode.Type.TREE)
                possibleStripsAction.add(new HarvestWood(resourceView.getPosition(), resourceView.getId()));
        }

        possibleStripsAction.add(new Deposit(new Position(townhallUnitView.getXPosition(), townhallUnitView.getYPosition()), townhallUnitView.getID()));
        */

        possibleStripsAction.add(new BuildPeasant());

        for (StripsAction action : possibleStripsAction) {
            //System.out.println("axtion" + action.toString());
            //Check the preconditions of all the possible actions
            if (action.preconditionsMet(this)) {
                childrenOfThisGameState.add(action.apply(this));
            }
        }

        //Applicable Actions are applied on this GameState to generate all possible game states
        //Generate the list of possible children games states
        return childrenOfThisGameState;

        // Check if agent is within the bounds of the Map  and not stepping on the ResourceLocations or enemy
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     * <p>
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
//        double nearestForestDistance = Double.MAX_VALUE;
//        double nearestGoldMineDistance = Double.MAX_VALUE;
//        int tripsToGoldMineRequired = requiredGold / 100;
//        int tripsToForestRequired = requiredWood / 100;
//        ResourceNode.ResourceView resourceView;
//        Position resourcePosition;
//        Position townhallPosition = new Position(townhallUnitView.getXPosition(), townhallUnitView.getYPosition());
//        int tempDistance;
//
//
//        //Minimal distance from Townhall to Resources
//        for (int resourceId : state.getAllResourceIds()) {
//            resourceView = state.getResourceNode(resourceId);
//            resourcePosition = new Position(resourceView.getXPosition(), resourceView.getYPosition());
//            tempDistance = resourcePosition.chebyshevDistance(townhallPosition);
//
//            if (resourceView.getType() == ResourceNode.Type.GOLD_MINE && tempDistance < nearestGoldMineDistance) {
//                nearestGoldMineDistance = tempDistance;
//            } else if (resourceView.getType() == ResourceNode.Type.TREE && nearestForestDistance > tempDistance) {
//                nearestForestDistance = tempDistance;
//
//            }
//        }
//        return requiredGold / 100 * nearestGoldMineDistance + requiredWood / 100  * nearestForestDistance;
        // return (requiredWood + requiredGold) / (numberOfPeasants); hmmmmm make this admissible and smart
        return 0;
    }

    /**
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost(){
        return cost;
    }

    /**
     *
     * @param cost
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        return Double.compare(this.getCost() + this.heuristic(), o.getCost() + o.heuristic());
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return false;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        return gameStateID;
    }
}


