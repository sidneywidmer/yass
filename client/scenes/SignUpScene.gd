extends Control

@onready var email := %EMail
@onready var password := %Password
@onready var username := %Name
@onready var signup_button := %SignUpButton
@onready var back_button := %BackButton
@onready var error := %ErrorLabel

var signup_flow = null

func _ready() -> void:
	OryClient.signup_flow(_on_signup_flow_success, _on_signup_flow_failed)
	email.text = Player._email
	username.text = Player._playername
	back_button.pressed.connect(_on_back_button_pressed)
	signup_button.pressed.connect(_on_signup_button_pressed)
	username.grab_focus()

func _on_back_button_pressed():
	SceneSwitcher.switch("res://scenes/LoginScene.tscn")

func _on_signup_button_pressed() -> void:
	OryClient.signup(
		username.text,
		email.text,
		password.text,
		signup_flow,
		_on_signup_success,
		_on_signup_failed
	)
	
func _on_signup_flow_success(data):
	print(data)
	signup_flow = data["id"]
	error.visible = false
	
func _on_signup_flow_failed(response_code: int, result: int, _parsed):
	error.visible = true
	error.text = tr("signup.error.server_connection").format({"response_code": response_code, "client_code": result})
	
func _on_signup_success(data):
	Player.set_player(
		data["session_token"],
		data["session"]["identity"]["traits"]["email"],
		data["session"]["identity"]["traits"]["name"]
	)
	Player.socket_connect()
	error.visible = false
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
	
func _on_signup_failed(_response_code: int, _result: int, _parsed):
	# TODO: Extract the following messages and display:
	#	ui.messages[].text where type == error
	#	ui.nodes[].message[].text where type == error
	error.visible = true
	error.text = tr("signup.error.signup_failed")
