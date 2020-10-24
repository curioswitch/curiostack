using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Script to steer an object around, using the vertical input axis to move forwards and backwards
  /// and the input axis to pan.
  /// </summary>
  public class SteeringController : MonoBehaviour {
    [Tooltip("Movement speed, as a proportion of the distance to the current camera per second.")]
    public float MoveSpeed = 0.5f;

    [Tooltip("Panning speed, in degrees per second.")]
    public float RotateSpeed = 45;

    [Tooltip("Whether to raycast player position onto ground, e.g., terrain.")]
    public bool RaycastToGround = true;

    [Tooltip("Height of player position above raycast result.")]
    public float RaycastHoverHeight = 1.0f;

    /// <summary>Per-frame update tasks.</summary>
    public void Update() {
      float dx = Input.GetAxis("Horizontal");
      float dy = Input.GetAxis("Vertical");
      float dt = Time.deltaTime;

      // Scale the speed based on the distance to the camera, so that the map moves under the object
      // at a constant rate on the screen.
      float absoluteSpeed =
          MoveSpeed * (Camera.main.transform.position - gameObject.transform.position).magnitude;

      gameObject.transform.Rotate(Vector3.up, RotateSpeed * dx * dt);

      gameObject.transform.position +=
          gameObject.transform.rotation * (Vector3.forward * absoluteSpeed * dy * dt);
      if (RaycastToGround) {
        gameObject.transform.position =
            ProjectToGround(gameObject.transform.position, RaycastHoverHeight);
      }
    }

    /// <summary>
    /// Project the supplied position by raycasting straight down onto ground or terrain.
    /// </summary>
    /// <param name="position">The position from which to cast ray.</param>
    /// <param name="hoverHeight">The height of returned result above ray intersection.</param>
    /// <returns>The raycast result (plus hover height).</returns>
    public Vector3 ProjectToGround(Vector3 position, float hoverHeight) {
      Ray ray = new Ray(position + Vector3.up * 1000f, Vector3.down);

      RaycastHit hit;

      if (Physics.Raycast(ray, out hit, Mathf.Infinity)) {
        return hit.point + Vector3.up * hoverHeight;
      }

      return position;
    }
  }
}
