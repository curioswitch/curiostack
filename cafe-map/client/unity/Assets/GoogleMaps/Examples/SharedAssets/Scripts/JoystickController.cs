using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

namespace Google.Maps.Demos.Utilities {

  /// <summary>
  /// This class implements a simple joystick meant for mobile deployment
  /// that allows users to move the camera around the scene.
  /// It includes support for: Vertical motion up and down, Forward, Reverse, Yaw.
  ///
  /// The rotations and forward/reverse motion are controlled by dragging the joystick knob
  /// on the circular area. Rotations and Forward are exclusive from each other,
  /// and triggered when the knob reaches specific angles.
  /// This is meant to create a smooth user experience.
  /// Rotation and forward/reverse speeds increase proportionally to the distance of the knob
  /// from the joystick center, up to a specified max value.
  ///
  /// Up and down motion are controlled by two separate buttons.
  ///
  /// </summary>
  public class JoystickController : MonoBehaviour, IDragHandler, IPointerUpHandler,
    IPointerDownHandler {

    #region properties
    // Joystick bounds
    public Image Background;
    // Camera Rig to move
    public Transform CameraRig;
    // Direction to follow
    public Vector3 InputDirection;
    // Lever knob to control forward/reverse, CW and CCW rotations
    public Image Lever;
    // Maximum rotation speed
    public float MaxRotationSpeed = 1f;
    // Maximum forward/reverse speed
    public float MaxForwardSpeed = 200f;
    // Controls going down
    public JoystickButton DownButton;
    // Min altitude (in meters)
    public float MinAltitude = 5;
    // Controls going up
    public JoystickButton UpButton;
    // Max altitude (in meters)
    public float MaxAltitude = 300;
    // Vertical displacement speed
    public float MaxVerticalSpeed = 200f;
    // Reference to camera (a child of the camera rig)
    private Camera Camera;

    #endregion

    /// <summary>
    /// On start, set joystick defaults.
    /// If the app is deployed on mobile device, show the joystick in the scene,
    /// and initialize the cameraRig.
    ///
    /// </summary>
    private void Start() {
      InputDirection = Vector3.zero;

      // Hide by default
      gameObject.transform.parent.gameObject.SetActive(false);

      // If the example isn't run on mobile devices, hide the joystick
      #if (UNITY_IOS || UNITY_ANDROID) && !UNITY_EDITOR
        gameObject.transform.parent.gameObject.SetActive(true);
        // Initialize the camera container's position
        Camera = CameraRig.GetComponentInChildren<Camera>();
        if (Camera != null) {
          CameraRig.position = new Vector3(
            Camera.transform.position.x,
            Camera.transform.position.y,
            Camera.transform.position.z);
          Camera.transform.localPosition = Vector3.zero;
        }
      #endif
    }

    /// <summary>
    /// On Updates, adjust the position and CW, CCW rotations of the Rig.
    /// The code also applies vertical motions if Up and Down buttons are continously pressed.
    ///
    /// </summary>
    private void Update() {
      // Move the camera at a speed that is linearly dependent on the height of the camera above
      // the ground plane to make camera manual camera movement practicable. The movement speed
      // is clamped between 1% and 100% of the configured MovementSpeed.
      float forwardSpeed = Mathf.Clamp(
                             CameraRig.transform.position.y,
                             MaxForwardSpeed * 0.01f,
                             MaxForwardSpeed)*Time.deltaTime;

      // Max speed in high altitude
      // Min speed at 0
      // 0 < Pos y < max altitude
      float verticalSpeed = Mathf.Clamp(
                              CameraRig.transform.position.y,
                              MaxVerticalSpeed * 0.01f,
                              MaxVerticalSpeed)*Time.deltaTime;

      if (InputDirection.magnitude != 0 && CameraRig != null) {
        float rotationDirection = 1f;
        float angle = Vector3.Angle(InputDirection, Vector3.right);

        if (angle > 90f) rotationDirection = -1f;
        if (angle < 80f || angle > 100) {
          // Rotate target around y axis
          CameraRig.transform.RotateAround(
            CameraRig.transform.position,
            Vector3.up,
            rotationDirection * MaxRotationSpeed * InputDirection.magnitude * Time.deltaTime);
        }
        else {
          float dir = InputDirection.y >= 0 ? 1f : -1f;
          CameraRig.transform.position += CameraRig.transform.forward * forwardSpeed * dir
                                          * InputDirection.magnitude;
        }
      }

      if (UpButton != null && CameraRig != null && UpButton.IsButtonPressed()) {
        CameraRig.transform.position += CameraRig.transform.up * verticalSpeed;
      }

      if (DownButton != null && CameraRig != null && DownButton.IsButtonPressed()) {
        CameraRig.transform.position -= CameraRig.transform.up * verticalSpeed;
      }

      CameraRig.transform.position = new Vector3(
        CameraRig.transform.position.x,
        Mathf.Clamp(CameraRig.transform.position.y, MinAltitude, MaxAltitude),
        CameraRig.transform.position.z);
    }

    #region event listeners
    /// <summary>
    /// Implements the IDragHandler interface.
    /// The function converts the drag of the joystick knob on the UI overlay
    /// to a direction vector in worldspace that can be applied to our target.
    /// </summary>
    /// <param name="ped">The pointer event data</param>
    public void OnDrag(PointerEventData ped) {
      // Current position
      var pos = Vector2.zero;
      var rect = Background.rectTransform;

      // Move the target based on the Lever's position
      RectTransformUtility.ScreenPointToLocalPointInRectangle(
        rect,
        ped.position,
        ped.pressEventCamera,
        out pos);

      pos.x = pos.x / rect.sizeDelta.x;
      pos.y = pos.y / rect.sizeDelta.y;

      InputDirection = new Vector3(pos.x, pos.y, 0f);
      InputDirection = InputDirection.magnitude > 1 ? InputDirection.normalized : InputDirection;

      Lever.rectTransform.anchoredPosition = new Vector3(
        InputDirection.x * (rect.sizeDelta.x / 3),
        InputDirection.y * rect.sizeDelta.y / 3);
    }

    /// <summary>
    /// Implements the IPointerUpHandler interface.
    /// Applies changes in similar ways to the OnDrag function.
    /// </summary>
    /// <param name="ped">The pointer event data</param>
    public void OnPointerDown(PointerEventData ped) {
      OnDrag(ped);
    }

    /// <summary>
    /// Implements the IPointerDownHandler interface.
    /// Resets the position of the joystick knob and the direction vector.
    /// </summary>
    /// <param name="ped"></param>
    public void OnPointerUp(PointerEventData ped) {
      Lever.rectTransform.anchoredPosition = Vector3.zero;
      InputDirection = Vector3.zero;
    }
    #endregion
  }
}
