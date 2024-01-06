extends VBoxContainer

@onready var _winners_text = %WinnersText

func add_additional_params(_params: Dictionary):
	pass

func before_slide_in(params: Dictionary):
	_winners_text.text = tr("game.finished.lbl.text").format({
		"winner1": params["winners"][0]["name"], 
		"winner2": params["winners"][1]["name"], 
		"loser1": params["losers"][0]["name"], 
		"loser2": params["losers"][1]["name"],
		"winner_points": params["winnerPoints"], 
		"loser_points": params["loserPoints"], 
	})

func _on_back_btn_pressed():
	Player.socket_seat_unsubscribe(Player.game_init_data["seat"]["uuid"])
	SceneSwitcher.switch("res://scenes/MainMenuScene.tscn")
