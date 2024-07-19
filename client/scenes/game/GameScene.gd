extends Node2D

@onready var _hand = %Hand
@onready var _table = %Table
@onready var _players = %Players
@onready var _points_ns_label = %PointsNS
@onready var _points_we_label = %PointsWE
@onready var _trump_label = %Trump
@onready var _choose_trump_gui = %ChooseTrump
@onready var _choose_weis_gui = %ChooseWeis
@onready var _show_weis_gui = %ShowWeis
@onready var _game_finished_gui = %GameFinished
@onready var _status_label = %StatusLabel
@onready var _confetti = %Confetti

var position_icon_tweens: Array
var possible_weise: Array
var active_position: Players.PositionsEnum
var state: String
var trump: String
var points: Dictionary
var socket_actions: Dictionary = {
	"CardPlayed": _on_card_played,
	"ClearPlayedCards": _on_clear_cards,
	"UpdateHand": _on_update_hand,
	"UpdateState": _on_update_state,
	"UpdateActive": _on_update_active_position,
	"UpdateTrump": _on_update_trump,
	"UpdatePoints": _on_update_points,
	"PlayerJoined": _on_player_joined,
	"PlayerDisconnected": _on_player_disconnected,
	"GameFinished": _on_game_finished,
	"UpdatePossibleWeise": _on_update_possible_weise,
	"ShowWeis": _on_show_weis,
}
var trumps: Dictionary = {
	"SPADES": "♠️",
	"HEARTS": "❤️",
	"DIAMONDS": "♦️",
	"CLUBS": "♣️",
	"UNEUFE": "⬆️️",
	"OBEABE": "⬇️️",
	"FREESTYLE": "🆓",
}

func _input(event):
	if event is InputEventKey and event.pressed and OS.is_debug_build():
		if event.keycode == KEY_Q:
			Player.socket_seat_unsubscribe(Player.game_init_data["seat"]["uuid"])
			SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")

func _ready():
	_hand.game_scene = self
	_players.game_scene = self
	_table.game_scene = self
	_choose_trump_gui.pass_additional_params({"game_scene": self})
	_choose_weis_gui.pass_additional_params({"game_scene": self})
	_show_weis_gui.pass_additional_params({"game_scene": self})
	
	Player.game_scene = self
	Player.position = Players.PositionsEnum[Player.game_init_data["seat"]["position"]]
	
	_on_update_possible_weise(Player.game_init_data["seat"])
	_on_update_active_position({"position": Player.game_init_data["seat"]["activePosition"]})
	_on_update_state(Player.game_init_data["seat"])
	_on_update_trump(Player.game_init_data["seat"])
	_on_update_points(Player.game_init_data["seat"])
	Player.game_init_data["otherPlayers"].map(func(player): _on_player_joined({"player": player}))
	
	# If we got some players already in the game at login we'll 
	# show them here including any cards they might"ve played
	for card in Player.game_init_data["cardsPlayed"]:
		_players.play(
			card["position"],
			card["suit"], 
			card["rank"],
			card["skin"]
		)
		
	_hand.draw(Player.game_init_data["seat"]["cards"])
	Player.socket_seat_subscribe(Player.game_init_data["seat"]["uuid"], _on_message)

func _show_status(text: String):
	_status_label.show()
	_status_label.text = text
	
func _hide_status():
	_status_label.hide()
	_status_label.text = ""

func _on_message(data):
	var actions = data["pub"]["data"]
	for action in actions:
		if socket_actions.has(action["type"]):
			socket_actions[action["type"]].call(action)
		else:
			print("Unknown action: " + action["type"])
			

func _on_card_played(data):
	var card = data["card"]
	
	# Don't play card since the client already did that for us
	if card["position"] == Player.game_init_data["seat"]["position"]:
		return
		
	_players.play(
		card["position"],
		card["suit"], 
		card["rank"],
		card["skin"]
	)
	
func _on_clear_cards(data):
	var winner = data["position"]
	_table.clean(winner)
	
func _on_update_hand(data):
	_hand.update(data["cards"], data["newCards"])
	
