import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation

# === Configuration ===
obstacle_radius = 0.6
step_size = 0.5
goal_threshold = 0.5
total_frames = 200
num_obstacles = 5

# Start and goal
start = np.array([0.0, 0.0])
goal = np.array([8.0, 6.0])

# Initialize obstacles and directions
np.random.seed(42)
obstacles = np.random.uniform([1, 1], [7, 5], size=(num_obstacles, 2))
obstacle_directions = np.random.uniform(-0.1, 0.1, size=(num_obstacles, 2))

# Robot state
robot_position = start.copy()
robot_path = [robot_position.copy()]
obstacles_over_time = []

def compute_repulsion(pos, obstacles):
    force = np.zeros(2)
    for obs in obstacles:
        direction = pos - obs
        distance = np.linalg.norm(direction)
        if distance < obstacle_radius:
            force += direction / (distance + 1e-6) * (1.0 / distance)
    return force

# === Simulation Loop ===
for _ in range(total_frames):
    # Update obstacle positions
    for i, obs in enumerate(obstacles):
        obs += obstacle_directions[i]
        if not (0 <= obs[0] <= 10):
            obstacle_directions[i][0] *= -1
        if not (0 <= obs[1] <= 7):
            obstacle_directions[i][1] *= -1
        obstacles[i] = obs
    obstacles_over_time.append(obstacles.copy())

    # Forces
    to_goal = goal - robot_position
    to_goal = to_goal / (np.linalg.norm(to_goal) + 1e-6)
    repulsion = compute_repulsion(robot_position, obstacles)
    total_force = to_goal + repulsion
    total_force = total_force / (np.linalg.norm(total_force) + 1e-6)

    # Move robot
    robot_position = robot_position + step_size * total_force
    robot_path.append(robot_position.copy())

    if np.linalg.norm(goal - robot_position) < goal_threshold:
        break

# === Animation ===
fig, ax = plt.subplots(figsize=(8, 6))
robot_dot, = ax.plot([], [], 'bo', markersize=10, label='Robot')
goal_dot, = ax.plot(goal[0], goal[1], 'ro', markersize=10, label='Goal')
robot_trace, = ax.plot([], [], 'b--', lw=1)
obstacle_patches = [plt.Circle((0, 0), obstacle_radius, color='red', alpha=0.4) for _ in range(num_obstacles)]
for patch in obstacle_patches:
    ax.add_patch(patch)

ax.set_xlim(-1, 11)
ax.set_ylim(-1, 8)
ax.set_title("Real-Time Reactive Path Planning (Elastic Band Style)")
ax.set_xlabel("X")
ax.set_ylabel("Y")
ax.legend()
ax.grid(True)
ax.set_aspect('equal')

def init():
    robot_dot.set_data([], [])
    robot_trace.set_data([], [])
    for patch in obstacle_patches:
        patch.center = (0, 0)
    return [robot_dot, robot_trace, goal_dot] + obstacle_patches

def update(frame):
    pos = robot_path[frame]
    robot_dot.set_data([pos[0]], [pos[1]])  # <-- fixed here
    trace = np.array(robot_path[:frame+1])
    robot_trace.set_data(trace[:, 0], trace[:, 1])
    for i, patch in enumerate(obstacle_patches):
        patch.center = obstacles_over_time[frame][i]
    return [robot_dot, robot_trace, goal_dot] + obstacle_patches

ani = animation.FuncAnimation(
    fig, update, frames=len(robot_path), init_func=init,
    blit=True, interval=150, repeat=False)

plt.show()
