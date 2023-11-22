extends Node2D

@onready var _hand = %Hand
@onready var _table = %Table
@onready var _players = %Players

func _ready():
	_hand.game_scene = self
	_players.game_scene = self
	_table.game_scene = self
	
	Player.game_scene = self
	Player.position = _players.Players[Player.game_init_data["seat"]['position']]
	# player.subscribe('game.' + player._game_code)
	# player.debugmsg('game ready with code: ' + player._game_code)
	
	# If we got some players already in the game at login we'll 
	# show them here including any cards they might've played
	for card in Player.game_init_data['cardsPlayed']:
		#player._game.player_joined(p['name'], p['position'])
		_players.play(
			card['position'],
			card['suit'], 
			card['rank'],
			card['skin']
		)
		
	# The player should have a hand or at least some welcome cards
	_hand.draw(Player.game_init_data["seat"]["cards"])
		
func player_joined(name, absolute_position):
	var position = _players.relative_position(absolute_position)
	var config = _players.config[position]
	var label = config['node'].get_node('name')
	
	label.text = name
	var from = label.position
	var to = label.position + (config['offset']/2)

	var tween = Tween.new()
	add_child(tween)
	tween.interpolate_property(
		label, 'position', 
		from, to, 0.1,
		Tween.TRANS_QUAD, Tween.EASE_OUT)
	tween.start()
