package sudokumodel.AgentTaker;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sudokumodel.SudokuEnvironment.SudokuEnvironment;

public class AgentTaker extends Agent {
    private int placerFailCount = 0; // Track berapa kali Placer gagal
    
    @Override
    protected void setup() {
        System.out.println(getAID().getLocalName() + " started (Backtrack Agent)");
        addBehaviour(new TakerBehaviour());
    }

    private class TakerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
//            Template Message untuk menangani input matikan atau giliran
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
                    myAgent.doDelete();
                    return;
                }
                
                // Cek apakah ada info dari Controller tentang Placer
                if (msg.getConversationId().equals("sudoku-round") && 
                    msg.getContent().startsWith("YOUR_TURN")) {
                    
                    // Parse: "YOUR_TURN:placerFailed"
                    String[] parts = msg.getContent().split(":");
                    boolean placerFailed = false;
                    if (parts.length > 1) {
                        placerFailed = Boolean.parseBoolean(parts[1]);
                    }
                    
                    // Cek Backtracking
                    boolean success = doBacktrack(placerFailed);
                    System.out.println("Taker: Diminta backtrack? " + placerFailed);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(success));
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }

        private boolean doBacktrack(boolean placerJustFailed) {
            SudokuEnvironment env = SudokuEnvironment.getInstance();
            
            // Backtrack jika:
            // 1. placer gagal, dan
            // 2. ada history untuk di-backtrack
            
            if (!placerJustFailed) {
                // Placer berhasil, jangan backtrack
                System.out.println(getAID().getLocalName() + 
                                 " skipped (Placer is making progress)");
                return false;
            }
            
            if (!env.hasHistory()) {
                System.out.println(getAID().getLocalName() + 
                                 " cannot backtrack (history is empty)");
                return false;
            }
            
            // Lakukan backtrack
            boolean result = env.removeLastNumber();
            
            if (result) {
                System.out.println( getAID().getLocalName() + 
                                 " removed last number (Placer was stuck)");
            }
            
            return result;
        }
    }
    
    @Override
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + " shutting down");
    }
}