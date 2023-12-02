extends CanvasLayer

@onready var overlay = %Overlay
	
func switch(new_scene: String):
	visible = true
	overlay.modulate.a = 1

	get_tree().change_scene_to_file(new_scene)
	var tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	tween.tween_property(overlay, "modulate:a", 0, 0.3).finished.connect(_on_fadeout_complete)

func _on_fadeout_complete():
	visible = false
