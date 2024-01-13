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
	var token = "anonToken_" + Crypto.new().generate_random_bytes(64).hex_encode()
	Player.set_anon_token(token)
	ApiClient.anon_sign_up(
		token,
		username.text,
		_on_signup_success,
		_on_signup_failed
	)
	
func _on_signup_success(data):
	Player.set_player(
		"",  # Anon users don't have an ory_sesson, anon_token set in _on_signup_button_pressed
		"", # Anon users don't have an email
		data["name"]
	)
	Player.socket_connect()
	error.visible = false
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
	
func _on_signup_failed(_response_code: int, _result: int, _parsed):
	print(_response_code, _result, _parsed)
	error.visible = true
	error.text = tr("anon.error.signup_failed")
