extends Control

@onready var username := %Name
@onready var signup_button := %SignUpButton
@onready var back_button := %BackButton
@onready var error := %ErrorLabel

func _ready() -> void:
	back_button.pressed.connect(_on_back_button_pressed)
	signup_button.pressed.connect(_on_signup_button_pressed)
	username.grab_focus()

func _on_back_button_pressed():
	SceneSwitcher.switch("res://scenes/auth/LoginScene.tscn")

func _on_signup_button_pressed() -> void:
	var token = Crypto.new().generate_random_bytes(64)
	print(token.hex_encode())
	ApiClient.anon_sign_up(
		token.hex_encode(),
		username.text,
		_on_signup_success,
		_on_signup_failed
	)
	
func _on_signup_success(data):
	Player.set_player(
		data["session_token"],
		"-",
		data["name"]
	)
	Player.socket_connect()
	error.visible = false
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
	
func _on_signup_failed(_response_code: int, _result: int, _parsed):
	error.visible = true
	error.text = tr("anon.error.signup_failed")
