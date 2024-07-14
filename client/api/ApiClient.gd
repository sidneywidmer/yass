extends BaseClient

func _init():
	super._init("http://localhost:8080", 5, true, true)

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
	
func schiebe(game: String, schiebe_value: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/schiebe",
		{"game": game, "gschobe": schiebe_value},
		on_success,
		on_error
	)
	
func trump(game: String, trump_value: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/trump",
		{"game": game, "trump": trump_value},
		on_success,
		on_error
	)
	
func weisen(game: String, weis: Dictionary, on_success: Callable, on_error: Callable):
	_http_post(
		"/game/weisen",
		{"game": game, "weis": weis},
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
	
func anon_sign_up(token: String, name_value: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/auth/anon/signup",
		{"name": name_value, "anonToken": token},
		on_success,
		on_error
	)
	
func anon_link_account(ory_session: String, on_success: Callable, on_error: Callable):
	_http_post(
		"/auth/anon/link",
		{"orySession": ory_session},
		on_success,
		on_error
	)
	
