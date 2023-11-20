extends Node2D

@onready var overlay := %FadeOverlay
@onready var email := %EMail
@onready var password := %Password
@onready var login_button := %LoginButton
@onready var error := %ErrorLabel
@onready var ory := %OryClient

var auth_flow = null

func _ready() -> void:
	if Player._ory_session != '':
		get_tree().change_scene_to_file("res://scenes/MainMenuScene.tscn")
	
	overlay.visible = true
	ory.login_flow(_on_auth_flow_success, _on_auth_flow_failed)
	
	email.text = Player._email
	
	login_button.pressed.connect(_on_login_button_pressed)
	overlay.on_complete_fade_out.connect(_on_fade_overlay_on_complete_fade_out)
	
	email.grab_focus()

func _on_login_button_pressed() -> void:
	ory.login(
		auth_flow,
		email.text,
		password.text,
		_on_login_success,
		_on_login_failed
	)

func _on_auth_flow_success(data):
	auth_flow = data["id"]
	login_button.disabled = false
	error.visible = false
	
func _on_auth_flow_failed(response_code: int, result: int, parsed):
	error.visible = true
	error.text = "Error: Could not reach server to login, code: {c}, result: {r}".format({"c": response_code, "r": result})

func _on_login_success(data):
	Player.set_player(
		data["session_token"],
		data["session"]["identity"]["traits"]["email"],
		data["session"]["identity"]["traits"]["name"]
	)
	
	error.visible = false
	overlay.fade_out()
	
func _on_login_failed(response_code: int, result: int, parsed):
	error.visible = true
	error.text = "E-Mail/Passwort falsch"

func _on_fade_overlay_on_complete_fade_out() -> void:
	get_tree().change_scene_to_file("res://scenes/MainMenuScene.tscn")
