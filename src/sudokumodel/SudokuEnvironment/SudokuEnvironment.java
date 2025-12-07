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
    
    public synchronized boolean hasHistory() {
        return !historyStack.isEmpty();
    }
  
    public synchronized boolean removeLastNumber() {
        if (historyStack.isEmpty()) {
            return false;
        }
        
        int[] lastMove = historyStack.pop();
        int row = lastMove[0];
        int col = lastMove[1];
        int val = lastMove[2];
        
        board[row][col] = 0;
        
        String key = row + "," + col;
        backtrackMemory.put(key, val);

        System.out.println("Env: [TAKE] Cabut " + val + " dari [" + row + "," + col + "]. Ingat next start > " + val);
        
        // Update GUI
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.updateByAgent(row, col, val, false));
        }
        return true;
    
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
        
        // Update GUI ...
        if (gui != null) {
            SwingUtilities.invokeLater(() -> gui.updateByAgent(row, col, num, isPlacing));
        }
        return true;
    }

        public void setInitialBoard(int[][] newBoard) {
        // Copy array value (Deep copy) agar aman
        for (int i = 0; i < 9; i++) {
            System.arraycopy(newBoard[i], 0, this.board[i], 0, 9);
        }

        // Clear memory
        historyStack.clear();
        backtrackMemory.clear();
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
        
        if (model == 2) {
             if (!checkSpecificRules1(row, col, val)) {
                 return false;
             }
        } else if (model == 3) {
             if (!checkSpecificRules2(row, col, val)) {
                 return false;
             }
        } else if (model == 4) {
             if (!checkSpecificRules3(row, col, val)) {
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
    
    private boolean checkSpecificRules1(int row, int col, int val) {
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;

        // Aturan A: Cek baris lain dalam sub-board yang sama
        for (int r = startRow; r < startRow + 3; r++) {
            if (r == row) continue; // Skip baris kita sendiri

            boolean conditionMet = false;

            // Syarat 1: Di baris 'r' (global) sudah ada angka 'val'?
            if (subBoardRowHasNumber(r, val)) {
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
            if (subBoardColHasNumber(c, val)) {
                conditionMet = true;
            }

            if (!conditionMet) return false;
        }

        return true;
    }
    
    private boolean checkSpecificRules2(int row, int col, int val) {
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;

        // Aturan A: Cek baris lain dalam sub-board yang sama
        for (int r = startRow; r < startRow + 3; r++) {
            if (r == row) continue; // Skip baris kita sendiri

            boolean conditionMet = false;
            
            // Syarat 2 (Model 2): Cell sejajar (r, col) tidak kosong?
            if (subBoardRowHasNumber(r, val) || board[r][col] != 0) {
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

            if (subBoardColHasNumber(c, val) || board[row][c] != 0) {
                conditionMet = true;
            }

            if (!conditionMet) return false;
        }

        return true;
    }
    
    private boolean checkSpecificRules3(int row, int col, int val) {
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;

        // Aturan A: Cek baris lain dalam sub-board yang sama
        for (int r = startRow; r < startRow + 3; r++) {
            if (r == row) continue; // Skip baris kita sendiri

            boolean conditionMet = false;

            if (subBoardRowHasNumber(r, val) || board[r][col] != 0 || isSubRowFull(r, startCol)) {
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

            if (subBoardColHasNumber(c, val) || board[row][c] != 0 || isSubColFull(c, startRow)) {
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
    
    private boolean subBoardRowHasNumber(int r, int val) {
        for (int c = 0; c < 3; c++) {
            if (board[r][c] == val) return true;
        }
        return false;
    }

    private boolean subBoardColHasNumber(int c, int val) {
        for (int r = 0; r < 3; r++) {
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
    }
    
    public void clearBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = 0;
            }
        }
        historyStack.clear();
    }
}