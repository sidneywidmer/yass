extends Node

class_name BaseClient

var base_url : String = ""
var timeout: int = 10
var use_ory_session: bool = false
var use_anon_token: bool = false

func _init(_base_url: String = "", _timeout: int = 10, _use_ory_session: bool = false, _use_anon_token: bool = false):
	base_url = _base_url
	timeout = _timeout
	use_ory_session = _use_ory_session
	use_anon_token = _use_anon_token

func _http_get(endpoint: String, on_success: Callable, on_error: Callable, extra_headers: Array) -> void:
	var headers = ["Accept: application/json"]
	
	if extra_headers.size() > 0:
		headers.append_array(extra_headers)
	
	if use_ory_session:
		headers.append("X-Session-Token: {token}".format({"token": Player._ory_session}))
		
	if use_anon_token:
		headers.append("X-Anon-Token: {token}".format({"token": Player._anon_token}))
	
	var request = HTTPRequest.new()
	add_child(request)
	
	request.request_completed.connect(self._on_request_completed.bind(on_error).bind(on_success))
	request.timeout = timeout
	request.request(base_url + endpoint, headers)
	
func _http_post_form(endpoint: String, body: Dictionary, on_success: Callable, on_error: Callable) -> void:
	var headers = ["Accept: application/json", "Content-Type: application/x-www-form-urlencoded"]		
	var request = HTTPRequest.new()
	var bodyEncoded = _dict_to_query_string(body)
	add_child(request)
	
	request.request_completed.connect(self._on_request_completed.bind(on_error).bind(on_success))
	request.timeout = timeout
	
	request.request(base_url + endpoint, headers, HTTPClient.METHOD_POST, bodyEncoded)
	
func _http_post(endpoint: String, body: Dictionary, on_success: Callable, on_error: Callable) -> void:
	var headers = ["Accept: application/json", "Content-Type: application/json"]
	
	if use_ory_session:
		headers.append("X-Session-Token: {token}".format({"token": Player._ory_session}))
		
	if use_anon_token:
		headers.append("X-Anon-Token: {token}".format({"token": Player._anon_token}))
	
	var request = HTTPRequest.new()
	var bodyEncoded = JSON.stringify(body)
	add_child(request)
	
	request.request_completed.connect(self._on_request_completed.bind(on_error).bind(on_success))
	request.timeout = timeout
	
	request.request(base_url + endpoint, headers, HTTPClient.METHOD_POST, bodyEncoded)

func _on_request_completed(result: int, response_code: int, _headers: PackedStringArray, body: PackedByteArray, on_success: Callable, on_error: Callable):
	var parsed = null
	if result == 0:
		parsed = JSON.parse_string(body.get_string_from_utf8())
	
	if response_code == 200 and result == 0:
		on_success.call(parsed)
	else:
		on_error.call(response_code, result, parsed)
		
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
