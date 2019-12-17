using UnityEngine;

namespace GoogleMaps.Examples.Scripts {
  /// <summary>
  /// A controller for keyboard-controlled movement in eight directions. The attached
  /// <see cref="GameObject"/> must have an attached <see cref="Rigidbody"/>.
  /// </summary>
  public class EightDirectionalController : MonoBehaviour {
    /// <summary>
    /// Speed at which to cut off acceleration.
    /// </summary>
    public float MaxSpeed = 40f;

    /// <summary>
    /// Update the direction of the <see cref="GameObject"/> and apply forces if the movement
    /// keys are pressed.
    /// </summary>
    private void FixedUpdate() {
      float step = 0f;
      float speed = 10f;

      bool up = Input.GetKey("w");
      bool down = Input.GetKey("s");
      bool left = Input.GetKey("a");
      bool right = Input.GetKey("d");

      Vector3 direction = Vector2.zero;

      if (up) {
        direction.z = 1;
      }
      if (down) {
        direction.z = -1;
      }
      if (left) {
        direction.x = -1;
      }
      if (right) {
        direction.x = 1;
      }


      if (up || down || left || right) {
        step = speed;
        gameObject.transform.LookAt(gameObject.transform.position + direction);
      }

      direction *= step;

      Rigidbody rigidbody = GetComponent<Rigidbody>();
      if (rigidbody.velocity.magnitude < MaxSpeed)
        rigidbody.AddForce(direction, ForceMode.VelocityChange);
    }
  }
}
