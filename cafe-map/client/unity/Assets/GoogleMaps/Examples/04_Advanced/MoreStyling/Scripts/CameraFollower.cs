using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// A MonoBehaviour to make an object follow the center of view of Camera
  /// </summary>
  public class CameraFollower : MonoBehaviour {
    /// <summary>
    /// The camera whose center of view is tracked.
    /// </summary>
    public Camera Camera;

    /// <summary>
    /// A collider used as the ground plane for ray casting from the camera.
    /// </summary>
    public Collider Target;

    /// <summary>
    /// Casts a ray from <see cref="Camera"/> to the ground plane collider <see cref="Target"/> and,
    /// if a hit occurs, moves the object to which this behaviour is attached to the location of the
    /// hit.
    /// </summary>
    private void Update() {
      RaycastHit hit;
      Ray ray = new Ray(Camera.transform.position, Camera.transform.forward);

      if (Target.Raycast(ray, out hit, 10000.0f)) {
        transform.position = hit.point;
      }
    }
  }
}
