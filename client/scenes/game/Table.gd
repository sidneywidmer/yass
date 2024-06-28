extends Control

var rng = RandomNumberGenerator.new()
var game_scene = null

func clean(absolute_position):
	var relative_position = game_scene._players.relative_position(absolute_position)
	
	var cards = self.get_children()
	if len(cards) == 0:
		return
		
	var node = game_scene._players.config[relative_position]["node"]
	var off_table = node.global_position - game_scene._players.config[relative_position]["off_table"]
	
	for card in cards:
		card.tween("rotation_degrees", rng.randf_range(-10.0, 10.0))
		card.tween("global_position", off_table, _clean_complete.bind(card))

func _clean_complete(card_instance):
	card_instance.queue_free()
