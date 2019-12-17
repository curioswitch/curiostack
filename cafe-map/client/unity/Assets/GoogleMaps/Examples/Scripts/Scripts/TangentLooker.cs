using UnityEngine;

///<summary>
/// Keeps an object pointed at right angles to its parent object. Used to keep the camera pointing
/// in the direction of motion.
/// </summary>
public class TangentLooker : MonoBehaviour {
  void Update () {
    transform.LookAt(transform.parent);
    transform.eulerAngles = new Vector3(0, transform.eulerAngles.y, 0);
    transform.Rotate(Vector3.up * -90, Space.World);
    transform.Rotate(Vector3.right, 60);
  }
}
