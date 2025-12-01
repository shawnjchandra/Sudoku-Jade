/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sudokumodel;

import sudokumodel.SudokuEnvironment.SudokuEnvironment;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 *
 * @author User
 */
public class AgentController extends Agent {
    private int model= 1; 
    private int totalRobots = 2; //test 2 

    @Override
    protected void setup() {
        System.out.println("Controller: " + getAID().getLocalName());
        
        // Load puzzle di environment
//        SudokuEnvironment.getInstance().loadSamplePuzzle();
//        SudokuEnvironment.getInstance().printBoard();

        // Mulai penjadwalan
        addBehaviour(new TokenPassingBehaviour());
    }

    private class TokenPassingBehaviour extends SimpleBehaviour {
        private int currentRobotID = 1;
        private boolean actionInRound = false; // Apakah ada progres di ronde ini?
        private int step = 0;
        private MessageTemplate mt;

        @Override
        public void action() {
            switch (step) {
                case 0: // --- KIRIM GILIRAN ---
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    // Asumsi nama agen adalah "Robot1", "Robot2", dst.
                    msg.addReceiver(new AID("Robot" + currentRobotID, AID.ISLOCALNAME));
                    msg.setContent("YOUR_TURN");
                    msg.setConversationId("sudoku-round");
                    msg.setReplyWith("req" + System.currentTimeMillis());
                    myAgent.send(msg);

                    // Siapkan template untuk menunggu balasan spesifik
//                    mt = MessageTemplate.and(
//                            MessageTemplate.MatchConversationId("sudoku-round"),
//                            MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    
                    step = 1;
                    break;

                case 1: // --- TERIMA LAPORAN ---
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            boolean success = Boolean.parseBoolean(reply.getContent());
                            if (success) {
                                actionInRound = true;
                                System.out.println("Controller: Robot" + currentRobotID + " berhasil melakukan langkah.");
                            }
                        }
                        // Pindah ke robot berikutnya
                        currentRobotID++;
                        step = 2;
                    } else {
                        block();
                    }
                    break;

                case 2: 
                    if (currentRobotID > totalRobots) {
                        System.out.println("--- Akhir Ronde ---");
                        SudokuEnvironment.getInstance().printBoard();
                        
                        if (SudokuEnvironment.getInstance().isSolved()) {
                            System.out.println("Controller: SUDOKU SELESAI!");
                            myAgent.doDelete();
                        } else if (!actionInRound) {
                            System.out.println("Controller: Tidak ada solusi (Deadlock) atau Selesai.");
                            myAgent.doDelete();
                        } else {
                            // Reset untuk ronde baru
                            currentRobotID = 1;
                            actionInRound = false;
                            step = 0;
                        }
                    } else {
                        // Lanjut ke robot berikutnya
                        step = 0;
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return false; // Behaviour ini berhenti jika agen di-kill (doDelete)
        }
    }
}
