package sudokumodel.AgentTaker;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sudokumodel.SudokuEnvironment.SudokuEnvironment;

public class AgentTaker extends Agent {
    
    @Override
    protected void setup() {
        addBehaviour(new TakerBehaviour());
    }

    private class TakerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("sudoku-round");
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                // --- LOGIKA UTAMA TAKER ---
                boolean success = doBacktrack();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(String.valueOf(success));
                myAgent.send(reply);
            } else {
                block();
            }
        }

        private boolean doBacktrack() {
            SudokuEnvironment env = SudokuEnvironment.getInstance();
            boolean result = env.removeLastNumber();
            if (result) {
                System.out.println(getAID().getLocalName() + " (Taker) mengambil blok terakhir.");
            } else {
                System.out.println(getAID().getLocalName() + " (Taker) gagal ambil blok (Stack kosong).");
            }
            return result;
        }
    }
}