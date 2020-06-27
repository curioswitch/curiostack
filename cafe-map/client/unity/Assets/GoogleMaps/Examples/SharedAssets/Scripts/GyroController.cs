using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  ///   This class controls the pitch elevation of the camera rig when the examples
  ///   are deployed on a mobile device.
  /// </summary>
  public class GyroController : MonoBehaviour {
    /// <summary>
    /// Reference to the camera rig (wraps the camera object)
    /// </summary>
    public Transform CameraRig;
    /// <summary>
    /// Text field for feedback on Gyro availability
    /// </summary>
    public Text InfoTxt;
    /// <summary>
    /// Keeps track of the euler rotation on the forward axis
    /// </summary>
    private float AngleX;
    /// <summary>
    /// Reference to the transform of the camera
    /// </summary>
    private Transform CameraTransform;
    /// <summary>
    /// Reference to the gyroscope when available
    /// </summary>
    private Gyroscope Gyro;
    /// <summary>
    /// Indicates if the gyroscope is supported on the device where the app is deployed
    /// </summary>
    private bool GyroSupported;

    /// <summary>
    /// At start, we detect the availability of the gyroscope and keep track of camera transform
    /// and euler rotation on the X axis.
    /// </summary>
    private void Start() {

      GyroSupported = SystemInfo.supportsGyroscope;
      if (GyroSupported) {
        Gyro = Input.gyro;
        Gyro.enabled = true;
      }

      if (CameraRig != null) {
        var c = CameraRig.gameObject.GetComponentInChildren<Camera>();
        if (c != null) {
          CameraTransform = c.transform;
          AngleX = CameraTransform.rotation.eulerAngles.x;
        }
      }

      if (InfoTxt != null) {
        InfoTxt.text = "";
      }
    }

    /// <summary>
    /// During updates, keep track of the rotation diffs around the X axis,
    /// and apply the new combined angle to the camera transform.
    /// </summary>
    private void Update() {
      if (GyroSupported) {
        if (CameraRig != null) {
          AngleX += -Input.gyro.rotationRateUnbiased.x;

          AngleX = Mathf.Clamp(AngleX, -30f, 90f);
          CameraTransform.transform.localRotation = Quaternion.Euler(
              AngleX,
              0f,
              0f);
        }
      } else {
        InfoTxt.text = "Gyro Not supported !";
      }
    }
  }
}
