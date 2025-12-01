package sudokumodel.SudokuEnvironment;

import java.util.Stack; // Wajib import ini untuk fitur Backtrack

/**
 * Lingkungan Bersama (Shared Environment).
 * Menyimpan status papan dan riwayat langkah untuk fitur Taker/Backtrack.
 */
public class SudokuEnvironment {
    private static SudokuEnvironment instance = null;
    private int[][] board; 
    private int size = 9;
    
    // STACK: Menyimpan riwayat langkah {baris, kolom, angka}
    // Ini diperlukan agar TakerAgent bisa mengambil blok terakhir.
    private Stack<int[]> historyStack; 

    // Constructor Private
    private SudokuEnvironment() {
        board = new int[size][size];
        historyStack = new Stack<>(); 
    }

    // Singleton Pattern
    public static SudokuEnvironment getInstance() {
        if (instance == null) {
            instance = new SudokuEnvironment();
        }
        return instance;
    }

    // --- FITUR UTAMA: MENARUH BLOK (Dipakai Placer) ---
    public void placeNumber(int row, int col, int num) {
        board[row][col] = num;
        
        // PENTING: Catat langkah ini ke stack history
        historyStack.push(new int[]{row, col, num}); 
        
        System.out.println("Env: [PUT] Angka " + num + " di [" + row + "," + col + "]");
    }

    //  untuk AgentTaker
    public boolean removeLastNumber() {
        if (historyStack.isEmpty()) {
            return false; // Tidak ada yang bisa diambil (Stack kosong)
        }
        
        // 1. Ambil data langkah terakhir dari stack
        int[] lastMove = historyStack.pop();
        int row = lastMove[0];
        int col = lastMove[1];
        int val = lastMove[2];
        
        // 2. Kosongkan sel di papan (set jadi 0)
        board[row][col] = 0;
        
        System.out.println("Env: [TAKE] Angka " + val + " diambil dari [" + row + "," + col + "]");
        return true; // Berhasil ambil
    }

    public boolean isValidMove(int row, int col, int num) {
        // Cek jika sel sudah terisi
        if (board[row][col] != 0) return false;

        // Cek Baris & Kolom
        for (int i = 0; i < size; i++) {
            if (board[row][i] == num) return false;
            if (board[i][col] == num) return false;
        }

        // Cek Sub-grid 3x3
        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i + startRow][j + startCol] == num) return false;
            }
        }
        return true;
    }
    
    public void loadSamplePuzzle() {
        // Contoh load manual (bisa diganti load file)
        // board[0][0] = 5; 
        System.out.println("Env: Puzzle loaded (Empty Board for Demo).");
    }

    // Cek apakah papan penuh (Solved)
    public boolean isSolved() {
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++) {
                if (board[i][j] == 0) return false; // Masih ada yang kosong
            }
        }
        return true;
    }
    
    // Getter Board (untuk GUI atau Debug)
    public int[][] getBoard() {
        return board;
    }

    // Print status ke console
    public void printBoard() {
        System.out.println("--- Status Papan ---");
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++) {
                System.out.print((board[i][j] == 0 ? "." : board[i][j]) + " ");
            }
            System.out.println();
        }
    }
}