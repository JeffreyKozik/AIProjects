package edu.cwru.sepia.agent.minimax;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.ResourceNode.*;
import edu.cwru.sepia.environment.model.state.State.*;
import edu.cwru.sepia.util.Direction;
import java.io.IOException;
import java.util.*;
/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    /**
     * Encapsulates all of the valuable information from unitview that we want to include in our program
     */
    public class MyUnitView {
        private int id;
        private int x;
        private int y;
        private int hp;
        private int basicAttack;
        private Direction myDirection;
        /**
         * Constructor for MyUnitView
         * @param id
         * @param x
         * @param y
         * @param hp
         * @param basicAttack
         * @param myDirection
         */
        public MyUnitView(int id, int x, int y, int hp, int basicAttack, Direction myDirection) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.basicAttack = basicAttack;
            this.myDirection = myDirection;
        }
        public int getID(){
            return id;
        }
        public int getXPosition(){
            return x;
        }
        public MyUnitView setXPosition(int x){
            this.x = x;
            return this;
        }
        public int getYPosition(){
            return y;
        }
        public MyUnitView setYPosition(int y){
            this.y = y;
            return this;
        }
        public MyUnitView setHP(int hp){
            this.hp = hp;
            return this;
        }
        public int getHP() {
            return hp;
        }
        public int getBasicAttack(){return basicAttack;}
        /**
         * returns the distance using pythagorean theorem from one unitview to another
         * @param uv
         * @return
         */
        public double distanceFrom(MyUnitView uv) {
            return Math.sqrt(Math.pow(getXPosition() - uv.getXPosition(), 2) +
                    Math.pow(getYPosition() - uv.getYPosition(), 2));
        }

        public Direction getDirection(){
            return this.myDirection;
        }

        public MyUnitView setDirection(Direction myDirection){
            this.myDirection = myDirection;
            return this;
        }
    }
    // all of the footman unit views in a gamestate
    private ArrayList<MyUnitView> footmanUnitViews = new ArrayList<>();
    // all of the archer unit views in a gamestate
    private ArrayList<MyUnitView> archerUnitViews = new ArrayList<>();
    // all of the resources in a gamestate
    private Map<Integer, ResourceView> resources = new HashMap<>();
    // the stateView a game state is derived from
    private StateView state;
    //Tracks Footman Turn
    private boolean isFootmanTurn;
    // used for hashing
    private int cantorPairingFunction(int x, int y){
        return (x + y) * (x + y + 1) / 2 + y;
    }
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIds(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs belonging to the player.
     * You control player 0, the enemy controls player 1.
     *
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the Unit for a specific unit
     *
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     *
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(StateView state) {
        // store passed-in game state
        this.state = state;
        this.isFootmanTurn = true;
        //Storing UnitViews for Player 0
        for (int id : state.getUnitIds(0)) {
            footmanUnitViews.add(new MyUnitView(id, state.getUnit(id).getXPosition(),
                    state.getUnit(id).getYPosition(), state.getUnit(id).getHP(), state.getUnit(id).getTemplateView().getBasicAttack(), null));
        }
        //Storing UnitViews for Player 1
        for (int id : state.getUnitIds(1)) {
            archerUnitViews.add(new MyUnitView(id, state.getUnit(id).getXPosition(),
                    state.getUnit(id).getYPosition(), state.getUnit(id).getHP(), state.getUnit(id).getTemplateView().getBasicAttack(), null));
        }
        //Storing resources on the map
        for (int id : state.getAllResourceIds()) {
            resources.put(cantorPairingFunction(state.getResourceNode(id).getXPosition(),
                    state.getResourceNode(id).getYPosition()), state.getResourceNode(id));
        }
    }

    /**
     * Creates a new gamestate from an existing gamestate by deeply cloning everything over
     * @param gameState
     */
    public GameState(GameState gameState){
        // clones the footmanunitviews
        ArrayList<MyUnitView> newFootmanUnitViews = new ArrayList<>();
        for (MyUnitView footman : gameState.getFootmanUnitViews()){
            newFootmanUnitViews.add(new MyUnitView(footman.getID(), footman.getXPosition(),
                    footman.getYPosition(), footman.getHP(), footman.getBasicAttack(), footman.getDirection()));
        }
        // clones the archerunitviews
        ArrayList<MyUnitView> newArcherUnitViews = new ArrayList<>();
        for (MyUnitView archer : gameState.getArcherUnitViews()){
            newArcherUnitViews.add(new MyUnitView(archer.getID(), archer.getXPosition(),
                    archer.getYPosition(), archer.getHP(), archer.getBasicAttack(), archer.getDirection()));
        }
        this.footmanUnitViews = newFootmanUnitViews;
        this.archerUnitViews = newArcherUnitViews;
        this.resources = gameState.resources;
        this.state = gameState.state;
    }

    //Helper method to get the Archer Range
    public int getArcherRange(){ return state.getUnit(state.getUnitIds(1).get(0)).getTemplateView().getRange(); }
    //Helper method to get Footman views
    public ArrayList<MyUnitView> getFootmanUnitViews(){
        return this.footmanUnitViews;
    }
    //Helper method to get Archer views
    public ArrayList<MyUnitView> getArcherUnitViews(){
        return this.archerUnitViews;
    }
    //Helper method to get the Footman Turn
    public boolean getIsFootmanTurn(){
        return isFootmanTurn;
    }
    //Helper method to set the Footman Turn
    public void setIsFootmanTurn(boolean isFootmanTurn){
        this.isFootmanTurn = isFootmanTurn;
    }
    //Helper method to get all the possible directions
    public static List<Direction> getMovableDirections() {
        List<Direction> directions = new ArrayList<>();
        directions.add(Direction.NORTH);
        directions.add(Direction.SOUTH);
        directions.add(Direction.EAST);
        directions.add(Direction.WEST);
        return directions;
    }
    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        int totalArcherHP = 0;
        int totalFootmanHP = 0;

        //Loop to get the HP for all the archer
        for (MyUnitView archUV : archerUnitViews){
            totalArcherHP += archUV.getHP();
        }
        //Loop to get the HP for all the footmen
        for (MyUnitView footUV : footmanUnitViews){
            totalFootmanHP += footUV.getHP();
        }

        return totalFootmanHP - totalArcherHP;
        /*
        int distBetweenArch = 1;//start at a
        int sumDistFromFootToClosestArch = 0;
        int totalArcherHP = 1;
        int totalFootmanHP = 0;
        int obstaclesInPath = 0;

        //Loop to find the distance between archers and footman
        for (MyUnitView archUV: archerUnitViews){
            for (MyUnitView archUV2: archerUnitViews){
                distBetweenArch += Math.abs(archUV2.getXPosition() - archUV.getXPosition()) +
                        Math.abs(archUV2.getYPosition() - archUV.getYPosition());
            }
        }

        // loop to find the distance from each footman to the closest archer to it
        // sums these respective distances together
        for (MyUnitView footUV : footmanUnitViews) {
            int distFromFootToClosestArch = Integer.MAX_VALUE;
            for (MyUnitView archUV : archerUnitViews) {
                int tempDistFromFootToArch = Math.abs(archUV.getXPosition() - footUV.getXPosition())
                        + Math.abs(archUV.getYPosition() - footUV.getYPosition());
                if (tempDistFromFootToArch < distFromFootToClosestArch) {
                    distFromFootToClosestArch = tempDistFromFootToArch;
                }
            }
            sumDistFromFootToClosestArch += distFromFootToClosestArch;
        }

        //Loop to get the HP for all the archer
        for (MyUnitView archUV : archerUnitViews){
            totalArcherHP += archUV.getHP();
        }
        //Loop to get the HP for all the footmen
        for (MyUnitView footUV : footmanUnitViews){
            totalFootmanHP += footUV.getHP();
        }

        int distBtArchWt = 1;
        int distWt = 1;
        int archHPWt = 100;
        int footHPWt = 1;
        //int obWt = 1000000;


        //Return the weighted utility function values
        return (distWt * (1.0 / sumDistFromFootToClosestArch)
                + distBtArchWt * (1.0 / distBetweenArch)
                - archHPWt * (totalArcherHP / 100.0)
                + footHPWt * (totalFootmanHP / 320)
                /*- obWt * (obstaclesInPath)*/
        //);
    }

    /**
     * Helper method to find number of obstacles in given direction
     * @return
     */
    /*private int obstaclesPathHelper(Direction direction, MyUnitView footUV) {
        int xIt = 0;
        int yIt = 0;

        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            yIt = direction == Direction.NORTH ? -1 : 1;
        } else {
            xIt = direction == Direction.WEST ? -1 : 1;
        }

        int x = footUV.getXPosition();
        int y = footUV.getYPosition();
        while (inBounds(x, y)) {
            if (isArcherAt(x, y)) return 0;
            if (isResourceAt(x, y)) {
                return 1;
            }
            x += xIt;
            y += yIt;
        }
        return 0;
    }*/

    /**
     * Helper method which checks whether the given position is within the bounds of the board
     * @param x
     * @param y
     * @return
     */
    private boolean inBounds(int x, int y) {
        if(state == null)
            return false;
        return x < state.getXExtent() && x >= 0 &&
                y < state.getYExtent() && y >= 0;
    }

    /**
     * Helper method which checks the resource at the given position
     * @param x
     * @param y
     * @return
     */
    private boolean isResourceAt(int x, int y) {
        return resources.get(cantorPairingFunction(x, y)) != null;
    }

    /**
     * Helper method which checks whether the footman is at the given position
     * @param x
     * @param y
     * @return
     */
    private boolean isFootmanAt(int x, int y) {
        for (MyUnitView footman : getFootmanUnitViews()) {
            if (footman.getXPosition() == x && footman.getYPosition() == y) return true;
        }
        return false;
    }

    /**
     * Helper method which checks whether the archer is at the given position
     * @param x
     * @param y
     * @return
     */
    private boolean isArcherAt(int x, int y) {
        for (MyUnitView archer : getArcherUnitViews()) {
            if (archer.getXPosition() == x && archer.getYPosition() == y) return true;
        }
        return false;
    }

    /**
     * Helper method which returns the id of the unit at the given position
     * @param x
     * @param y
     * @return
     */
    private Integer unitAt(int x, int y) {
        for (MyUnitView archer : getArcherUnitViews()) {
            if (archer.getXPosition() == x && archer.getYPosition() == y) return archer.getID();
        }
        for (MyUnitView footman : getFootmanUnitViews()) {
            if (footman.getXPosition() == x && footman.getYPosition() == y) return footman.getID();
        }
        return null;
    }

    /**
     * Helper method which removes the unit with the given id
     * @param id
     * @return
     */
    private GameState removeUnit(int id) {
        boolean isFootman = isFootmanAt(getMyUnitViewByID(id).getXPosition(), getMyUnitViewByID(id).getYPosition());
        if (isFootman)
            getFootmanUnitViews().remove(getMyUnitViewByID(id));
        else
            getArcherUnitViews().remove(getMyUnitViewByID(id));
        return this;
    }

    /**
     * Helper method which checks whether there is any unit at the given position
     * @param x
     * @param y
     * @return
     */
    private boolean isUnitAt(int x, int y) {
        return isFootmanAt(x, y) || isArcherAt(x, y);
    }

    /**
     * Helper method which returns the unitView of the unit given by the id
     * @param id
     * @return
     */
    private MyUnitView getMyUnitViewByID(int id) {
        for (MyUnitView footmanUV : footmanUnitViews) {
            if (footmanUV.getID() == id) return footmanUV;
        }
        for (MyUnitView archerUV : archerUnitViews) {
            if (archerUV.getID() == id) return archerUV;
        }
        return null;
    }


    /**
     * returns all unit move child states
     * @param uv
     * @param stateChildren
     * @return
     * @throws IOException
     */
    private List<GameStateChild> pollinateAgentMoveStates(MyUnitView uv, List<GameStateChild> stateChildren) {
        List<GameStateChild> newStates = new ArrayList<>();
        for (GameStateChild stateChild : stateChildren) {
            GameState curState = stateChild.state;
            for (Direction direction : getMovableDirections()) {
                int newXLocation = uv.getXPosition() + direction.xComponent();
                int newYLocation = uv.getYPosition() + direction.yComponent();
                // skip location if out of bounds / occupied
                if (!curState.inBounds(newXLocation, newYLocation)) continue;
                if (curState.isResourceAt(newXLocation, newYLocation)) continue;
                if (curState.isUnitAt(newXLocation, newYLocation)) continue;
                // create new state based on child state's stateView
                GameState newState = new GameState(curState);
                newState.getMyUnitViewByID(uv.getID())
                        .setXPosition(newXLocation)
                        .setYPosition(newYLocation)
                        .setDirection(direction);
                Map<Integer, Action> actionMap = new HashMap<>(stateChild.action);
                actionMap.put(uv.getID(), Action.createPrimitiveMove(uv.getID(), direction));
                newStates.add(new GameStateChild(actionMap, newState));
            }
        }
        return newStates;
    }

    /**
     * returns all footman attack child states
     * @param uv
     * @param stateChildren
     * @return
     */
    private List<GameStateChild> pollinateFootmanAttackStates(MyUnitView uv, List<GameStateChild> stateChildren) {
        List<GameStateChild> newStates = new ArrayList<>();
        for (GameStateChild stateChild : stateChildren) {
            GameState curState = stateChild.state;
            for (Direction direction : getMovableDirections()) {
                //System.out.println(direction);
                int newXLocation = uv.getXPosition() + direction.xComponent();
                int newYLocation = uv.getYPosition() + direction.yComponent();
                if (curState.isArcherAt(newXLocation, newYLocation)) {
                    // get archerId at location
                    Integer archerId = curState.unitAt(newXLocation, newYLocation);
                    // create new state based on child state's stateView
                    GameState newState = new GameState(curState);
                    // health of archer after being attacked
                    int hp = newState.getMyUnitViewByID(archerId).getHP() - uv.getBasicAttack();
                    if (hp > 0) {
                        newState.getMyUnitViewByID(archerId).setHP(hp);
                    } else {
                        newState.removeUnit(archerId);
                    }
                    Map<Integer, Action> actionMap = new HashMap<>(stateChild.action);
                    actionMap.put(uv.getID(), Action.createPrimitiveAttack(uv.getID(), archerId));
                    newStates.add(new GameStateChild(actionMap, newState));
                }
            }
        }
        return newStates;
    }

    /**
     * returns all archer attack child states
     * @param uv
     * @param stateChildren
     * @return
     */
    private List<GameStateChild> pollinateArcherAttackStates(MyUnitView uv, List<GameStateChild> stateChildren) {
        List<GameStateChild> newStates = new ArrayList<>();
        for (GameStateChild stateChild : stateChildren) {
            GameState curState = stateChild.state;
            for (MyUnitView footmanUV : stateChild.state.footmanUnitViews) {
                // the footman is out of range
                if (footmanUV.distanceFrom(uv) > curState.getArcherRange()) continue;
                // create new state based on child state's stateView
                GameState newState = new GameState(curState);
                // health of footman after being attacked
                int hp = footmanUV.getHP() - uv.getBasicAttack();
                if (hp > 0) {
                    newState.getMyUnitViewByID(footmanUV.getID()).setHP(hp);
                } else {
                    // footman is dead
                    newState.removeUnit(footmanUV.getID());
                }

                Map<Integer, Action> actionMap = new HashMap<>(stateChild.action);
                actionMap.put(uv.getID(), Action.createPrimitiveAttack(uv.getID(), footmanUV.getID()));
                newStates.add(new GameStateChild(actionMap, newState));
            }
        }
        return newStates;


    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     *
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     *
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     *
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() throws IOException {
        List<GameStateChild> childStates = new ArrayList<>();
        List<GameStateChild> temp;
        Map <Integer, Action> actionMap = new HashMap<>();
        childStates.add(new GameStateChild(actionMap, this));

        // set next move based on whose turn it is
        if (isFootmanTurn) {
            // loop over footmen and combine footman move and attack states
            for (MyUnitView footmanUV : footmanUnitViews) {
                temp = pollinateAgentMoveStates(footmanUV, childStates);
                temp.addAll(pollinateFootmanAttackStates(footmanUV, childStates));
                childStates = temp;
            }
        } else {
            // loop over archers and combine archer move and attack states
            for (MyUnitView archerUV : archerUnitViews) {
                temp = pollinateAgentMoveStates(archerUV, childStates);
                temp.addAll(pollinateArcherAttackStates(archerUV, childStates));
                childStates = temp;
            }
        }

        for (GameStateChild c : childStates){
            c.state.setIsFootmanTurn(!getIsFootmanTurn());
        }
        return childStates;
    }
}