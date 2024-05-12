package src;

import java.util.Map;
import org.json.simple.JSONObject;

public class Game {
    private final int SLAVE_PRICE = 5;
    private final int WARRIOR_PRICE = 10;

    private long id;
    private int move;

    private PlayerStatus[] status = new PlayerStatus[2];
    private int[] warrior = new int[2];
    private int[] worker = new int[2];
    private int[] gem = new int[2];

    public Game() {
        warrior[0] = warrior[1] = 0;
        gem[0] = gem[1] = 0;
        worker[0] = worker[1] = 5;

        move = 0;
        status[0] = PlayerStatus.FINDING_OPPONENT;
        id = System.currentTimeMillis();
    }

    public void setStatusEndOfGame(){
        boolean isDraw = warrior[0] == warrior[1];
            if (isDraw) {
                status[0] = PlayerStatus.DRAW;
                status[1] = PlayerStatus.DRAW;
            } else {
                boolean isWinner = warrior[0] > warrior[1];
                status[0] = isWinner ? PlayerStatus.YOU_WIN : PlayerStatus.YOU_LOSE;
                status[1] = !isWinner ? PlayerStatus.YOU_WIN : PlayerStatus.YOU_LOSE;
            }
    } 

    public PlayerAction doAction(Map<PlayerAction, Object> actions, Player player) throws Exception {
        int i = player.ordinal();
        int iOpponent = i ^ 1;
        if (status[i] == PlayerStatus.ATTACKED) {
            setStatusEndOfGame();
            return PlayerAction.ATTACK;
        } else if (actions.containsKey(PlayerAction.WAIT_OPPONENT) &&
                (Boolean) actions.get(PlayerAction.WAIT_OPPONENT) == true) {
            return PlayerAction.WAIT_OPPONENT;
        } else if (actions.containsKey(PlayerAction.ATTACK) &&
                (Boolean) actions.get(PlayerAction.ATTACK) == true) {
                    if(status[iOpponent] == PlayerStatus.WAITING_YOUR_MOVE){
                        status[iOpponent] = PlayerStatus.ATTACKED;
                        status[i] = PlayerStatus.WAITING_OPPONENT_MOVE;
                    }
                    else{
                        setStatusEndOfGame();
                    }
            return PlayerAction.ATTACK;
        } else if (actions.containsKey(PlayerAction.BUY_WORKERS) &&
                actions.containsKey(PlayerAction.BUY_WARRIORS)) {

            long boughtSlaves = (Long) actions.get(PlayerAction.BUY_WORKERS);
            worker[i] += boughtSlaves;
            gem[i] -= boughtSlaves * SLAVE_PRICE;

            long boughtWarriors = (Long) actions.get(PlayerAction.BUY_WARRIORS);
            warrior[i] += boughtWarriors;
            gem[i] -= boughtWarriors * WARRIOR_PRICE;

            if (status[iOpponent] == PlayerStatus.WAITING_YOUR_MOVE) {
                status[i] = PlayerStatus.WAITING_OPPONENT_MOVE;
            } else if (status[iOpponent] == PlayerStatus.WAITING_OPPONENT_MOVE) {
                status[i] = PlayerStatus.WAITING_YOUR_MOVE;
                status[iOpponent] = PlayerStatus.WAITING_YOUR_MOVE;
                gem[i] += worker[i];
                gem[iOpponent] += worker[iOpponent];
                move++;
            }
            return PlayerAction.BUY_WARRIORS;
        } else {
            throw new Exception("actions are not valid");
        }
    }

    public int gem(Player player) {
        int i = player.ordinal();
        return gem[i];
    }

    public int warrior(Player player) {
        int i = player.ordinal();
        return warrior[i];
    }

    public int worker(Player player) {
        int i = player.ordinal();
        return worker[i];
    }

    public long ID() {
        return id;
    }

    public int move() {
        return move;
    }

    public String status(Player player) {
        int i = player.ordinal();
        return status[i].toString();
    }

    public void connectPlayer() {
        status[0] = PlayerStatus.WAITING_YOUR_MOVE;
        status[1] = PlayerStatus.WAITING_YOUR_MOVE;
    }

    public JSONObject jsonGameData(Player player) {
        JSONObject json = new JSONObject();
        json.put("id", ID());
        json.put("move", move());
        json.put("warrior", warrior(player));
        json.put("gem", gem(player));
        json.put("worker", worker(player));
        json.put("status", status(player));
        return json;
    }

}

enum PlayerStatus {
    WAITING_YOUR_MOVE,
    WAITING_OPPONENT_MOVE,

    FINDING_OPPONENT,

    YOU_WIN,
    YOU_LOSE,
    DRAW,

    ATTACKED,

    GAME_ABORTED
}

enum PlayerAction {
    BUY_WARRIORS,
    BUY_WORKERS,

    ATTACK,
    WAIT_OPPONENT
}

enum Player {
    FIRST,
    SECOND
}