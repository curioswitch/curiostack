using UnityEngine;

namespace Google.Maps.Examples {
  public class SmoothFollowCamera : MonoBehaviour {
    public Transform target;

    public Vector3 Offset = new Vector3(0, 5, -10);

    void LateUpdate() {
      if (target != null) {
        transform.position = target.position + Offset;

        Camera.main.transform.LookAt(target);
      }
    }
  }
}
