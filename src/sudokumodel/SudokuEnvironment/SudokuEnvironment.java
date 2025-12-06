package sudokumodel.SudokuEnvironment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import javax.swing.SwingUtilities;
import sudokumodel.PuzzleFrame;

public class SudokuEnvironment {
    private static SudokuEnvironment instance = null;
    private int[][] board; 
    private int size = 9;
    private PuzzleFrame gui;
    private Stack<int[]> historyStack; 
    private Map<String, Integer> backtrackMemory = new HashMap<>();
    private int model=1;

    private SudokuEnvironment() {
        board = new int[size][size];
        historyStack = new Stack<>(); 
    }

    public static SudokuEnvironment getInstance() {
        if (instance == null) {
            instance = new SudokuEnvironment();
        }
        return instance;
    }

    public void setGui(PuzzleFrame gui) {
        this.gui = gui;
    }
    
    public void setModel (int model){
        this.model = model;
    }
    
    public int getModel () {
        return model;
    }
    
    // METHOD BARU: Get cell value
    public synchronized int getCell(int row, int col) {
        return board[row][col];
    }
    
    // METHOD BARU: Check if history exists
    public synchronized boolean hasHistory() {
        return !historyStack.isEmpty();
    }
  

    // UPDATED: Synchronized untuk thread-safety
    public synchronized boolean removeLastNumber() {
        if (historyStack.isEmpty()) {
            return false;
        }
        
        // 1. Ambil data langkah terakhir
        int[] lastMove = historyStack.pop();
        int row = lastMove[0];
        int col = lastMove[1];
        int val = lastMove[2];
        
        // 2. Kosongkan papan
        board[row][col] = 0;
        
        // 3. SIMPAN KE MEMORI: 
        // "Di koordinat ini, kita baru saja mencabut angka 'val'. 
        // Jangan coba angka <= val lagi di putaran ini."
        String key = row + "," + col;
        backtrackMemory.put(key, val);

        // 4. BERSIHKAN MEMORI MASA DEPAN
        // Jika kita mundur dari kotak 5 ke kotak 4, maka memori "gagal" 
        // di kotak 5 (dan 6, 7...) harus direset. Karena dengan angka baru di kotak 4,
        // kotak 5 mungkin punya solusi baru mulai dari 1 lagi.
        cleanForwardMemory(row, col);

        System.out.println("Env: [TAKE] Cabut " + val + " dari [" + row + "," + col + "]. Ingat next start > " + val);
        
        // Update GUI
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.updateByAgent(row, col, val, false));
        }
        return true;
    
    }
    
    private void cleanForwardMemory(int currentRow, int currentCol) {
        // Implementasi sederhana: Karena pengisian sudoku biasanya urut (kiri ke kanan, atas ke bawah),
        // Kita bisa menghapus key yang koordinatnya > current.
        // Atau cara termudah untuk backtracking murni:
        // Setiap kali Placer BERHASIL menaruh angka, dia harus mereset memori untuk sel tersebut.
        // Lihat perubahan di method placeNumber nanti.
    }

    // Method baru untuk Placer bertanya "Mulai dari angka berapa?"
    public synchronized int getStartValueFor(int row, int col) {
        String key = row + "," + col;
        return backtrackMemory.getOrDefault(key, 0);
    }
    
    // PENTING: Update method placeNumber (dipakai Placer)
    public synchronized boolean placeNumber(int row, int col, int num, boolean isPlacing) {
        // ... validasi standar sudoku ...
        if (!isValidMove(row, col, num)) return false;

        board[row][col] = num;
        historyStack.push(new int[]{row, col, num});
        
        // KUNCI SUKSES: 
        // Kalau berhasil taruh angka baru, HAPUS memori kegagalan di koordinat ini
        // supaya kalau nanti di-backtrack lagi ke sini, dia ingat angka ini.
        // TAPI lebih penting lagi: Hapus memori untuk koordinat SETELAHNYA (future cells).
        // Karena logic backtracking: ubah masa lalu = masa depan jadi baru lagi.
        // Untuk sederhananya, kita clear memori koordinat ini saat Taker mengambil, 
        // tapi Placer menimpa history lama.
        
        // Update GUI ...
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.updateByAgent(row, col, num, isPlacing));
        }
        return true;
    }

    
    
    public synchronized void resetMemoryAt(int row, int col) {
        String key = row + "," + col;
        backtrackMemory.remove(key);
    }
    
    public boolean isValidMove(int row, int col, int val) {
        // Cek jika sel sudah terisi
        if (board[row][col] != 0) return false;

        // Cek Sub-grid 3x3
        if (!isSafeStandard(row, col, val)) {
            return false;
        }
        
        if (model >= 2) {
             if (!checkSpecificRules(row, col, val)) {
                 return false;
             }
        }
        
        return true;
    }
    
    private boolean isSafeStandard(int row, int col, int val) {
        // Cek Baris & Kolom
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == val) return false;
            if (board[i][col] == val) return false;
        }
        // Cek Kotak 3x3
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[startRow + i][startCol + j] == val) return false;
            }
        }
        return true;
    }
    
    private boolean checkSpecificRules(int row, int col, int val) {
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;

        // Aturan A: Cek baris lain dalam sub-board yang sama
        for (int r = startRow; r < startRow + 3; r++) {
            if (r == row) continue; // Skip baris kita sendiri

            boolean conditionMet = false;

            // Syarat 1: Di baris 'r' (global) sudah ada angka 'val'?
            if (rowHasNumber(r, val)) {
                conditionMet = true;
            }
            // Syarat 2 (Model 2): Cell sejajar (r, col) tidak kosong?
            else if (board[r][col] != 0) {
                conditionMet = true;
            }
            // Syarat 3 (Model 3): Semua cell di baris 'r' DALAM sub-board penuh?
            else if (model == 3 && isSubRowFull(r, startCol)) {
                conditionMet = true;
            }

            // Jika untuk salah satu baris tetangga, tidak ada syarat yang terpenuhi,
            // maka kita TIDAK BOLEH menaruh angka di sini.
            if (!conditionMet) return false;
        }

        // Aturan B: Cek kolom lain dalam sub-board yang sama
        for (int c = startCol; c < startCol + 3; c++) {
            if (c == col) continue; // Skip kolom kita sendiri

            boolean conditionMet = false;

            // Syarat 1: Di kolom 'c' (global) sudah ada angka 'val'?
            if (colHasNumber(c, val)) {
                conditionMet = true;
            }
            // Syarat 2 (Model 2): Cell sejajar (row, c) tidak kosong?
            else if (board[row][c] != 0) {
                conditionMet = true;
            }
            // Syarat 3 (Model 3): Semua cell di kolom 'c' DALAM sub-board penuh?
            else if (model == 3 && isSubColFull(c, startRow)) {
                conditionMet = true;
            }

            if (!conditionMet) return false;
        }

        return true;
    }

    public boolean isSolved() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) return false;
            }
        }
        return true;
    }
    
    private boolean rowHasNumber(int r, int val) {
        for (int c = 0; c < 9; c++) {
            if (board[r][c] == val) return true;
        }
        return false;
    }

    private boolean colHasNumber(int c, int val) {
        for (int r = 0; r < 9; r++) {
            if (board[r][c] == val) return true;
        }
        return false;
    }

    // Cek apakah 3 kotak di baris r (dalam sub-grid) sudah terisi semua
    private boolean isSubRowFull(int r, int startCol) {
        for (int c = startCol; c < startCol + 3; c++) {
            if (board[r][c] == 0) return false;
        }
        return true;
    }

    // Cek apakah 3 kotak di kolom c (dalam sub-grid) sudah terisi semua
    private boolean isSubColFull(int c, int startRow) {
        for (int r = startRow; r < startRow + 3; r++) {
            if (board[r][c] == 0) return false;
        }
        return true;
    }
    
    public int[][] getBoard() {
        return board;
    }

    public void printBoard() {
        System.out.println("-------------------------");
        for (int i = 0; i < 9; i++) {
            System.out.print("| ");
            for (int j = 0; j < 9; j++) {
                int val = board[i][j];
                System.out.print(val == 0 ? "." : val);
                System.out.print(" ");
                if (j == 2 || j == 5) System.out.print("| ");
            }
            System.out.println("|");
            if (i == 2 || i == 5) {
                System.out.println("-------------------------");
            }
        }
        System.out.println("-------------------------");
        
        Iterator<int[]> it = historyStack.iterator();
        while (it.hasNext()) {
            int[] arr = it.next();
            
             for (int i = 0; i < arr.length ;i++) {
//                 System.out.print(arr[i]+" ");
             }
//                 System.out.println();
             
        }
//        System.out.println("History size: "+getHistorySize());
        
        
    }
    
    public void clearBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = 0;
            }
        }
        historyStack.clear();
    }
    
    public int getHistorySize() {
        return historyStack.size();
    }
}