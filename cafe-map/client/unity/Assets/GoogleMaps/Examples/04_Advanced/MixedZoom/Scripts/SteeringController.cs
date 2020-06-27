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
    }
  }
}
