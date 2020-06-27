using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Script that moves a camera to follow another GameObject.
  /// </summary>
  /// <remarks>
  /// Movement is at a variable height controlled by the Q and E keys.
  /// </summary>
  [RequireComponent(typeof(Camera))]
  public class ObjectFollower : MonoBehaviour {
    [Tooltip("Move the camera to follow this GameObject.")]
    public GameObject Following;

    [Tooltip("How quickly the camera should catch up with the object.")]
    public float CatchupSpeed = 3;

    [Tooltip(
        "Rate to move the camera up and down, as a proportion of the " +
        "current height per second.")]
    public float VerticalSpeed = 1;

    [Tooltip(
        "Amount to maintain the camera inclination as the camera moves up and down. 1 keeps " +
        "the same angle; 0 looks straight down when the camera is high.")]
    public float KeepCameraAngle = 0.8f;

    /// <summary>Offset to maintain between the camera and the followed object.</summary>
    private Vector3 FollowOffset;

    /// <summary>Set up this script.</summary>
    public void Start() {
      // Remember the initial offset between the camera and the object it's following.
      // FollowTarget() will move the camera to try to maintain this offset.
      FollowOffset = gameObject.transform.position - Following.transform.position;
    }

    /// <summary>Per-frame update tasks.</summary>
    public void Update() {
      ControlHeight();
      FollowTarget();
    }

    /// <summary>Use the Q and E keys to change the height of the camera.</summary>
    private void ControlHeight() {
      float scale = VerticalSpeed * Time.deltaTime;

      if (Input.GetKey(KeyCode.E)) {
        FollowOffset.x *= 1 + scale * KeepCameraAngle;
        FollowOffset.y *= 1 + scale;
        FollowOffset.z *= 1 + scale * KeepCameraAngle;
      }

      if (Input.GetKey(KeyCode.Q)) {
        FollowOffset.x /= 1 + scale * KeepCameraAngle;
        FollowOffset.y /= 1 + scale;
        FollowOffset.z /= 1 + scale * KeepCameraAngle;
      }
    }

    /// <summary>Move the camera to catch up with the GameObject that it's following.</summary>
    private void FollowTarget() {
      float dt = Time.deltaTime;
      Vector3 pos = gameObject.transform.position;
      Vector3 target = Following.transform.position + (Following.transform.rotation * FollowOffset);
      float catchup = CatchupSpeed * dt;
      pos = ((1 - catchup) * pos) + (catchup * target);

      gameObject.transform.position = pos;
      gameObject.transform.LookAt(Following.transform);
    }
  }
}