func _on_update_state(data):
	state = data["state"]
	_hide_status()
	
	if state == "WAITING_FOR_PLAYERS":
		DisplayServer.clipboard_set(Player.game_init_data["code"])
		_show_status(tr("game.lbl.waiting_for_player") + " - Code: " + Player.game_init_data["code"])
		
	if (state == "TRUMP" or state == "SCHIEBE") and active_position != Player.position:
		_show_status(tr("game.lbl.waiting_for_trump").format({"player": _players.config[active_position]["label"]}))
		
	if (state == "TRUMP" or state == "SCHIEBE") and active_position == Player.position:
		_choose_trump_gui.slide_in({"state": state})
		
	if state == "WEISEN_FIRST" and active_position == Player.position and possible_weise.size() > 0:
		_choose_weis_gui.slide_in({"weise": possible_weise})

# Update the active_position and handle the position icon animations
func _on_update_active_position(data):
	active_position = Players.PositionsEnum[data["position"]]
	
	# Stop any already running tweens and empty buffer
	position_icon_tweens.map(func(tween): tween.kill())
	position_icon_tweens = []
	
	# Get correct icon node
	var player_position = _players.relative_position(data["position"])
	var icon_node = _players.config[player_position]["icon"]
	
	# Animate icon "pulse" and add tween to bufffer so we can later kill it again
	var tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	tween.tween_property(icon_node, "scale", Vector2(1.1, 1.1), 0.4)
	tween.tween_property(icon_node, "scale", Vector2(0.9, 0.9), 0.4)
	tween.set_loops()
	position_icon_tweens.push_back(tween)
	
func _on_update_trump(data):
	if data["trump"] == null:
		return
		
	_trump_label.text = trumps[data["trump"]]

func _on_update_points(data):
	points = data["points"]
	_points_ns_label.text = str(points["NORTH"]["cardPoints"] + points["NORTH"]["weisPoints"] + points["SOUTH"]["cardPoints"] + points["SOUTH"]["weisPoints"])
	_points_we_label.text = str(points["WEST"]["cardPoints"] + points["WEST"]["weisPoints"] + points["EAST"]["cardPoints"] + points["SOUTH"]["weisPoints"])

func _on_player_joined(data):
	var player_position = _players.relative_position(data["player"]["position"])
	var config = _players.config[player_position]
	var icon_node = config["icon"]
	
	icon_node.texture = load(_get_position_icon_name(data["player"]))
	
	# Early return if node already visible. This means the player connect/disconnected and we 
	# we don't need to animate it in the viewport, this ist just at the beginning
	if icon_node.visible == true:
		return
	
	icon_node.show()
	
	var to = icon_node.position + (config["offset"]/1.5)
	if player_position == Players.PositionsEnum.SOUTH:
		# South needs the icon a little more towards the center because of the cards that are also there
		to = icon_node.position + (config["offset"]*2.5)

	var tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	tween.tween_property(icon_node, "position", to, 0.4)
	
func _get_position_icon_name(data):
	var positionLower = data["position"].to_lower()
	var status = "connected" # this default also applies for status BOT, they're always online
	if data["status"] == "DISCONNECTED":
		status = "disconnected"
	return str("res://assets/positions/", positionLower, "-", status, ".svg")
	
func _on_player_disconnected(data):
	var player_position = _players.relative_position(data["player"]["position"])
	var config = _players.config[player_position]
	var icon_node = config["icon"]
#
	var asset = str("res://assets/positions/", data["player"]["position"].to_lower(),  "-disconnected.svg")
	icon_node.texture = load(asset)
	
func _on_game_finished(data):
	_confetti.start()
	_game_finished_gui.slide_in(data)
	
func _on_update_possible_weise(data):
	possible_weise = data["weise"]
	
func _on_show_weis(data):
	# Don't show the weis because it's us
	if data["position"] == Player.game_init_data["seat"]["position"]:
		return
		
	if _show_weis_gui.open:
		_show_weis_gui.push_data(data)
	else:
		_show_weis_gui.slide_in(data)
