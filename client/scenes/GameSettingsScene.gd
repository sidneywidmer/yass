extends Node2D

@onready var return_button := %ReturnButton

func _ready():
	return_button.pressed.connect(_on_return_button_pressed)
	return_button.grab_focus()

func _on_return_button_pressed():
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
