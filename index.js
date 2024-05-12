const url = "http://localhost:80/";
const timeout = 5000;

const price_warrior = 10;
const price_worker = 5;

let move = 0;
let status = "OUT_OF_GAME";
let gem = 0;
let warrior = 0;
let worker = 5;
let gameID = "no";
let player = '';
let total_price = 0;
update();

function update() {
    changeInnerHTML("gem", gem);
    changeInnerHTML("worker", worker);
    changeInnerHTML("warrior", warrior);
    changeInnerHTML("gameID", gameID);
    changeInnerHTML("status", status);
    changeInnerHTML("move", move);

    let bought_warrior = Number(document.getElementById("input-warrior").value);
    let bought_worker = Number(document.getElementById("input-worker").value);
    total_price = bought_warrior * price_warrior + bought_worker * price_worker;
    changeInnerHTML("total-price", total_price);
    setStateInputs(status);
}


function setStateInputs(status) {
    let isYourMove = status === "WAITING_YOUR_MOVE";
    let isWaiting = status === "FINDING_OPPONENT" || status === "WAITING_OPPONENT_MOVE";
    let isHigherPrice = total_price > gem;

    document.getElementById("attack-button").disabled = isWaiting || !isYourMove;
    document.getElementById("end-button").disabled = isWaiting || !isYourMove || isHigherPrice;

    document.getElementById("new-button").disabled = isWaiting || isYourMove;

    document.getElementById("input-warrior").disabled = isWaiting || !isYourMove;
    document.getElementById("input-worker").disabled = isWaiting || !isYourMove;
}

function changeInnerHTML(id, value) {
    const el = document.getElementById(id);
    el.textContent = value;
}

function handlerResponse(data) {
    move = data.move;
    status = data.status;
    gem = data.gem;
    warrior = data.warrior;
    worker = data.worker;
    gameID = data.id;
    player = data.player;
    update();
}

async function buttonHandler(button) {
    const init = {};
    init.headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    }

    if (button === "new-button")
        init.method = "put";
    else {
        init.method = "post";
        init.body = {
            id: gameID,
            player: player,
        }
        if (button === "attack-button") {
            init.body.actions = { ATTACK: true };
        }
        else if (button === "end-button") {
            let bought_warrior = Number(document.getElementById("input-warrior").value);
            let bought_worker = Number(document.getElementById("input-worker").value);
            init.body.actions = {
                BUY_WARRIORS: bought_warrior,
                BUY_WORKERS: bought_worker
            };
        }
        init.body = JSON.stringify(init.body);
    }


    let response = await fetch(url, init);
    let data = await response.json();
    handlerResponse(data);

    if (status === "FINDING_OPPONENT" || status === "WAITING_OPPONENT_MOVE") {
        let response = await fetch(url, {
            method: "post",
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                id: gameID,
                player: player,
                actions: { WAIT_OPPONENT: true }
            })
        })
        let data = await response.json();
        handlerResponse(data);
    }
}