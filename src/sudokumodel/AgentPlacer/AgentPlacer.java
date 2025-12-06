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
    private int myNumber;  // Angka yang akan ditaruh (0 = semua angka untuk Model 1)
    private int model;     // Model yang digunakan
    private boolean isRunning = true;

    @Override
    protected void setup() {
        Object[] args = getArguments();

        // Default anggap sebagai Generic (0)
        myNumber = 0; 

        // Cek Argumen
        if (args != null && args.length > 0) {
            try {
                myNumber = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing argument, defaulting to Generic.");
            }
        }

        // LOGIKA PEMILIHAN BEHAVIOUR
        if (myNumber == 0) {
            // Jika angkanya 0 (atau null), berarti ini Model 1 (Generic Backtracking)
            System.out.println("RobotPlacer (Generic) started - Ready for Backtracking.");
            addBehaviour(new PlacerBehaviour()); // Pastikan nama classnya sesuai (yang logika lama)
        } else {
            // Jika angkanya 1-9, berarti ini Model 2-4 (Specific Fixed Point)
            System.out.println("Robot" + myNumber + " (Specific) started - Ready for Fixed Point.");
            addBehaviour(new SpecificPlacerBehaviour()); // Class baru untuk logika fixed point
        }
    }
    
    private class SpecificPlacerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                if (msg.getContent().equals("YOUR_TURN_SPECIFIC")) {
                    
                    // LOGIKA FIXED POINT:
                    // Robot ini (misal angka 5) akan scan SELURUH papan.
                    // Jika nemu kotak kosong yang AMAN untuk angka 5, dia isi.
                    // Dia menghitung berapa kali dia berhasil naruh angka.
                    
                    int placedCount = tryPlaceAllPossible(myNumber);
                    
                    // Lapor ke Controller berapa angka yang berhasil ditaruh
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
                // Hanya cek kotak kosong
                if (board[r][c] == 0) {
                    // Cek validitas sesuai Model yang aktif di Env
                    if (env.isValidMove(r, c, number)) {
                        // Taruh! (true = mode placing)
                        env.placeNumber(r, c, number, true);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Behaviour utama untuk menerima giliran dan menaruh angka
     */
    private class PlacerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Buat message template untuk menerima 2 jenis pesan:
            // 1. Pesan giliran (conversationId: "sudoku-round")
            // 2. Pesan shutdown (conversationId: "system")
            
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
                // Cek apakah ini shutdown request
                if (msg.getConversationId().equals("system") && 
                    msg.getContent().equals("SHUTDOWN")) {
                    System.out.println(getAID().getLocalName() + " received shutdown signal");
                    isRunning = false;
                    myAgent.doDelete();
                    return;
                }
                
                // Handle giliran normal
                if (msg.getConversationId().equals("sudoku-round") && 
                    msg.getContent().equals("YOUR_TURN")) {
                    
                    // Coba taruh angka
                    boolean success = tryToPlaceNumber();
                    
                    // Kirim balasan ke Controller
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.valueOf(success));
                    myAgent.send(reply);
                }
            } else {
                // Tidak ada pesan, block behaviour
                block();
            }
        }

        private boolean tryToPlaceNumber() {
            SudokuEnvironment env = SudokuEnvironment.getInstance();

            // 1. Cari kotak kosong pertama (Scan urut baris -> kolom)
            int[] nextEmpty = findFirstEmptySpot(env.getBoard());

            if (nextEmpty == null) {
                // Papan penuh = Solved? Biarkan Controller yang cek
                return true; 
            }

            int row = nextEmpty[0];
            int col = nextEmpty[1];

            // 2. TANYA ENV: Mulai loop dari angka berapa?
            // Jika barusan di-backtrack dan angka 9 dicabut, getStartValue akan return 9.
            // Maka startNum jadi 10 (loop berhenti).
            int lastTried = env.getStartValueFor(row, col);
            int startNum = lastTried + 1;

            System.out.println("Placer: Coba isi [" + row + "," + col + "] mulai dari " + startNum);

            for (int num = startNum; num <= 9; num++) {
                // Cek validitas aturan Sudoku (baris, kolom, kotak)
                if (env.isValidMove(row, col, num)) {
                    // Lakukan penempatan
                    env.placeNumber(row, col, num,true);

                    // Backtrack memory logic:
                    // Saat berhasil menaruh, kita harus mereset memori untuk sel berikutnya (future)
                    // Tapi karena kita pakai Map, sel berikutnya belum ada di Map, jadi aman.
                    // Yang penting: Key [row,col] di map akan tertimpa nanti kalau Taker mundur ke sini lagi.
                    
                    return true; // Sukses taruh angka
                }
            }

            // 3. JIKA LOOP SELESAI (1-9 sudah dicoba semua dan gagal)
            // Atau startNum sudah > 9
            System.out.println("Placer: Buntu di [" + row + "," + col + "]. Semua angka dicoba.");

            // PENTING:
            // Placer gagal total di kotak ini. 
            // Kita harus menyuruh Controller memanggil Taker untuk memundurkan kotak SEBELUMNYA.

            // TAPI SEBELUM ITU:
            // Kita harus mereset memori kotak INI ke 0.
            // Kenapa? Karena nanti kalau kotak SEBELUMNYA diganti angkanya,
            // kotak INI harus dicoba lagi mulai dari 1, bukan dari 9 lagi.
            env.resetMemoryAt(row, col); 

            return false; // Kirim false ke Controller -> Controller panggil Taker
        }
    }
    
    private int[] findFirstEmptySpot(int[][] board) {
        // Loop baris (0-8)
        for (int row = 0; row < 9; row++) {
            // Loop kolom (0-8)
            for (int col = 0; col < 9; col++) {
                // Jika ketemu angka 0, berarti ini kotak kosong
                if (board[row][col] == 0) {
                    return new int[]{row, col}; // Kembalikan koordinat {baris, kolom}
                }
            }
        }
        // Jika loop selesai dan tidak ketemu 0, berarti papan penuh
        return null;
    }
    
    @Override
    protected void takeDown() {
        System.out.println( getAID().getLocalName() + " shutting down");
    }
}