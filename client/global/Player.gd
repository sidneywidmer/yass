extends Node

const SECTION = "auth"
const SETTINGS_FILE = "user://settings.cfg"

var game_init_data: Dictionary
var game_scene: Node2D
var position: Players.PositionsEnum
var config: ConfigFile

var _ory_session: String
var _email: String
var _playername: String
var _socket = WebSocketPeer.new()
var _socket_poll: bool = false
var _socket_connected: bool = false
var _socket_channels: Dictionary = {}

func _ready():
	config = ConfigFile.new()
	config.load(SETTINGS_FILE)
	
	load_values()
	
func socket_connect():
	if _socket_connected:
		return
	_socket.set_handshake_headers(["X-Session-Token: {session}".format({"session": _ory_session})])
	_socket.connect_to_url("ws://127.0.0.1:8000/connection/websocket")
	_socket_poll = true

func socket_seat_subscribe(uuid: String, on_message: Callable):
	var channel = "seat:#{uuid}".format({"uuid": uuid})
	_socket_subscribe(channel, on_message)
	
func _socket_subscribe(channel: String, on_message: Callable):
	_socket.send_text(JSON.stringify({"subscribe":{"channel": channel},"id":2}))
	_socket_channels[channel] = on_message
	
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
	return config.get_value(SECTION, key, "")
	
func _process(_delta):
	if _socket_poll == false:
		return
		
	_socket.poll()
	var state = _socket.get_ready_state()
	if state == WebSocketPeer.STATE_OPEN:
		# Send initial centrifugo connect
		if _socket_connected == false:
			_socket_connected = true
			_socket.send_text(JSON.stringify({"connect": {"name":"js"}, "id":1}))
			
		while _socket.get_available_packet_count():
			var packet = _socket.get_packet().get_string_from_utf8()
			
			# Answer the centrifugo ping/pong call
			if packet == "{}":
				_socket.send_text("{}")
				continue
				
			var parsed = JSON.parse_string(packet)
			if parsed.has("push"):
				_socket_channels[parsed["push"]["channel"]].call(parsed["push"]["pub"]["data"])
				
	elif state == WebSocketPeer.STATE_CLOSING:
		pass
		
	elif state == WebSocketPeer.STATE_CLOSED:
		_socket_connected = false
		var code = _socket.get_close_code()
		var reason = _socket.get_close_reason()
		print("WebSocket closed with code: %d, reason %s. Clean: %s" % [code, reason, code != -1])
		set_process(false)
