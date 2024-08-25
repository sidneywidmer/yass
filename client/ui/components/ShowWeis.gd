extends VBoxContainer

@export var overlay_parent = Node2D
var duplicates: Array = [] # keep track so we can delete on slide out
@onready var weis_repeatable = %WeisRepeatable

var game_scene: Node2D
var parent_overlay: Control

func add_additional_params(params: Dictionary):
	game_scene = params["game_scene"]
	parent_overlay = params["parent_overlay"]

func push_data(params: Dictionary):
	before_slide_in(params)

func before_slide_in(params: Dictionary):
	# TODO: Display nice playing cards and not just the name e.g. VIER_BLATT
	var instance = weis_repeatable.duplicate()
	weis_repeatable.add_sibling(instance)
	duplicates.append(instance)
	instance.show()
	
	var label = instance.get_node("Label")
	label.text = params["position"] + " " + params["weis"]["type"] + " (" + str(params["weis"]["points"]) + " pts)"
	add_child(instance)

func _on_button_pressed():
	for inst in duplicates:
		if inst != null: # No idea how this can be null but sometimes happens
			inst.queue_free()
		
	duplicates = []
	parent_overlay.slide_out()
