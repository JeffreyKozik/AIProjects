package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.*;
import edu.cwru.sepia.action.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;
    private static int numberOfPeasants = 0;
    private int currentPeasantId = 0;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<>();
        this.plan = plan;
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        //System.out.println("In PEAgent Initial Step");
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if (unitType.equals("peasant")) {
                peasantIdMap.put(++numberOfPeasants, unitId);
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
     * To check a unit's progress on the action they were executing last turn, you can use the following:
     * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).get(unitID).getFeedback()
     * This returns an enum ActionFeedback. When the action is done, it will return ActionFeedback.COMPLETED
     *
     * Alternatively, you can see the feedback for each action being executed during the last turn. Here is a short example.
     * if (stateView.getTurnNumber() != 0) {
     *   Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     *   for (ActionResult result : actionResults.values()) {
     *     <stuff>
     *   }
     * }
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView){
        Map<Integer, Action> sepiaActions = new HashMap<>();

        if (!plan.empty()) {
            getNewPeasantSEPIAID(stateView);
            StripsAction thisAction = plan.peek();

            int numberOfPeasantsThatHaveFinished = 0;

            List<Integer> peasantsToRemove = new ArrayList<>();
            for (Integer i : thisAction.getWhichPeasantsAct()) {
                if (stateView.getTurnNumber() == 0) continue;

                if (i == townhallId) {
                    //numberOfPeasantsThatHaveFinished++;
                    peasantsToRemove.add(i);
                }
                else {
                    ActionResult result = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).get(peasantIdMap.get(i));
                    if (result.getFeedback() == ActionFeedback.COMPLETED) {
                        //numberOfPeasantsThatHaveFinished++;
                        peasantsToRemove.add(i);
                        System.out.println("GET WHICH PEASANTS ACT" + thisAction.getWhichPeasantsAct());


                    }
                }
            }

            for(Integer j : peasantsToRemove){
                thisAction.setWhichPeasantsAct(j);
            }

            if (thisAction.getWhichPeasantsAct().isEmpty()) {
                plan.pop();
            }

            System.out.println(numberOfPeasantsThatHaveFinished + "" + thisAction.getWhichPeasantsAct().size());

            //if (numberOfPeasantsThatHaveFinished == thisAction.getWhichPeasantsAct().size()) plan.pop();

            if (!plan.empty()) {
                StripsAction action = plan.peek();

                if (action instanceof BuildPeasant){
                    sepiaActions.put(townhallId, createSepiaAction(action));
                } else {
                    // create sepia action for each peasant
                    for(Integer i : action.getWhichPeasantsAct()) {
                        currentPeasantId = peasantIdMap.get(i);
                        sepiaActions.put(currentPeasantId, createSepiaAction(action));
                    }
                }
            }
            return sepiaActions;
        }
        else {
            System.out.println("exiting the system");
            terminalStep(stateView, historyView);
        }
        return null;
    }

    /**
     * Helper Method to get the new Peasant mapped to their SEPIA ID's
     * @param stateView View of the Current State
     */
    public void getNewPeasantSEPIAID(State.StateView stateView){
        for (int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();

            if (unitType.equals("peasant") && !peasantIdMap.containsValue(unitId))
                peasantIdMap.put(++numberOfPeasants, unitId);
        }
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     *
     * Hint:
     * peasantId could be found in peasantIdMap
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action) {
        if(action instanceof HarvestGold){
            //System.out.println(action.getId());
            return Action.createCompoundGather(currentPeasantId, action.getId());
        }
        else if(action instanceof HarvestWood){
            //System.out.println("currentPeasantId" + currentPeasantId);
            //System.out.println("actionID" + action.getId());
            return Action.createCompoundGather(currentPeasantId, action.getId());
        }
        else if(action instanceof BuildPeasant){
            //System.out.println("currentPeasantId" + currentPeasantId);
            //System.out.println("actionID" + action.getId());
            return Action.createPrimitiveProduction(GameState.getTownhallUnitView().getID(), peasantTemplateId);
        }
        // if(action instanceof Deposit)
        else if (action instanceof Deposit) {
            return Action.createCompoundDeposit(currentPeasantId, action.getId());
        } else {
            //System.out.println("Not a sepia action");
            return null;
        }
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
        System.out.println("Open the pod bay doors. Success!");
        System.exit(0);
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
