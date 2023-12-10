extends Control

@onready var back_button := %BackButton
@onready var create_button := %CreateCustomGameButton
@onready var bot_north_toggle := %BotNorth
@onready var bot_east_toggle := %BotEast
@onready var bot_south_toggle := %BotSouth
@onready var bot_west_toggle := %BotWest
@onready var winning_condition_type := %WinningCondition
@onready var winning_condition_value := %WinningConditionValue

# Called when the node enters the scene tree for the first time.
func _ready():
	back_button.pressed.connect(_on_back_button_pressed)
	create_button.pressed.connect(_on_create_button_pressed)

func _on_back_button_pressed():
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
	
func _on_create_button_pressed():
	var wc = ("HANDS") if (winning_condition_type.selected == 0) else ("POINTS")
	var payload = {
		"botNorth": bot_north_toggle.button_pressed,
		"botEast": bot_east_toggle.button_pressed,
		"botSouth": bot_south_toggle.button_pressed,
		"botWest": bot_west_toggle.button_pressed,
		"winningConditionType": wc,
		"winningConditionValue": winning_condition_value.text
	}
	
	ApiClient.create_custom_game(payload, _on_create_success, _on_create_failed)
	
func _on_create_success(data):
	print(data["code"])
	ApiClient.join(
		data["code"],
		_on_join_success,
		_on_join_failed
	)
	
func _on_join_success(data) -> void:
	Player.game_init_data = data
	SceneSwitcher.switch("res://scenes/GameScene.tscn")
	
func _on_join_failed(response_code, _result, _data) -> void:
	pass
	
func _on_create_failed(_response_code: int, _result: int, _parsed):
	pass
