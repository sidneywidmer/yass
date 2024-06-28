extends Control


func _on_profile_pressed(profile: String):
	Player.profile = profile
	Player.boot()
	SceneSwitcher.switch("res://scenes/auth/LoginScene.tscn")


func _on_reset_pressed():
	# var profiles = ["default", "debug-1", "debug-2", "debug-3", "debug-4"]
	var config = ConfigFile.new()
	config.load("user://settings-debug-3.cfg")
	config.clear()
	config.save("user://settings-debug-3.cfg")
