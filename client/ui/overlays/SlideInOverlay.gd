extends Control

@onready var overlay = %Overlay
@onready var background = %Background
@onready var container = %Container

@export var content_scene: PackedScene

var content = null

func _ready():
	content = content_scene.instantiate()
	content.parent_overlay = self
	container.add_child(content)

func slide_in():
	self.visible = true
	var temp_position = content.global_position.y
	overlay.position.y = -overlay.size.y
#	
	background.modulate.a = 0
	
	var overlay_tween = get_tree().create_tween().set_ease(Tween.EASE_OUT).set_trans(Tween.TRANS_QUAD)
	overlay_tween.tween_property(overlay, "position:y", temp_position, 0.3)
	
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
