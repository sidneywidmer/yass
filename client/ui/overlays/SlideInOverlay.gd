extends Control

@onready var overlay = %Overlay
@onready var background = %Background
@onready var container = %Container

var content = null
var original_position = null

func _ready():
	original_position = overlay.global_position.y
	
	# Content needs a script with two functions:
	# - add_additional_params
	# - before_slide_in
	# With these we can react to those two events, i bet there's a better 
	# way with signals but for now this works :)
	content = container.get_child(0)
	
func pass_additional_params(params: Dictionary):
	params["parent_overlay"] = self
	content.add_additional_params(params)

func slide_in(params: Dictionary):
	content.before_slide_in(params)
	overlay.position.y = -overlay.size.y
	self.visible = true
	background.modulate.a = 0
	
	var overlay_tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	overlay_tween.tween_property(overlay, "position:y", original_position, 0.3)
	
	var bg_tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	bg_tween.tween_property(background, "modulate:a", 0.5, 0.3)

func slide_out():
	var overlay_tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	overlay_tween.tween_property(overlay, "position:y", -overlay.size.y, 0.3).finished.connect(_on_slide_out_complete)

func _on_slide_out_complete():
	var bg_tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	bg_tween.tween_property(background, "modulate:a", 0, 0.3).finished.connect(_on_slide_out_bg_fade_complete)

func _on_slide_out_bg_fade_complete():
	self.visible = false
