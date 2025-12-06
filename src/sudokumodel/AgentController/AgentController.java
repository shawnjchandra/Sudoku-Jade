package sudokumodel.AgentController;

import sudokumodel.SudokuEnvironment.SudokuEnvironment;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentController extends Agent {
    private int model = 1;
    private String[] robotNames;

    @Override
    protected void setup() {
        // Ambil Model dari Environment
        int currentModel = SudokuEnvironment.getInstance().getModel();
        System.out.println("Controller start. Detected Model: " + currentModel);

        if (currentModel == 1) {
            // MODEL 1: BACKTRACKING (Logic lama kita)
            addBehaviour(new TokenPassingBehaviour());
        } else {
            // MODEL 2-4: FIXED POINT ITERATION (Logic baru)
            addBehaviour(new FixedPointBehaviour());
        }
    }

    private class FixedPointBehaviour extends SimpleBehaviour {
        private int currentRobot = 1; // Mulai dari Robot 1
        private int totalMovesInRound = 0; // Menghitung perubahan di 1 putaran penuh
        private int roundNumber = 1;
        private int step = 0;
        private String replyWithKey;

        @Override
        public void action() {
            switch (step) {
                case 0: // KIRIM REQUEST KE ROBOT i
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    // Panggil agen spesifik: "Robot1", "Robot2", dst.
                    msg.addReceiver(new AID("Robot" + currentRobot, AID.ISLOCALNAME));
                    msg.setContent("YOUR_TURN_SPECIFIC");

                    replyWithKey = "req-iter-" + System.currentTimeMillis();
                    msg.setReplyWith(replyWithKey);

                    System.out.println("Round " + roundNumber + ": Memanggil Robot" + currentRobot);
                    myAgent.send(msg);
                    step = 1;
                    break;

                case 1: // TERIMA LAPORAN
                    MessageTemplate mt = MessageTemplate.MatchInReplyTo(replyWithKey);
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {
                        // Robot melapor berapa angka yang dia taruh
                        int moves = Integer.parseInt(reply.getContent());
                        totalMovesInRound += moves;

                        if (moves > 0) {
                            System.out.println(" -> Robot" + currentRobot + " menaruh " + moves + " angka.");
                        }

                        // Lanjut ke robot berikutnya
                        currentRobot++;
                        step = 0;

                        // CEK APAKAH SUDAH SELESAI 1 PUTARAN (Robot 1-9 sudah jalan semua?)
                        if (currentRobot > 9) {
                            step = 2; // Evaluasi Akhir Ronde
                        }
                    } else {
                        block();
                    }
                    break;

                case 2: // EVALUASI FIXED POINT (Termination Condition)
                    System.out.println("=== END OF ROUND " + roundNumber + " ===");
                    System.out.println("Total perubahan di papan: " + totalMovesInRound);

                    // LOGIKA TERMINATION:
                    // Jika dalam satu putaran penuh (Robot 1-9) TIDAK ADA yang menaruh angka (0
                    // moves),
                    // berarti kondisi "Fixed Point" tercapai. Papan tidak akan berubah lagi.

                    if (totalMovesInRound == 0) {
                        System.out.println("STOP. Tidak ada perubahan lagi (Converged).");

                        if (SudokuEnvironment.getInstance().isSolved()) {
                            System.out.println("RESULT: SOLVED! 🎉");
                        } else {
                            System.out.println("RESULT: STUCK / PARTIAL SOLUTION (Sifat Model 2-4)");
                        }

                        // Matikan semua agen
                        killAllAgents9();
                        myAgent.doDelete();
                    } else {
                        // Jika masih ada perubahan, LANJUT ronde baru
                        System.out.println("Masih ada progress. Lanjut Ronde " + (roundNumber + 1));
                        roundNumber++;
                        currentRobot = 1; // Reset ke Robot 1
                        totalMovesInRound = 0; // Reset counter
                        step = 0; // Ulang loop
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return false;
        }

        // Helper untuk matikan Robot1 - Robot9
        private void killAllAgents9() {
            ACLMessage bye = new ACLMessage(ACLMessage.REQUEST);
            bye.setContent("SHUTDOWN"); // Pastikan agen punya handler shutdown
            for (int i = 1; i <= 9; i++) {
                bye.addReceiver(new AID("Robot" + i, AID.ISLOCALNAME));
            }
            myAgent.send(bye);
        }
    }

    private void initRobotNames() {
        if (model == 1) {
            // Model 1: RobotPlacer dan RobotTaker
            robotNames = new String[] { "RobotPlacer", "RobotTaker" };
        } else {
            // Model 2-4: Robot1 sampai Robot9
            robotNames = new String[9];
            for (int i = 0; i < 9; i++) {
                robotNames[i] = "Robot" + (i + 1);
            }
        }
    }

    private class TokenPassingBehaviour extends SimpleBehaviour {
        private int step = 0;
        private String currentReplyWith;
        private int movesCount = 0;
        private String targetAgent = "RobotPlacer";

        @Override
        public void action() {
            switch (step) {

                case 0: // KIRIM PERINTAH
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID(targetAgent, AID.ISLOCALNAME));
                    msg.setConversationId("sudoku-round");

                    // Logika Pesan: Beri tahu konteks ke agen
                    if (targetAgent.equals("RobotTaker")) {
                        // Jika kita memanggil Taker, berarti Placer PASTI baru saja gagal
                        msg.setContent("YOUR_TURN:true"); // true = placerFailed
                    } else {
                        // Jika memanggil Placer, suruh dia jalan normal
                        msg.setContent("YOUR_TURN");
                    }

                    currentReplyWith = "req-" + targetAgent + "-" + System.currentTimeMillis();
                    msg.setReplyWith(currentReplyWith);

                    System.out.println("Controller: Meminta " + targetAgent + " untuk jalan.");
                    myAgent.send(msg);

                    step = 1;
                    break;

                case 1: // TERIMA LAPORAN & TENTUKAN LANGKAH BERIKUTNYA
                    MessageTemplate mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("sudoku-round"),
                            MessageTemplate.MatchInReplyTo(currentReplyWith));

                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            boolean success = Boolean.parseBoolean(reply.getContent());
                            String senderName = reply.getSender().getLocalName();

                            // --- LOGIKA INTI BACKTRACKING ---

                            if (senderName.equals("RobotPlacer")) {
                                if (success) {
                                    System.out.println(">> Placer SUKSES taruh angka. Lanjut Placer lagi.");
                                    // Jika sukses, lanjut isi kotak berikutnya (tetap Placer)
                                    targetAgent = "RobotPlacer";
                                    movesCount++;
                                } else {
                                    System.out.println(">> Placer GAGAL (Buntu). Panggil Taker.");
                                    // Jika gagal, WAJIB panggil Taker untuk mundur
                                    targetAgent = "RobotTaker";
                                }
                            } else if (senderName.equals("RobotTaker")) {
                                if (success) {
                                    System.out.println(">> Taker SUKSES mundur. Coba Placer lagi.");
                                    // Setelah mundur, panggil Placer untuk coba angka lain
                                    targetAgent = "RobotPlacer";
                                } else {
                                    System.out.println(">> Taker GAGAL (Tidak bisa mundur lagi). No Solution.");
                                    // Deadlock total
                                    killAllAgents();
                                    myAgent.doDelete();
                                    return;
                                }
                            }

                            // Cek kondisi selesai (Sudoku Solved)
                            if (SudokuEnvironment.getInstance().isSolved()) {
                                System.out.println("\n SUDOKU SOLVED! 🎉 dalam " + movesCount + " langkah");
                                killAllAgents();
                                myAgent.doDelete();
                                return;
                            }

                            // Kembali ke langkah kirim pesan
                            step = 0;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        private void killAllAgents() {
            System.out.println("\nShutting down all agents...");
            for (String robotName : robotNames) {
                ACLMessage killMsg = new ACLMessage(ACLMessage.REQUEST);
                killMsg.addReceiver(new AID(robotName, AID.ISLOCALNAME));
                killMsg.setContent("SHUTDOWN");
                killMsg.setConversationId("system");
                myAgent.send(killMsg);
            }
        }

        @Override
        public boolean done() {
            return false;
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Controller shutting down...");
    }
}