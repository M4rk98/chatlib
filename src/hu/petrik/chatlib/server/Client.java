package hu.petrik.chatlib.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

/**
 * A chat szerveren belül egy kliens kapcsolatot kezel.
 */
class Client {
    private Socket socket;
    private ChatServer server;
    private Object syncRoot = new Object();
    private OutputStreamWriter writer;
    
    private String nickname;
    private static Random random = new Random();
    
    /**
     * Létrehoz egy kliens objektumot a megadott socket-hez.
     * 
     * @param socket A kapcsolat a klienshez.
     * @param server A szerver objektum, amely létrehozta a klienst.
     */
    public Client(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.nickname = "User #" + random.nextLong();
    }
    
    /**
     * Fogadja a kliens által küldött üzeneteket, és továbbítja a szervernek.
     * A függvény blokkoló. Csak egyszer hívjuk meg!
     * Ha kivételt dob, feltételezhetjük, hogy a kliens kapcsolat használhatatlan.
     * 
     * @throws IOException 
     */
    public void start() throws IOException {
        
        writer = new OutputStreamWriter(socket.getOutputStream());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            
            server.send(nickname + " joined. \n");
            
            while ((line = reader.readLine()) != null) {
                String[] darabolt = line.split(" ", 2);
                
                if (darabolt.length == 0) {
                    continue;
                }
                
                String parancs = darabolt[0];
                String parameter = "";
                if (darabolt.length != 1) {
                    parameter = darabolt[1];
                }
                
                
                switch (parancs) {
                    case "/q":
                        // TODO: Üzenet
                        String quitMessage = parameter.trim();
                        server.send(nickname + " has left. (" + quitMessage + ")");
                        socket.close();
                        return;
                        
                    case "/nick":
                        String ujNicknev = parameter.trim();
                        if (!ujNicknev.equals("")) {
                            server.send("* " + nickname + " új nickneve " + ujNicknev + "\n");
                            nickname = ujNicknev;
                        }
                      
                        break;
                    case "/list":
                        String message = "Current user list: \n";
                        
                        for(Client client: server.getClients())
                        {
                            message += client.getNickname() + " \n";
                            
                        }
                        
                        server.send(message);
                        
                        break;
                        
                    default:
                        
                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                        
                        server.send("[" + sdf.format(cal.getTime()) + "] " + nickname + ": " + line + "\n");
                        break;
                }
            }
        }
    }
    
    /**
     * Elküldi a megadott üzenetet a kliensnek.
     * A függvény szálbiztos.
     * 
     * @param message
     * @throws IOException 
     */
    void send(String message) throws IOException {
        synchronized(syncRoot) {
            writer.write(message);
            writer.flush();
        }
    }
    
    /**
     * Leállítja a szervert (a hatására a {@link #start()} függvény kivételt dob).
     * A függvény szálbiztos.
     * 
     * @throws IOException 
     */
    public void stop() throws IOException {
        socket.close();
    }

    public String getNickname() {
        return nickname;
    }
    
    
}
