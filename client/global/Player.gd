extends Node

const SECTION = "auth"
const SETTINGS_FILE = "user://settings.cfg"

var _ory_session = null
var _email = null
var _playername = null

var game_init_data = null
var game_scene = null
var position = null

var config:ConfigFile

func _ready():
	config = ConfigFile.new()
	config.load(SETTINGS_FILE)
	
	load_values()
	
	if _ory_session == '':
		get_tree().change_scene_to_file("res://scenes/LoginScene.tscn")

func logout():
	_set_value("ory_session", "")
	load_values()

func load_values():
	_ory_session = _get_value("ory_session")
	_email = _get_value("email")
	_playername = _get_value("playername")
	
func set_player(ory_session: String, email: String, playername: String):
	_set_value("ory_session", ory_session)
	_set_value("email", email)
	_set_value("playername", playername)
	
	load_values()

func _set_value(key, value):
	config.set_value(SECTION, key, value)
	config.save(SETTINGS_FILE)
	
func _get_value(key):
	return config.get_value(SECTION, key, '')
