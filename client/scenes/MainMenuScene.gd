extends Node2D

@export var settings_scene:PackedScene
@export var login_scene:PackedScene

@onready var overlay := %FadeOverlay
@onready var settings_button := %SettingsButton
@onready var logout_button := %LogoutButton
@onready var join_button := %JoinGameButton
@onready var exit_button := %ExitButton
@onready var player_name := %PlayerName
@onready var error := %ErrorLabel
@onready var code := %GameCode
@onready var api := %ApiClient

var next_scene = null
var new_game = true

func _ready() -> void:
	overlay.visible = true
	settings_button.disabled = settings_scene == null
	
	player_name.text = "Willkommen, {name} ({mail})".format({"name": Player._playername, "mail": Player._email})
	
	settings_button.pressed.connect(_on_settings_button_pressed)
	logout_button.pressed.connect(_on_logout_button_pressed)
	exit_button.pressed.connect(_on_exit_button_pressed)
	join_button.pressed.connect(_on_join_button_pressed)
	overlay.on_complete_fade_out.connect(_on_fade_overlay_on_complete_fade_out)

func _on_join_button_pressed() -> void:
	api.join(
		code.text,
		_on_join_success,
		_on_join_failed
	)

func _on_settings_button_pressed() -> void:
	new_game = false
	next_scene = settings_scene
	overlay.fade_out()
	
func _on_logout_button_pressed() -> void:
	Player.logout()
	next_scene = login_scene
	overlay.fade_out()

func _on_exit_button_pressed() -> void:
	get_tree().quit()
	
func _on_join_success(data) -> void:
	print(data)
	
func _on_join_failed(response_code, result, data) -> void:
	error.visible = true
	if response_code == 404:
		error.text = "No game with code {code} found".format({"code": code.text})
	else:
		error.text = "Could not join game, maybe it's already full? :("
	
func _on_fade_overlay_on_complete_fade_out() -> void:
	get_tree().change_scene_to_packed(next_scene)
