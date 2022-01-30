package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.AstarAgent.MapLocation;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
/*
 * @author 3 including myself names not included for privacy
 */
public class AstarAgent extends Agent {

    class MapLocation
    {
        public int x, y;
        // stores parent of MapLocation
        public MapLocation cameFrom;
        // stores path cost to MapLocation
        public float cost;


        /**
         * constructor for MapLocation class
         * @param x
         * @param y
         * @param cameFrom
         * @param cost
         */
  		public MapLocation(int x, int y, MapLocation cameFrom, float cost)
  		{
  			this.x = x;
  			this.y = y;
  			this.cameFrom = cameFrom;
  			this.cost = cost;
  		}

        /**
         * overrides object equals method, checks if two MapLocations store same x,y coordinates
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj instanceof MapLocation) {
                MapLocation that = (MapLocation) obj;
                return this.x == that.x && this.y == that.y;
            }
            return false;
        }

        /**
         * provides value that associates MapLocation with its x,y coordinates
         * Generate hashcode by Cantor Pairing function
         * @return
         */
        @Override
        public int hashCode() {
            return (int) (0.5 * (this.x + this.y) * (this.x + this.y + 1) + this.y);
        }

        /**
         * used in A* search algorithm
         * Uses Chebyshev distance to optimistically estimate true path cost
         * @param goalX
         * @param goalY
         * @return
         */
		private int heuristic(int goalX, int goalY) {
			return Math.max(Math.abs(this.x - goalX), Math.abs(this.y - goalY));
		}
    }

    Stack<MapLocation> path;
    // hashmap storing path - allows for constant time search for any map location
    HashMap<Integer, MapLocation> pathHashMap = new HashMap<>();
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y)) {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        }
        else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * You can check the position of the enemy footman with the following code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     *
     * There are more examples of getting the positions of objects in SEPIA in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
        // check if enemy exists
        if (enemyFootmanID == -1) return false;
        // store enemy location
        MapLocation enemyLocation = new MapLocation(state.getUnit(enemyFootmanID).getXPosition(), state.getUnit(enemyFootmanID).getYPosition(), null, 0);

        // create a new hashmap of path if agent replans path
        if (pathHashMap.isEmpty()){
            // iterator for iterating over the stack of currentPath
            Iterator<MapLocation> currentPathIterator = currentPath.iterator();
            MapLocation next;

            // build hashmap from stack and stop if enemy is present
             while(currentPathIterator.hasNext())
            {
                next = currentPathIterator.next();
                if(next.equals(enemyLocation))
                {
                    //Enemy found so new Path will be determined
                    pathHashMap = new HashMap<Integer,MapLocation>();
                    return true;
                }
                pathHashMap.put(next.hashCode(), next);
            }

        }

        // if enemy location is in current path
        return pathHashMap.containsKey(enemyLocation.hashCode());
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * Therefore your you need to find some possible adjacent steps which are in range
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
        //HashMap to store the closed list to make search operation constant
        HashMap<Integer, MapLocation> closedList = new HashMap<Integer, MapLocation>();
		//Priority Queue to store the closed list to get the min MapLocation in constant time
        /*
         * Overriden the non-static non-default method of interface to build anonymous class
         * Helps in building the min PriorityQueue
         * @param a
         * @param b
         * @return
         */
        PriorityQueue<MapLocation> openList;
        openList = new PriorityQueue<>(
                (a, b) -> {
                    if (a.cost + a.heuristic(goal.x, goal.y) > b.cost + b.heuristic(goal.x, goal.y))
                        return 1;
                    else if (a.cost + a.heuristic(goal.x, goal.y) == b.cost + b.heuristic(goal.x, goal.y))
                        return 0;
                    else
                        return -1;
                });

        // Add current MapLocation to openList
        openList.add(start);

 		//Loop to iterate through open list till it gets empty
 		while (openList.size() > 0) {
      //Local variable to store the neighbour with min cost
 			MapLocation currentMapLocation = openList.poll();
 			//check for Townhall
 			if(currentMapLocation.heuristic(goal.x, goal.y) == 0){
 				//build a stack of current MapLocation and it's parents
 				Stack<MapLocation> determinedPath = new Stack<MapLocation>();
 				//Don't want to store the initial MapLocation in the Path
 				currentMapLocation = currentMapLocation.cameFrom;
 				//Loop to iterate over the parent and stop when the parent is goal state
 				while(currentMapLocation.cameFrom != null){
 					determinedPath.push(currentMapLocation);
 					currentMapLocation = currentMapLocation.cameFrom;
 				}
        //If agent reached Townhall
 				return determinedPath;
 			}
 			// add currentMapLocation to closed list
 			closedList.put(currentMapLocation.hashCode(), currentMapLocation);
            // Loop to find currentMapLocation's neighbors
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                  //Skip the currentMapLocation
                    if (i == 0 && j == 0) continue;
                    //Local variable to store the neighbor of current MapLocation
                    MapLocation nextMapLocation = new MapLocation(currentMapLocation.x + i, currentMapLocation.y + j, currentMapLocation, currentMapLocation.cost + 1);
                    // Check if agent is within the bounds of the Map  and not stepping on the ResourceLocations or enemy
                    if (nextMapLocation.x < xExtent && nextMapLocation.x >= 0 &&
                            nextMapLocation.y < yExtent && nextMapLocation.y >= 0 &&
                            resourceLocations.contains(nextMapLocation) == false &&
                            !nextMapLocation.equals(enemyFootmanLoc)) {
                      //If the neighbour hasn't been seen, the sameLocation stores null otherwise it stores the MapLocation from the HashMap
                        MapLocation sameLocation = closedList.get(nextMapLocation.hashCode());
                        //check if the neighbor is in the closedList
                        if (sameLocation != null) {
                          // If the neighbor has been seen before update the parent and the cost (if it's "cheaper") otherwise add it to the open list
                            if (sameLocation.cost > nextMapLocation.cost) {
                                sameLocation.cost = nextMapLocation.cost;
                                sameLocation.cameFrom = nextMapLocation.cameFrom;
                            }
                        }
                        //otherwise put the neighbor to OpenList
                        else {
                            openList.add(nextMapLocation);
                        }
                    }
                }
            }
          }
        // return an empty path if no path to TownHall
        return new Stack<MapLocation>();
    }

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
