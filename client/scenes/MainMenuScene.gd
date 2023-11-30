extends Node2D

@onready var settings_button := %SettingsButton
@onready var logout_button := %LogoutButton
@onready var join_button := %JoinGameButton
@onready var exit_button := %ExitButton
@onready var player_name := %PlayerName
@onready var error := %ErrorLabel
@onready var code := %GameCode

var new_game = true

func _ready() -> void:
	player_name.text = "Willkommen, {name} ({mail})".format({"name": Player._playername, "mail": Player._email})
	
	settings_button.pressed.connect(_on_settings_button_pressed)
	logout_button.pressed.connect(_on_logout_button_pressed)
	exit_button.pressed.connect(_on_exit_button_pressed)
	join_button.pressed.connect(_on_join_button_pressed)

func _on_join_button_pressed() -> void:
	ApiClient.join(
		code.text,
		_on_join_success,
		_on_join_failed
	)

func _on_settings_button_pressed() -> void:
	new_game = false
	SceneSwitcher.switch("res://scenes/GameSettingsScene.tscn")
	
func _on_logout_button_pressed() -> void:
	Player.logout()
	SceneSwitcher.switch("res://scenes/LoginScene.tscn")

func _on_exit_button_pressed() -> void:
	get_tree().quit()
	
func _on_join_success(data) -> void:
	Player.game_init_data = data
	SceneSwitcher.switch("res://scenes/GameScene.tscn")
	
func _on_join_failed(response_code, _result, _data) -> void:
	error.visible = true
	if response_code == 404:
		error.text = "No game with code {code} found".format({"code": code.text})
	else:
		error.text = "Could not join game, maybe it's already full? :("
