extends BaseClient

class_name ApiClient

func _init():
	super._init("http://127.0.0.1:8080", 10, true)

func whoami(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"/auth/whoami",
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
