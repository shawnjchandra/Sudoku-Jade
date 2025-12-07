package sudokumodel.AgentPlacer;

import sudokumodel.SudokuEnvironment.SudokuEnvironment;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Agent Placer - Menaruh angka pada Sudoku board
 * 
 * Cara kerja:
 * - Model 1: Generic Placer (bisa taruh angka 1-9)
 * - Model 2-4: Specific Placer (hanya taruh angka myNumber)
 * 
 * Arguments:
 * args[0] = myNumber (0 untuk generic, 1-9 untuk specific)
 * args[1] = model (1, 2, 3, atau 4)
 */
public class AgentPlacer extends Agent {
    private int myNumber;  // Angka yang akan ditaruh
    private int model;     // Model yang digunakan
    private boolean isRunning = true;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        // Default anggap sebagai Generic (0)
        myNumber = 0; 

        if (args != null && args.length > 0) {
            try {
                myNumber = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing argument, defaulting to Generic.");
            }
        }

        // LOGIKA PEMILIHAN BEHAVIOUR
        if (myNumber == 0) {

            System.out.println("RobotPlacer (Generic) started - Ready for Backtracking.");
            addBehaviour(new PlacerBehaviour()); 
        } else {
            System.out.println("Robot" + myNumber + " (Specific) started - Ready for Fixed Point.");
            addBehaviour(new SpecificPlacerBehaviour()); 
        }
    }
    
    private class SpecificPlacerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (msg.getContent().equals("YOUR_TURN_SPECIFIC")) {
                    
                 
                    int placedCount = tryPlaceAllPossible(myNumber);
                    

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(placedCount));
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }
    }
    
    private int tryPlaceAllPossible(int number) {
        int count = 0;
        SudokuEnvironment env = SudokuEnvironment.getInstance();
        int[][] board = env.getBoard();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    if (env.isValidMove(r, c, number)) {
                        env.placeNumber(r, c, number, true);
                        count++;
                    }
                }
            }
        }
        return count;
    }

 
    private class PlacerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mtTurn = MessageTemplate.and(
                MessageTemplate.MatchConversationId("sudoku-round"),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
            );
            
            MessageTemplate mtSystem = MessageTemplate.and(
                MessageTemplate.MatchConversationId("system"),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
            );
            
            MessageTemplate mt = MessageTemplate.or(mtTurn, mtSystem);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (msg.getConversationId().equals("system") && 
                    msg.getContent().equals("SHUTDOWN")) {
                    System.out.println(getAID().getLocalName() + " received shutdown signal");
                    isRunning = false;
                    myAgent.doDelete();
                    return;
                }
                

                if (msg.getConversationId().equals("sudoku-round") && 
                    msg.getContent().equals("YOUR_TURN")) {
                    

                    boolean success = tryToPlaceNumber();
                    

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(success));
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }

        private boolean tryToPlaceNumber() {
            SudokuEnvironment env = SudokuEnvironment.getInstance();
            
            int[] nextEmpty = findFirstEmptySpot(env.getBoard());

            if (nextEmpty == null) {
                return true; 
            }

            int row = nextEmpty[0];
            int col = nextEmpty[1];


            int lastTried = env.getStartValueFor(row, col);
            int startNum = lastTried + 1;

            System.out.println("Placer: Coba isi [" + row + "," + col + "] mulai dari " + startNum);

            for (int num = startNum; num <= 9; num++) {

                if (env.isValidMove(row, col, num)) {

                    env.placeNumber(row, col, num,true);


                    return true;
                }
            }


            System.out.println("Placer: Buntu di [" + row + "," + col + "]. Semua angka dicoba.");


            env.resetMemoryAt(row, col); 

            return false; 
        }
    }
    
    private int[] findFirstEmptySpot(int[][] board) {
 
        for (int row = 0; row < 9; row++) {

            for (int col = 0; col < 9; col++) {

                if (board[row][col] == 0) {
                    return new int[]{row, col}; 
                }
            }
        }

        return null;
    }
    
    @Override
    protected void takeDown() {
        System.out.println( getAID().getLocalName() + " shutting down");
    }
}