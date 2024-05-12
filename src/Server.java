package src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.*;
import org.json.simple.JSONObject;

class Server {
    private List<Game> games;
    private Map<JSONObject, Socket> waitingChannels;
    private ServerSocket server;
    private Socket clientChannel;
    private BufferedReader in;
    private BufferedWriter out;

    private long lastChangedID;

    private final static int BUFFER_SIZE = 256;

    private final static int IMAGE_NAME_LENGTH = 3;
    private final static String[] IMAGE_EXTENSIONS = { ".jpg", ".png", ".webp" };

    private Response requestHandler(Request req) {
        String code, meaning;
        code = "200";
        meaning = "OK";

        JSONObject jsonAnswerBody = new JSONObject();
        Game currentGame = null;
        Player player = null;

        Response res = new Response();
        try {
            Method method = req.getMethod();
            switch (method) {
                case GET:
                    String file;
                    String url = req.getUrl();

                    file = url.equals("/") ? "lab2.html" : url.substring(1, url.length());

                    int iExtension = file.lastIndexOf('.') + 1;
                    String extension = file.substring(iExtension, file.length());
                    String contentType;
                    if (extension.equals("html"))
                        contentType = "text/html";
                    else if (extension.equals("css"))
                        contentType = "text/css";
                    else if (extension.equals("js"))
                        contentType = "text/javascript";
                    else
                        throw new Exception("no such file extension");

                    String body;
                    ByteArrayOutputStream outputStream = readFile(file);
                    body = outputStream.toString();
                    body = changeImageToBase64(body);

                    res.addBody(body);
                    res.addHeader("Content-Type", contentType);
                    break;
                case POST: {
                    Object o = new JSONParser().parse(req.getBody());
                    JSONObject jsonBody = (JSONObject) o;

                    String number_player = (String) jsonBody.get("player");
                    player = Player.valueOf(number_player);
                    Long id = (Long) jsonBody.get("id");
                    for (Game game : games) {
                        if (game.ID() == id)
                            currentGame = game;
                    }

                    if (currentGame != null) {
                        JSONObject jsonActions = (JSONObject) jsonBody.get("actions");
                        Map<PlayerAction, Object> actions = new HashMap<>();
                        jsonActions.keySet().forEach((key) -> {
                            actions.put(PlayerAction.valueOf((String) key), jsonActions.get(key));
                        });
                        PlayerAction currentAction = currentGame.doAction(actions, player);
                        lastChangedID = currentGame.ID();
                        if (currentAction == PlayerAction.WAIT_OPPONENT) {
                            waitingChannels.put(jsonBody, clientChannel);
                            return null;
                        }
                        jsonAnswerBody = currentGame.jsonGameData(player);

                    } else {
                        jsonAnswerBody.put("status", PlayerStatus.GAME_ABORTED.toString());
                    }

                    jsonAnswerBody.put("player", player.toString());
                    res.addBody(jsonAnswerBody.toString());
                    res.addHeader("Content-Type", "application/json");
                    break;
                }
                case PUT:
                    boolean haveWaiting = (games.size() != 0) &&
                            games.get(0).status(Player.FIRST) == PlayerStatus.FINDING_OPPONENT.toString();
                    if (haveWaiting) {
                        Game lastGame = games.get(0);
                        currentGame = lastGame;
                        player = Player.valueOf("SECOND");

                        lastGame.connectPlayer();
                    } else {
                        Game newGame = new Game();
                        currentGame = newGame;
                        player = Player.valueOf("FIRST");

                        games.add(0, currentGame);
                    }
                    lastChangedID = currentGame.ID();
                    jsonAnswerBody = currentGame.jsonGameData(player);
                    jsonAnswerBody.put("player", player.toString());
                    res.addBody(jsonAnswerBody.toString());
                    res.addHeader("Content-Type", "application/json");
                    break;
                case DELETE:
                    Object o = new JSONParser().parse(req.getBody());
                    JSONObject jsonBody = (JSONObject) o;
                    long id = (long) jsonBody.get("id");
                    for (int i = 0; i < games.size(); i++) {
                        if (games.get(i).ID() == id) {
                            games.remove(i);
                            break;
                        }
                    }
                default:
                    code = "405";
                    meaning = "Method Not Allowed";
                    break;
            }
        } catch (Exception e) {
            code = "500";
            meaning = "Something Bad Happened";
        }

        res.addResponseCode(code, meaning);
        return res;
    }

