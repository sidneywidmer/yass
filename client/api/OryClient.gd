extends BaseClient

class_name OryClient

func _init():
	super._init("http://127.0.0.1:4433", 10, false)

func login_flow(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"/self-service/login/api?refresh&aal",
		on_success,
		on_error
	)
	
func login(flow: String, identifier: String, password: String, on_success: Callable, on_error: Callable) -> void:
	var fields = {"method": "password", "password": password, "identifier": identifier}
	
	_http_post_form(
		"/self-service/login?flow={flow}".format({"flow": flow}),
		fields,
		on_success,
		on_error
	)
