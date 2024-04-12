import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTTS //Server 
{

    public static void main(String[] args) {
        int portNumber = 58901; //port number on which the server socket will listen to incoming connections
        ExecutorService executor = Executors.newCachedThreadPool(); //manage thread execution of the program

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) //waits for and accepts connection from first player
        {
            System.out.println("Tic Tac Toe Server is Running...");
            Socket clientSocketX = serverSocket.accept(); //waits for the second player
            System.out.println("First player connected. Waiting for second player...");
            Socket clientSocketO = serverSocket.accept(); //accepts second player's connection
            System.out.println("Second player connected. Starting the game...");
            executor.submit(new Game(clientSocketX, clientSocketO)); //represents the connections to Player X and O 
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        } 
    }
}

class Game implements Runnable {

    private static final int BOARD_SIZE = 9; 
    private Player[] board;
    private Player currentPlayer;
    private List<Player> players;

    public Game(Socket clientSocketX, Socket clientSocketO) //each client initialized through socket
    {
        board = new Player[BOARD_SIZE];
        players = new ArrayList<>();
        players.add(new Player(clientSocketX, 'X')); 
        players.add(new Player(clientSocketO, 'O'));
    }

    @Override
    public void run() {
        try {
            initialize();  //initializes the game 
            play();    // runs the game 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialize() throws IOException {
        for (Player player : players) {
            player.initialize(); //sets up communication socket for each player
        }
        currentPlayer = players.get(0); //first player to join socket gets (X)
    }

    private void play() throws IOException {
        while (true) {
            for (Player player : players) {
                player.processInput();
                if (player.winner()) {
                    player.output.println("VICTORY");
                    player.opponent.output.println("DEFEAT");
                    return;
                } else if (player.fullBoard()) {
                    player.output.println("TIE");
                    player.opponent.output.println("TIE");
                    return;
                }
            }
        }
    }
    

    class Player {
        char mark;
        Player opponent;
        Socket socket;
        Scanner input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
        }

        public void initialize() throws IOException //responsible for the player's input and output streams
        {
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            
            if (mark == 'X') //the player is first
            {
                output.println("MESSAGE: Waiting for opponent to connect");
                
            }
            output.println("WELCOME " + mark);  //Welcome X and O respectively
            
            if (mark != 'X') {
                opponent = players.get(0); // Set opponent to the first player in the list
                opponent.opponent = this;
            }
        }
       

        public void processInput() {
            output.println("MESSAGE: Your move (Enter 'MOVE [0-8]')"); //prompt user for valid input
            while (true) {
                String command = input.nextLine();  //read in user's input 
           
                if (command.startsWith("MOVE")) {
                    int location = Integer.parseInt(command.substring(5)); //read the integer value entered by user 
                    if (isValidMove(location)) //if the location is valid, it is processed
                    {
                        try {
                            move(location);
                            output.println("Valid Move!");
                            opponent.output.println("Opponent Moved: " + location); //print the valid location on opponent's screen
                            break;
                        } catch (IllegalStateException e) {
                            output.println("MESSAGE: " + e.getMessage());
                        }
                    } 
                    else // if input is invalid 
                    {
                        output.println("MESSAGE: Invalid move. Please enter a valid location[0-8].");
                    }
                } 
            }
        }

        private boolean isValidMove(int location) //Checks to see if the integer is between 0-8 inclusive
        {
            return location >= 0 && location <= 8 && board[location] == null;
        }

        public void move(int location) 
        {
            board[location] = currentPlayer; //updates game board 
            currentPlayer = currentPlayer.opponent; //alternates players
        }

        public boolean winner() //checks win conditions (rows, columns, diagonals)
        {
       	 	    return (board[0] != null && board[0] == board[1] && board[0] == board[2])
                    || (board[3] != null && board[3] == board[4] && board[3] == board[5])
                    || (board[6] != null && board[6] == board[7] && board[6] == board[8])
                    || (board[0] != null && board[0] == board[3] && board[0] == board[6])
                    || (board[1] != null && board[1] == board[4] && board[1] == board[7])
                    || (board[2] != null && board[2] == board[5] && board[2] == board[8])
                    || (board[0] != null && board[0] == board[4] && board[0] == board[8]) 
                    || (board[2] != null && board[2] == board[4] && board[2] == board[6]);
      
       }

        public boolean fullBoard() //checks to see if the board if filled up to determine tie 
        {
            return Arrays.stream(board).allMatch(p -> p != null); //checks if all elements in the board are not null 
        }

   }
}