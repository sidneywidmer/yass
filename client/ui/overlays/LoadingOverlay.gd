class_name LoadingOverlay
extends ColorRect

@onready var label := %LoadingLabel

@export var fade_in_duration: float = 0.5
@export var fade_out_duration: float = 0.5

func set_text(text: String):
	label.text = text

func fade_in():
	var tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	tween.tween_property(self, "modulate", Color(modulate, 0.0), fade_in_duration)

func fade_out():
	var tween = create_tween().set_trans(Tween.TRANS_CUBIC)
	tween.tween_property(self, "modulate", Color(modulate, 1), fade_out_duration)
