package sudokumodel.AgentPlacer;

import sudokumodel.SudokuEnvironment.SudokuEnvironment;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgentPlacer extends Agent {
    private int myNumber; // Angka spesifik (1-9) untuk Model 2-4. (0 jika Model 1)
    private int model;    // Model berapa yang sedang berjalan

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            myNumber = Integer.parseInt((String) args[0]); // Arg 1: number
            model = Integer.parseInt((String) args[1]);    // Arg 2: model
        } else {
            System.out.println("argumen kosong");
            doDelete();
            return;
        }

        System.out.println("PlacerAgent " + getAID().getLocalName() + "  model: " + model);
        addBehaviour(new PlacerBehaviour());
    }

    private class PlacerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Tunggu token giliran "sudoku-round"
            MessageTemplate mt = MessageTemplate.MatchConversationId("sudoku-round");
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                // --- LOGIKA UTAMA PLACER ---
                boolean success = tryToPlaceBlock();

                // Lapor hasil ke Controller
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(String.valueOf(success));
                myAgent.send(reply);
            } else {
                block();
            }
        }

        private boolean tryToPlaceBlock() {
            SudokuEnvironment env = SudokuEnvironment.getInstance();
            
            // Tentukan rentang angka yang boleh ditaruh
            // Model 1: Boleh menaruh angka 1-9 (Generic Placer)
            // Model 2: Hanya boleh menaruh 'myNumber' (Specific Placer)
            int startNum = (model == 1) ? 1 : myNumber;
            int endNum   = (model == 1) ? 9 : myNumber;

            for (int num = startNum; num <= endNum; num++) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        // Cek validasi environment (rule checker)
                        if (env.isValidMove(i, j, num)) {
                            env.placeNumber(i, j, num);
                            // Debug log
                            System.out.println(getAID().getLocalName() + " (Placer) menaruh " + num + " di [" + i + "," + j + "]");
                            return true; // Berhasil taruh 1 blok, selesai giliran
                        }
                    }
                }
            }
            return false; // Tidak ada langkah valid
        }
    }
}