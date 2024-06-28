extends BaseClient

func _init():
	super._init("http://127.0.0.1:4433", 5, false, false)

func whoami(on_success: Callable, on_error: Callable) -> void:
	var headers = ["Authorization: Bearer {session}".format({"session": Player._ory_session})]

	_http_get(
		"/sessions/whoami",
		on_success,
		on_error,
		headers
	)

func login_flow(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"/self-service/login/api?refresh&aal",
		on_success,
		on_error,
		[]
	)
	
func signup_flow(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"/self-service/registration/api",
		on_success,
		on_error,
		[]
	)
	
func signup(username: String, email: String, password: String, flow: String, on_success: Callable, on_error: Callable) -> void:
	_http_post(
		"/self-service/registration?flow={flow}".format({"flow": flow}),
		{
			"traits.email": email,
			"traits.name": username,
			"password": password,
			"method": "password"
		},
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
