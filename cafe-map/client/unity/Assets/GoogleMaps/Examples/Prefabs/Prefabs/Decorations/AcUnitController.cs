using UnityEngine;

public class AcUnitController : MonoBehaviour {
  public GameObject FanObj;

  private void Start() {
    transform.localRotation = Quaternion.Euler(0, Random.Range(0, 180), 0);
  }

  private void Update() {
    Vector3 fanRotationVector = FanObj.transform.localRotation.eulerAngles;
    fanRotationVector.y += Time.deltaTime * 200;
    FanObj.transform.localRotation = Quaternion.Euler(fanRotationVector);
  }
}
