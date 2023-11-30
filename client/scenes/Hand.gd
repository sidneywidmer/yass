class_name Hand
extends Control

const Card: PackedScene = preload("res://scenes/Card.tscn")

var game_scene: Node2D
var rng: RandomNumberGenerator = RandomNumberGenerator.new()
	
func draw(new_cards):
	for new_card in new_cards:
		# The server sends also the already played cards, ignore them
		if new_card["state"] == "ALREADY_PLAYED":
			continue
			
		var card_instance = Card.instantiate()
		card_instance.skin = new_card["skin"]
		card_instance.suit = new_card["suit"]
		card_instance.rank = new_card["rank"]
		card_instance.state = new_card["state"]

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
		"rotation_degrees", 
		rng.randf_range(-10.0, 10.0)
	)
	card_instance.tween(
		"global_position", 
		game_scene._table.global_position-Vector2(0, -80),
		_play_out_complete.bind(card_instance)
	)

	ApiClient.play(
		Player.game_init_data["gameUuid"],
		card_instance.suit,
		card_instance.rank,
		card_instance.skin,
		_on_play_success,
		_on_play_failed
	)
		
func update(cards):	
	# If we don"t have any cards we can"t update anything which means
	# it"s a new hand and we draw
	if self.get_children().size() == 0:
		return await draw(cards)
		
	for card in cards:
		for card_instance in self.get_children():
			if card["suit"] == card_instance.suit and card["rank"] == card_instance.rank:
				card_instance.state = card["state"]

func _on_play_success(_data):
	pass
	
func _on_play_failed(_response_code: int, _result: int, _parsed):
	# TODO: Revert card instance back to hand somehow?
	# Or just show an error overlay to "rejoin"?
	pass
	
func _play_out_complete(card_instance):
	card_instance.z_index = 0
	self.remove_child(card_instance)
	game_scene._table.add_child(card_instance)
	card_instance.position = Vector2(0, 80)
	rearange_hand()

func rearange_hand():
	# Cards left and right should be "lower" to describe an arc.
	var vertical_shrink = 0.0005 
	var card_rotation = 1.2
	
	var cards = self.get_children()
	if len(cards) == 0:
		return
	
	# If we have only two cards the spacing is smaller then with 9, but 
	# never go bellow 35.
	var spacing = 180/len(cards)
	if spacing < 42:
		spacing = 42
		
	var start_pos = Vector2(-(spacing / 2) - (spacing * ((float(cards.size()) / 2) - 1)), 0)
	var start_rot = -(card_rotation / 2) - (card_rotation * ((float(cards.size()) / 2) - 1))
	
	for index in range(cards.size()):
		var card = cards[index]
		var x = start_pos.x + (spacing * index)
		var y = vertical_shrink * pow(x,2) - 20
		card.tween("position", Vector2(x, y))
		card.tween("rotation_degrees", start_rot + card_rotation * index)
