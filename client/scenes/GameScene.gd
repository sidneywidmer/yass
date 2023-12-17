extends Node2D

@onready var _hand = %Hand
@onready var _table = %Table
@onready var _players = %Players
@onready var _points_ns_label = %PointsNS
@onready var _points_we_label = %PointsWE
@onready var _trump_label = %Trump
@onready var _choose_trump_gui = %ChooseTrump
@onready var _status_label = %StatusLabel

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

func _ready():
	_hand.game_scene = self
	_players.game_scene = self
	_table.game_scene = self
	_choose_trump_gui.pass_additional_params({"game_scene": self})
	
	Player.game_scene = self
	Player.position = Players.PositionsEnum[Player.game_init_data["seat"]["position"]]
	
	_on_update_active_position({"position": Player.game_init_data["seat"]["activePosition"]})
	_on_update_state(Player.game_init_data["seat"])
	_on_update_trump(Player.game_init_data["seat"])
	_on_update_points(Player.game_init_data["seat"])
	
	# If we got some players already in the game at login we"ll 
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

func _on_message(actions):
	for action in actions:
		if socket_actions.has(action["type"]):
			socket_actions[action["type"]].call(action)
		else:
			print("Unknown action: " + action["type"])

func _on_card_played(data):
	var card = data["card"]
	
	# Don"t play card since the client already did that for us
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
		
func _on_update_active_position(data):
	active_position = Players.PositionsEnum[data["position"]]
	
func _on_update_trump(data):
	if data["trump"] == null:
		return
		
	trump = data["trump"]
	_trump_label.text = trumps[trump]

func _on_update_points(data):
	points = data["points"]
	_points_ns_label.text = str(points["NORTH"] + points["SOUTH"])
	_points_we_label.text = str(points["WEST"] + points["EAST"])
	
func player_joined(_player_name, absolute_position):
	var player_position = _players.relative_position(absolute_position)
	var config = _players.config[player_position]
	var label = config["node"].get_node("name")
	
	label.text = name
	var from = label.position
	var to = label.position + (config["offset"]/2)

	var tween = Tween.new()
	add_child(tween)
	tween.interpolate_property(
		label, "position", 
		from, to, 0.1,
		Tween.TRANS_QUAD, Tween.EASE_OUT)
	tween.start()
