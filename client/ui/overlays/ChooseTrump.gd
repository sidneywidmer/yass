extends Control

@onready var overlay = %Overlay
@onready var background = %Background
@onready var schiebe_container = %SchiebeContainer

var game_scene: Node2D

func slide_in(allow_schiebe: bool):
	var temp_position = overlay.position.y
	overlay.position.y = -150
	self.visible = true
	background.modulate.a = 0
	schiebe_container.visible = allow_schiebe
	
	var overlay_tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	overlay_tween.tween_property(overlay, "position:y", temp_position, 0.5)
	
	var bg_tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	bg_tween.tween_property(background, "modulate:a", 0.5, 0.3)

func slide_out():
	var overlay_tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	overlay_tween.tween_property(overlay, "position:y", -300, 0.5).finished.connect(_on_slide_out_complete)
	
func _on_trump_pressed(trump: String):
	slide_out()
	if trump == "SCHIEBE":
		return ApiClient.schiebe(Player.game_init_data["gameUuid"], "YES", _on_schiebe_success, _on_schiebe_failed)

	# Player chose an actual trump but state is still schiebe, so we do that first and 
	# then choose the trump.
	if game_scene.state == "SCHIEBE":
		return ApiClient.schiebe(Player.game_init_data["gameUuid"], "NO", _on_schiebe_extra_success.bind(trump), _on_schiebe_failed)

	ApiClient.trump(Player.game_init_data["gameUuid"], trump, _on_trump_success, _on_trump_failed)

func _on_slide_out_complete():
	var bg_tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	bg_tween.tween_property(background, "modulate:a", 0, 0.3).finished.connect(_on_slide_out_bg_fade_complete)

func _on_slide_out_bg_fade_complete():
	self.visible = false

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
