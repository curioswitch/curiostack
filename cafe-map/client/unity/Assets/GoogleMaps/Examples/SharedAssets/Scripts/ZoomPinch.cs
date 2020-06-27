using UnityEngine;

namespace Google.Maps.Examples.Shared {

  /// <summary>
  /// This class implements a zoom for mobile which allows the user to control the field of view
  /// for a perspective camera or the orthographic size for an orthographic camera with two fingers.
  /// </summary>
  public class ZoomPinch : MonoBehaviour {

    #region properties
    /// <summary>
    /// The rate of change every frame of the field of view (angle) in perspective mode.
    /// </summary>
    [Tooltip("The rate of change of the field of view in perspective mode every frame.")]
    public float PerspectiveZoomSpeed = 0.5f;
    /// <summary>
    /// The rate of change every frame of the orthographic size (meters) in orthographic mode.
    /// </summary>
    [Tooltip("The rate of change of the orthographic size in orthographic mode.")]
    public float OrthoZoomSpeed = 0.5f;
    /// <summary>
    /// Reference to the active camera
    /// </summary>
    public Camera ActiveCamera;
    /// <summary>
    /// Min field of view
    /// </summary>
    [Tooltip("The minimum field of view when in perspective mode.")]
    public float MinFieldOfView = 30f;
    /// <summary>
    /// Max field of view
    /// </summary>
    [Tooltip("The maximum field of view when in perspective mode.")]
    public float MaxFieldOfView = 80;
    /// <summary>
    /// Max orthographic size (half height in meters).
    /// (Max zoom out)
    /// </summary>
    [Tooltip("The maximum size when in orthographic mode (Max zoom out).")]
    public float MaxOrthographicSize = 30f;
    /// <summary>
    /// Min orthographic size (half height in meters).
    /// (Max zoom in)
    /// </summary>
    [Tooltip("The minimum size when in orthographic mode (Max zoom in).")]
    public float MinOrthographicSize = 0.1f;

    #endregion

    /// <summary>
    /// Set the active camera to the main camera if it isn't already set.
    /// </summary>
    private void Start() {
      if (ActiveCamera == null) {
        ActiveCamera = Camera.main;
      }
    }

    /// <summary>
    /// Detects touches on screen. You need 2 touches to perform a zoom.
    /// Makes adjustments to camera orthographic size or camera field of view
    /// depending on camera settings.
    ///
    /// </summary>
    private void Update() {
      // If there are two touches on the device...
      if (Input.touchCount == 2) {
        // Store both touches.
        var touchZero = Input.GetTouch(0);
        var touchOne = Input.GetTouch(1);

        // Find the position in the previous frame of each touch.
        Vector2 touchZeroPrevPos = touchZero.position - touchZero.deltaPosition;
        Vector2 touchOnePrevPos = touchOne.position - touchOne.deltaPosition;

        // Find the magnitude of the vector (the distance) between the touches in each frame.
        float prevTouchDeltaMag = (touchZeroPrevPos - touchOnePrevPos).magnitude;
        float touchDeltaMag = (touchZero.position - touchOne.position).magnitude;

        // Find the difference in the distances between each frame.
        float deltaMagnitudeDiff = prevTouchDeltaMag - touchDeltaMag;

        // If the camera is orthographic...
        if (ActiveCamera.orthographic) {
          // ... change the orthographic size based on the change in distance between the touches.
          ActiveCamera.orthographicSize += deltaMagnitudeDiff * OrthoZoomSpeed;

          // Make sure the orthographic size is clamped.
          ActiveCamera.orthographicSize = Mathf.Clamp(ActiveCamera.orthographicSize,
            MinOrthographicSize, MaxOrthographicSize);
        } else {
          // Otherwise change the field of view based on the change in distance between the touches.
          ActiveCamera.fieldOfView += deltaMagnitudeDiff * PerspectiveZoomSpeed;

          // Clamp the field of view to make sure it's between min and max values.
          ActiveCamera.fieldOfView = Mathf.Clamp(ActiveCamera.fieldOfView,
            MinFieldOfView, MaxFieldOfView);
        }
      }
    }
  }
}
