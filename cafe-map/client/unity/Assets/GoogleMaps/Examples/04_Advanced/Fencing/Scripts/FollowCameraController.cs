using UnityEngine;
using System.Collections;

namespace Google.Maps.Examples {
  public class FollowCameraController : MonoBehaviour {
    public GameObject Player;

    private Vector3 Offset;

    void Start() {
      Offset = transform.position - Player.transform.position;
    }

    void LateUpdate() {
      transform.position = Player.transform.position + Offset;
    }
  }
}
