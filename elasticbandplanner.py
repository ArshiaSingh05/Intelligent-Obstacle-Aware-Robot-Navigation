import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation

# Define initial path points (start to goal)
initial_path = np.array([
    [0, 0],     # Start
    [2, 2],
    [4, 2],
    [6, 2],
    [8, 4],     # Goal
])

obstacle_radius = 1.0
alpha = 0.1
total_frames = 60

# Moving obstacle position function
def get_moving_obstacle(t):
    x = 4
    y = 2.2 + 1.0 * np.sin(2 * np.pi * t / total_frames)
    return np.array([x, y])

# Elastic Band function
def simulate_elastic_band(path, obstacle, alpha=0.1):
    new_path = path.copy()
    for i in range(1, len(path) - 1):
        prev = path[i - 1]
        curr = path[i]
        nxt = path[i + 1]

        # Tension (smoothness force)
        tension = (prev + nxt - 2 * curr)

        # Repulsive force from obstacle
        direction = curr - obstacle
        distance = np.linalg.norm(direction)
        if distance < obstacle_radius:
            repulsion = direction / (distance + 1e-6) * (1.0 / distance)
        else:
            repulsion = np.zeros(2)

        # Total force applied to point
        force = tension + repulsion
        new_path[i] = curr + alpha * force

    return new_path

# Precompute path and obstacle positions
path = initial_path.copy()
trajectories = []
obstacle_positions = []

for t in range(total_frames):
    obstacle = get_moving_obstacle(t)
    path = simulate_elastic_band(path, obstacle)
    trajectories.append(path.copy())
    obstacle_positions.append(obstacle.copy())

# Create plot
fig, ax = plt.subplots(figsize=(8, 6))
line, = ax.plot([], [], 'bo-', lw=2)
obstacle_circle = plt.Circle((0, 0), obstacle_radius, color='red', alpha=0.3)
ax.add_patch(obstacle_circle)
obstacle_point, = ax.plot([], [], 'rx', label='Moving Obstacle')
ax.set_xlim(-1, 10)
ax.set_ylim(-1, 6)
ax.set_title("Elastic Band with Dynamic Obstacle")
ax.set_xlabel("X")
ax.set_ylabel("Y")
ax.grid(True)
ax.legend()
ax.set_aspect('equal')

# Animation functions
def init():
    line.set_data([], [])
    obstacle_point.set_data([], [])
    obstacle_circle.center = (0, 0)
    return line, obstacle_point, obstacle_circle

def update(frame):
    data = trajectories[frame]
    obstacle = obstacle_positions[frame]
    line.set_data(data[:, 0], data[:, 1])
    obstacle_point.set_data([obstacle[0]], [obstacle[1]])  # <-- fixed here
    obstacle_circle.center = obstacle
    return line, obstacle_point, obstacle_circle

ani = animation.FuncAnimation(fig, update, frames=total_frames,
                              init_func=init, blit=True, interval=100)

plt.show()
