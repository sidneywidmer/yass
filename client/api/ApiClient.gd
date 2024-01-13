extends BaseClient

func _init():
	super._init("http://127.0.0.1:8080", 30, true, true)

func whoami(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"/auth/whoami",
		on_success,
		on_error,
		[]
	)

func play(game: String, suit: String, rank: String, skin: String, on_success: Callable, on_error: Callable) -> void:
	_http_post(
		"/game/play",
		{"game": game, "card": {"suit": suit, "rank": rank, "skin": skin}},
		on_success,
		on_error
	)
	
func ping(seat: String, on_success: Callable, on_error: Callable) -> void:
	_http_post(
		"/game/ping",
		{"seat": seat},
		on_success,
		on_error
	)
	
func join(code: String, on_success: Callable, on_error: Callable) -> void:
	_http_post(
		"/game/join",
		{"code": code},
		on_success,
		on_error
	)
	
func schiebe(game: String, schiebe: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/schiebe",
		{"game": game, "gschobe": schiebe},
		on_success,
		on_error
	)
	
func trump(game: String, trump: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/trump",
		{"game": game, "trump": trump},
		on_success,
		on_error
	)
	
func create_custom_game(params: Dictionary, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/create",
		params,
		on_success,
		on_error
	)
	
func anon_sign_up(token: String, name: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/auth/anon/signup",
		{"name": name, "anonToken": token},
		on_success,
		on_error
	)
	
