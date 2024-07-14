extends VBoxContainer

@export var overlay_parent = Node2D
@onready var weis_repeatable = %WeisRepeatable

var game_scene: Node2D
var parent_overlay: Control
var duplicates: Array = [] # keep track so we can delete on slide out

func add_additional_params(params: Dictionary):
	game_scene = params["game_scene"]
	parent_overlay = params["parent_overlay"]

func before_slide_in(params: Dictionary):
	var weise = params["weise"]
	
	# TODO: Display nice playing cards and not just the name e.g. VIER_BLATT
	for weis in weise:
		var instance = weis_repeatable.duplicate()
		instance.queue_free()
		duplicates.append(instance)
		instance.show()
		
		var button = instance.get_node("Button")
		button.pressed.connect(_on_weis_selected.bind(weis))
		button.text = weis["type"] + " (" + str(weis["points"]) + " pts)"
		add_child(instance)
	
func _on_weis_selected(weis: Dictionary):
	parent_overlay.slide_out()
	for inst in duplicates:
		inst.queue_free()
		
	ApiClient.weisen(Player.game_init_data["gameUuid"], weis, _on_weis_success, _on_weis_failed)

func _on_weis_success(_data):
	pass
	
func _on_weis_failed(_response_code: int, _result: int, _parsed):
	# TODO: Just show rejoin or how do we handle this?
	print("weis failed")
