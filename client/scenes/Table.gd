extends Control

var rng = RandomNumberGenerator.new()
var game_scene = null

func clean(absolute_position):
	var position = game_scene._players.relative_position(absolute_position)
	
	var cards = self.get_children()
	if len(cards) == 0:
		return
		
	var node = game_scene._players.config[position]['node']
	var off_table = node.global_position - game_scene._players.config[position]['off_table']
	
	for card in cards:
		card.connect('clean_complete', Callable(self, '_clean_complete'))
		card.tween('rotation_degrees', card.rotation_degrees, rng.randf_range(-10.0, 10.0))
		card.tween('global_position', card.global_position, off_table, 'clean_complete', {}, 0.55)

func _clean_complete(card_instance, extra):
	self.remove_child(card_instance)
