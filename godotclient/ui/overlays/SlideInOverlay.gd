extends Control

@onready var overlay = %Overlay
@onready var background = %Background
@onready var container = %Container

@export var content_scene: PackedScene

var content = null
var original_position = null

# Ensure content has an add_additional_params and a before_slide_in function
# so we can pass data through. I bet there's a better way to do this 
# with either inerhitance or signals...
func _ready():
	content = content_scene.instantiate()
	container.add_child(content)
	self.visible = true # Not 100% sure why this is needed but without it the size of the node is wrong
	original_position = overlay.global_position.y
	self.visible = false
	
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
