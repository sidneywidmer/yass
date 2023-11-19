extends Node

class_name OryClient

var base_url : String = "http://127.0.0.1:4433/"

func login_flow(on_success: Callable, on_error: Callable) -> void:
	_http_get(
		"self-service/login/api?refresh&aal",
		on_success,
		on_error
	)
	
func login(flow: String, identifier: String, password: String, on_success: Callable, on_error: Callable) -> void:
	var fields = {"method": "password", "password": password, "identifier": identifier}
	
	_http_post(
		"/self-service/login?flow={flow}".format({"flow": flow}),
		fields,
		on_success,
		on_error
	)

func _http_get(endpoint: String, on_success: Callable, on_error: Callable) -> void:
	var request = HTTPRequest.new()
	var headers = ["Accept: application/json"]
	add_child(request)
	
	request.request_completed.connect(self._on_request_completed.bind(on_error).bind(on_success))
	request.timeout = 10
	request.request(base_url + endpoint, headers)
	
func _http_post(endpoint: String, body: Dictionary, on_success: Callable, on_error: Callable) -> void:
	var headers = ["Accept: application/json", "Content-Type: application/x-www-form-urlencoded"]
	var request = HTTPRequest.new()
	var bodyEncoded = _dict_to_query_string(body)
	add_child(request)
	
	request.request_completed.connect(self._on_request_completed.bind(on_error).bind(on_success))
	request.timeout = 10
	
	request.request(base_url + endpoint, headers, HTTPClient.METHOD_POST, bodyEncoded)

func _on_request_completed(result: int, response_code: int, headers: PackedStringArray, body: PackedByteArray, on_success: Callable, on_error: Callable):
	if response_code == 200 and result == 0:
		var parsed = JSON.parse_string(body.get_string_from_utf8())
		on_success.call(parsed)
	else:
		on_error.call(response_code, result, null)
		
func _dict_to_query_string(data: Dictionary) -> String:
	var query_string = ""
	var first_item = true

	for key in data.keys():
		var value = data[key]
		var encoded_key = str(key).uri_encode()
		var encoded_value = str(value).uri_encode()

		if first_item:
			query_string += encoded_key + "=" + encoded_value
			first_item = false
		else:
			query_string += "&" + encoded_key + "=" + encoded_value
			
	return query_string
