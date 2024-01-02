class_name Players
extends Node

const Card = preload("res://scenes/Card.tscn")

enum PositionsEnum {NORTH, EAST, SOUTH, WEST}

@onready var hand := %Hand
@onready var north := %North
@onready var east := %East
@onready var west := %West
@onready var north_icon := %NorthIcon
@onready var east_icon := %EastIcon
@onready var south_icon := %SouthIcon
@onready var west_icon := %WestIcon

@onready var config = {
	PositionsEnum.NORTH: {
		"label": tr("game.lbl.north"),
		"icon": north_icon,
		"node": north, 
		"offset": Vector2(0, 80), 
		"rotate": 0, 
		"off_table": Vector2(0, 100)
	},
	PositionsEnum.EAST: {
		"label": tr("game.lbl.easth"),
		"icon": east_icon,
		"node": east, 
		"offset": Vector2(-80, 0), 
		"rotate": 15, 
		"off_table": Vector2(-100, 0)
	},
	PositionsEnum.SOUTH: {
		"label": tr("game.lbl.south"),
		"icon": south_icon,
		"node": hand, 
		"offset": Vector2(0, -80), 
		"rotate": -15, 
		"off_table": Vector2(0, -100)
	},
	PositionsEnum.WEST: {
		"label": tr("game.lbl.west"),
		"icon": west_icon,
		"node": west, 
		"offset": Vector2(80, 0), 
		"rotate": -15, 
		"off_table": Vector2(100, 0)
	},
}

var game_scene = null
var rng = RandomNumberGenerator.new()

""" 
Calculate the position of given absolute position relative to ourselves. Eg if we 
are NORTH, our relative position is SOUTH. A card from our partner (absolute SOUTH)
will relative to us be NORTH.
"""
func relative_position(position: String):
	var south_offset = PositionsEnum.SOUTH - Player.position
	var new = PositionsEnum[position] + south_offset
	
	if new > 3:
		return new % 4
	elif new == -1:
		return 3
		
	return new

func play(absolute_position, suit, rank, skin):
	var position = self.relative_position(absolute_position)
	var card_instance = Card.instantiate()
	var cnfg = config[position]
	
	card_instance.state_position = card_instance.TABLE
	card_instance.suit = suit
	card_instance.rank = rank
	card_instance.skin = skin
	card_instance.rotation_degrees = cnfg["rotate"]
	
	cnfg["node"].add_child(card_instance)
	
	card_instance.z_index = 9
	card_instance.tween(
		"rotation_degrees", 
		rng.randf_range(-10.0, 10.0) + cnfg["rotate"]
	)
	
	var extra = {"offset": cnfg["offset"] * -1, "node": cnfg["node"]}
	card_instance.tween(
		"global_position", 
		 game_scene._table.global_position-cnfg["offset"],
		_play_out_complete.bind(extra).bind(card_instance)
	)

func _play_out_complete(card_instance, extra: Dictionary = {}):
	card_instance.z_index = 0
	extra["node"].remove_child(card_instance)
	game_scene._table.add_child(card_instance)
	card_instance.position = extra["offset"]
