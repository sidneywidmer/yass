extends VBoxContainer

@onready var schiebe_container = %SchiebeContainer
@export var overlay_parent = Node2D

var game_scene: Node2D
var parent_overlay: Control

func add_additional_params(params: Dictionary):
	game_scene = params["game_scene"]
	parent_overlay = params["parent_overlay"]

func before_slide_in(params: Dictionary):
	schiebe_container.visible = params["state"] == "SCHIEBE"

func _on_trump_pressed(trump: String):
	parent_overlay.slide_out()
	if trump == "SCHIEBE":
		return ApiClient.schiebe(Player.game_init_data["gameUuid"], "YES", _on_schiebe_success, _on_schiebe_failed)

	# Player chose an actual trump but state is still schiebe, so we do that first and 
	# then choose the trump.
	if game_scene.state == "SCHIEBE":
		return ApiClient.schiebe(Player.game_init_data["gameUuid"], "NO", _on_schiebe_extra_success.bind(trump), _on_schiebe_failed)

	ApiClient.trump(Player.game_init_data["gameUuid"], trump, _on_trump_success, _on_trump_failed)

func _on_schiebe_extra_success(_data, trump: String):
	ApiClient.trump(Player.game_init_data["gameUuid"], trump, _on_trump_success, _on_trump_failed)

func _on_schiebe_success(_data):
	pass
	
func _on_schiebe_failed(_response_code: int, _result: int, _parsed):
	# TODO: Just show rejoin or how do we handle this?
	pass
	
func _on_trump_success(_data):
	pass
	
func _on_trump_failed(_response_code: int, _result: int, _parsed):
	# TODO: Just show rejoin or how do we handle this?
	pass


func _on_texture_button_pressed(extra_arg_0):
	pass # Replace with function body.
