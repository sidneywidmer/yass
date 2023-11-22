class_name Hand
extends Control

const Card = preload('res://scenes/Card.tscn')

var game_scene = null
var rng = RandomNumberGenerator.new()

func clear():
	for card in self.get_children():
		self.remove_child(card)
	
func draw(new_cards):
	var cards = self.get_children()
	if len(cards) == 9:
		return
		
	for new_card in new_cards:
		var card_instance = Card.instantiate()
		card_instance.skin = new_card['skin']
		card_instance.suit = new_card['suit']
		card_instance.rank = new_card['rank']
		card_instance.locked = new_card.get('locked', false)  # TODO: ist neu state PLAYABLE, e.t.c

		# Relative to hand, this is out of our viewport so it appears like the 
		# cards come from bellow.
		card_instance.position = Vector2(0, 300) 
		self.add_child(card_instance)
		
		rearange_hand()
		await get_tree().create_timer(0.05).timeout

	
func play(card_instance: Card):
	card_instance.state_position = card_instance.TABLE
	card_instance.z_index = 9
	card_instance.tween(
		'rotation_degrees', 
		rng.randf_range(-10.0, 10.0)
	)
	card_instance.tween(
		'global_position', 
		game_scene._table.global_position-Vector2(0, -80),
		_play_out_complete.bind(card_instance)
	)
	var data = {
		# 'uid': player._uid,
		'card': {'suit': card_instance.suit, 'rank': card_instance.rank}
	}
	# player.game_api(player._game_code, 'hand/play', data)
	lock_all()
	
func lock_all():
	for card in self.get_children():
		card.locked = false # TODO: Should be true, just false for testing
		
func unlock_all():
	for card in self.get_children():
		card.locked = false
		
func unlock(cards):	
	if typeof(cards) == TYPE_STRING and cards == '*':
		self.unlock_all()
	
	for unlock in cards:
		for card in self.get_children():
			if unlock['suit'] == card.suit and unlock['rank'] == card.rank:
				card.locked = false
	
func _play_out_complete(card_instance):
	print(card_instance)
	card_instance.z_index = 0
	self.remove_child(card_instance)
	game_scene._table.add_child(card_instance)
	card_instance.position = Vector2(0, 80)
	rearange_hand()

func rearange_hand():
	# Cards left and right should be 'lower' to describe an arc.
	var vertical_shrink = 0.0005 
	var rotation = 1.2
	
	var cards = self.get_children()
	if len(cards) == 0:
		return
	
	# If we have only two cards the spacing is smaller then with 9, but 
	# never go bellow 35.
	var spacing = 180/len(cards)
	if spacing < 42:
		spacing = 42
		
	var start_pos = Vector2(-(spacing / 2) - (spacing * ((float(cards.size()) / 2) - 1)), 0)
	var start_rot = -(rotation / 2) - (rotation * ((float(cards.size()) / 2) - 1))
	
	for index in range(cards.size()):
		var card = cards[index]
		var x = start_pos.x + (spacing * index)
		var y = vertical_shrink * pow(x,2) - 20
		card.tween('position', Vector2(x, y))
		card.tween('rotation_degrees', start_rot + rotation * index)
