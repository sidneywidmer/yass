extends Control


func _on_profile_pressed(profile: String):
	Player.profile = profile
	Player.boot()
	SceneSwitcher.switch("res://scenes/LoginScene.tscn")
