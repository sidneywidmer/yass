extends Node2D

var emitting = false
var size = 3.0
var visibility_rect = Rect2(0.0, 0.0, 400.0, 300.0)
var colors = [
	Color("#ff9999"),
	Color("#db97ff"),
	Color("#9ab9ff"),
	Color("#9affc1"),
	Color("#fdffa6")
]

var timer_wait_time = 1.5

var particles = []
var particles_amount
var particles_position
var timer = 0.0

func _ready():
	pass
	
func start():
	self.emitting = true

func _process(delta):
	timer += delta

	if timer > timer_wait_time:
		timer = 0.0
		_create_particles()

	_particles_explode(delta)
	queue_redraw()

func _draw():
	for particle in particles:
		draw_circle(
			particle.position, ((particle.size.x + particle.size.y) / 2) / 2, particle.color
		)

func _create_particles():
	particles.clear()
	particles_amount = round(randf_range(150 / 2.0, 150 * 2.0))
	particles_position = _get_random_position()

	for _i in particles_amount:
		var particle = {
			color = _get_random_color(),
			gravity = _get_random_gravity(),
			position = particles_position,
			size = _get_random_size(),
			velocity = _get_random_velocity()
		}

		particles.append(particle)

func _particles_explode(delta):
	for particle in particles:
		particle.velocity.x *= .999
		particle.velocity.y *= .991

		particle.position += (particle.velocity + particle.gravity) * delta

func _get_random_color():
	return colors[randi() % colors.size()]

func _get_random_gravity():
	return Vector2(randf_range(-200, 200), randf_range(400, 800))

func _get_random_position():
	var x = randf_range(0, visibility_rect.size.x)
	var y = randf_range(0, visibility_rect.size.y)

	return Vector2(x, y)

func _get_random_size():
	var min_size = int(ceil(size / 2.0))
	var max_size = int(ceil(size * 2.0))
	var random_min_max_size = randi() % (max_size - min_size + 1) + min_size

	return Vector2(random_min_max_size, random_min_max_size)

func _get_random_velocity():
	return Vector2(randf_range(-200, 200), randf_range(-600, -800))

func _set_emitting(new_value):
	if new_value != emitting:
		emitting = new_value

		if emitting:
			set_process(true)
			_create_particles()
		else:
			set_process(false)
			particles.clear()
			timer = 0.0
