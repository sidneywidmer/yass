extends Node2D

@onready var settings_button := %SettingsButton
@onready var custom_game_button := %CustomGameButton
@onready var logout_button := %LogoutButton
@onready var signup_button := %SignUpButton
@onready var join_button := %JoinGameButton
@onready var exit_button := %ExitButton
@onready var player_name := %PlayerName
@onready var error := %ErrorLabel
@onready var code := %GameCode

func _ready() -> void:
	if Player.is_anon():
		player_name.text = tr("main.lbl.welcome").format({"name": Player._playername, "mail": "Anonymous"})
		logout_button.disabled = true
		signup_button.show()
	else:
		signup_button.hide()
		player_name.text = tr("main.lbl.welcome").format({"name": Player._playername, "mail": Player._email})
	
	settings_button.pressed.connect(_on_settings_button_pressed)
	logout_button.pressed.connect(_on_logout_button_pressed)
	exit_button.pressed.connect(_on_exit_button_pressed)
	join_button.pressed.connect(_on_join_button_pressed)
	custom_game_button.pressed.connect(_on_custom_game_button_pressed)
	signup_button.pressed.connect(_on_signup_button_pressed)

func _on_join_button_pressed() -> void:
	ApiClient.join(
		code.text,
		_on_join_success,
		_on_join_failed
	)

func _on_settings_button_pressed() -> void:
	SceneSwitcher.switch("res://scenes/GameSettingsScene.tscn")
	
func _on_logout_button_pressed() -> void:
	Player.logout()
	SceneSwitcher.switch("res://scenes/auth/LoginScene.tscn")

func _on_exit_button_pressed() -> void:
	get_tree().quit()
	
func _on_custom_game_button_pressed() -> void:
	SceneSwitcher.switch("res://scenes/CustomGameScene.tscn")
	
func _on_signup_button_pressed() -> void:
	SceneSwitcher.switch("res://scenes/auth/SignUpScene.tscn")
	
func _on_join_success(data) -> void:
	Player.game_init_data = data
	SceneSwitcher.switch("res://scenes/game/GameScene.tscn")
	
func _on_join_failed(response_code, _result, _data) -> void:
	error.visible = true
	if response_code == 404:
		error.text = tr("main.error.game_404").format({"code": code.text})
	else:
		error.text = tr("main.error.join")
