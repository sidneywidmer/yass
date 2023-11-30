extends Node2D

@onready var loading := %LoadingOverlay
@onready var email := %EMail
@onready var password := %Password
@onready var login_button := %LoginButton
@onready var error := %ErrorLabel

var auth_flow = null

func _ready() -> void:
	loading.visible = true
	
	OryClient.whoami(_on_whoami_success, _on_whoami_failed)
	
	email.text = Player._email
	login_button.pressed.connect(_on_login_button_pressed)
	email.grab_focus()

func _on_login_button_pressed() -> void:
	OryClient.login(
		auth_flow,
		email.text,
		password.text,
		_on_login_success,
		_on_login_failed
	)
	
func _on_whoami_success(_data):
	loading.fade_out()
	Player.socket_connect()
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
	
func _on_whoami_failed(_response_code: int, result: int, _parsed):
	if result != 0:
		loading.set_text("Could not reach server, try again later :( Code: {code}".format({"code": result}))
	else:
		OryClient.login_flow(_on_auth_flow_success, _on_auth_flow_failed)

func _on_auth_flow_success(data):
	loading.visible = false
	auth_flow = data["id"]
	error.visible = false
	
func _on_auth_flow_failed(response_code: int, result: int, _parsed):
	error.visible = true
	error.text = "Error: Could not reach server to login, code: {c}, result: {r}".format({"c": response_code, "r": result})

func _on_login_success(data):
	Player.set_player(
		data["session_token"],
		data["session"]["identity"]["traits"]["email"],
		data["session"]["identity"]["traits"]["name"]
	)
	Player.socket_connect()
	
	error.visible = false
	
func _on_login_failed(_response_code: int, _result: int, _parsed):
	error.visible = true
	error.text = "E-Mail/Passwort falsch"
