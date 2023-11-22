class_name Card
extends Node2D

signal peak_in_complete(card_instance, extra)
signal play_out_complete(card_instance, extra)
signal clean_complete(card_instance, extra)

@onready var _hand = self.get_parent()
@onready var image := %Image
@onready var transitions := %Transitions

enum{HAND, PEAK, DRAG, TABLE}

var suit = ''
var rank = ''
var skin = ''
var state = ''

var state_position = HAND
var locked = true
var _tmp_root_position = null
var _tmp_root_rotation = null

func _ready():
	var asset = str("res://assets/cards/", skin, "/", suit,  "-", rank, ".svg")
	image.texture = load(asset)

func tween(type, to, on_complete = null, duration = 0.18):
	var tween = transitions.create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	tween.tween_property(self, type, to, duration)
	if on_complete:
		tween.connect("finished", on_complete)

func _on_draggable_entered():
	if state_position != HAND:
		return
	
	state_position = PEAK
	_tmp_root_position = self.get_position()
	_tmp_root_rotation = self.rotation
	self.rotation = 0
	self.z_index = 9
	
	var offset = 50
	if locked:
		offset = 20

	self.tween(
		'position', 
		Vector2(self.get_position().x, self.get_position().y-offset)
	)

func _on_draggable_exited():
	if state_position != PEAK:
		return
	
	self.rotation = _tmp_root_rotation
	self.z_index = 0
	self.tween(
		'position', 
		_tmp_root_position,
		_peak_in_complete
	)
	
func _on_draggable_pressed():
	if locked:
		return
		
	_hand.play(self)
	
func _peak_in_complete():
	self.state_position = HAND