    private void checkWaitingChannels() throws IOException {
        Socket socket = null;
        JSONObject jsonBody = null;
        for (Map.Entry<JSONObject, Socket> entry : waitingChannels.entrySet()) {
            jsonBody = entry.getKey();
            if ((Long) jsonBody.get("id") == lastChangedID) {
                socket = entry.getValue();
            }
        }

        if (socket == null)
            return;

        Game currentGame = null;
        for (Game game : games) {
            if (game.ID() == lastChangedID)
                currentGame = game;
        }

        Response res = new Response();
        Player player = Player.valueOf((String) jsonBody.get("player"));
        if (currentGame.status(player) != PlayerStatus.FINDING_OPPONENT.toString() &&
                currentGame.status(player) != PlayerStatus.WAITING_OPPONENT_MOVE.toString()) {
            JSONObject jsonAnswerBody = currentGame.jsonGameData(player);
            jsonAnswerBody.put("player", player.toString());

            res.addBody(jsonAnswerBody.toString());
            res.addHeader("Content-Type", "application/json");
            res.addResponseCode("200", "OK");
            
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(res.message());
            out.flush();
            out.close();
            socket.close();
            waitingChannels.remove(jsonBody);
        }
    }

    private String changeImageToBase64(String body) throws FileNotFoundException, IOException {
        String imageFileSubString;
        Base64.Encoder encoder = Base64.getEncoder();
        int i;

        for (String extension : IMAGE_EXTENSIONS) {
            while ((i = body.indexOf(extension)) != -1) {
                imageFileSubString = body.substring(i - IMAGE_NAME_LENGTH, i + extension.length());
                ByteArrayOutputStream baos = readFile(imageFileSubString);
                String base64jpg = encoder.encodeToString(baos.toByteArray());
                body = body.replaceAll(imageFileSubString, "data:image/jpeg;base64," + base64jpg);
            }
        }
        return body;
    }

    private ByteArrayOutputStream readFile(String file) throws FileNotFoundException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();
        return outputStream;
    }

    public void bootstrap() {
        try {
            try {
                server = new ServerSocket(80);
                while (true) {
                    clientChannel = server.accept();
                    System.out.println("new client connection");

                    StringBuilder httpMessage = new StringBuilder();

                    in = new BufferedReader(new InputStreamReader(clientChannel.getInputStream()));
                    out = new BufferedWriter(new OutputStreamWriter(clientChannel.getOutputStream()));
                    CharBuffer charBuffer = CharBuffer.allocate(BUFFER_SIZE);
                    boolean keepReading = true;

                    int readResult = -1;
                    while (keepReading) {
                        readResult = in.read(charBuffer);
                        if (readResult == -1) {
                            break;
                        }

                        keepReading = readResult == BUFFER_SIZE;

                        char[] array = Arrays.copyOfRange(charBuffer.array(), 0, readResult);
                        httpMessage.append(new String(array));

                        charBuffer.clear();
                    }

                    if (readResult == -1) {
                        continue;
                    }

                    Request req = new Request(httpMessage.toString());
                    Response res = requestHandler(req);

                    if (res != null) {
                        out.write(res.message());
                        out.flush();
                        checkWaitingChannels();
                        clientChannel.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (clientChannel != null)
                    clientChannel.close();
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Server() {
        games = new LinkedList<>();
        waitingChannels = new HashMap<>();
    };
}
