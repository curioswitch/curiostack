using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Events;

/// <summary>
/// An augmented reality <see cref="Camera"/> controller, which also allows mouse-based movement in
/// Editor mode.
/// </summary>
/// <remarks>
/// Intended to be attached to <see cref="Camera.main"/>'s <see cref="GameObject"/> for controlling
/// the scene view.
/// <para>
/// In Editor mode the view is moved using the mouse. On device <see cref="Gyroscope"/> input is
/// used to synchronize device movement to view movement.
/// </para></remarks>
[RequireComponent(typeof(Camera))]
public class ArCameraController : MonoBehaviour {
#region Constants
  /// <summary>
  /// <see cref="Quaternion"/> used to convert from <see cref="Gyroscope"/> input to a
  /// <see cref="Camera"/> world space rotation.
  /// </summary>
  private static readonly Quaternion GyroscopeToCamera = new Quaternion(0, 0, 1, 0);
#endregion

#region Events
  /// <summary>
  /// Optional <see cref="UnityEngine.Event"/> called whenever the <see cref="Camera"/>'s view
  /// changes.
  /// </summary>
  public UnityEvent OnChange = new UnityEvent();
#endregion

#region Parameters
  [Tooltip("UI element to show augmented reality background in. If on device this will be the "
      + "real-time view of the world. However, a default texture should be applied for use in "
      + "Editor, or if a backward facing camera cannot be found on device to render a real-time "
      + "view of the world.")]
  public RawImage Background;

  [Tooltip("Look sensitivity in Editor mode. Sensitivity can be specified separately for x and y "
      + "axes (looking left/right and up/down respectively).")]
  public Vector2 EditorSensitivity = new Vector2(0.02f, 0.02f);

  [Tooltip("When enabled, moving the mouse down will look up in Editor mode.")]
  public bool InvertY;
#endregion

#region Variables
  /// <summary>The rendered view of the backward facing device camera.</summary>
  private WebCamTexture ArTexture;

  /// <summary>The position of the mouse last frame.</summary>
  /// <remarks>
  /// This is used to determine if the mouse has moved this frame, which will cause the
  /// <see cref="Camera"/> to be moved in response.
  /// </remarks>
  private Vector3 LastMousePosition;

  /// <summary>
  /// Convenient reference to <see cref="Background"/>'s <see cref="RectTransform"/>.
  /// </summary>
  private RectTransform BackgroundRect;

  /// <summary>
  /// UI element describing how to fit <see cref="Background"/>'s image to screens with different
  /// aspect ratios.
  /// </summary>
  private AspectRatioFitter BackgroundFitter;

  /// <summary><see cref="Canvas"/> containing <see cref="Background"/>.</summary>
  private Canvas BackgroundCanvas;

  /// <summary>
  /// <see cref="Background"/>'s dimensions stored as <see cref="Vector2"/> for convenience.
  /// </summary>
  private Vector2 BackgroundMovementRange;

  /// <summary>
  /// <see cref="Screen"/>'s dimensions, stored as <see cref="Vector2"/> for convenience.
  /// </summary>
  private Vector2 ScreenDimensions;

  /// <summary><see cref="Camera"/>'s current euler angles.</summary>
  /// <remarks>
  /// This is stored as a <see cref="Vector3"/> to avoid continually reading and writing to
  /// the <see cref="Camera"/>'s <see cref="Transform.eulerAngles"/> (which are not always
  /// reliable).
  /// </remarks>
  private Vector3 CameraAngles;

  /// <summary>
  /// Convenient reference to <see cref="Camera.main"/>'s <see cref="Transform"/> in the current
  /// scene.
  /// </summary>
  private Transform MainCamera;

  /// <summary>Device's <see cref="Gyroscope"/> if found (null if not).</summary>
  private Gyroscope Gyroscope;

  /// <summary>Is gyroscopic input available?</summary>
  /// <remarks>
  /// For this to be true we must be on device (not in Editor mode) and the device must have a
  /// gyroscopic sensor.
  /// </remarks>
  private bool GyroscopeAvailable;

  /// <summary>Is mouse input available?</summary>
  /// <remarks>
  /// For this to be true we must be in Editor mode, a Web GL build or a PC build.
  /// </remarks>
  private bool MouseAvailable;

  /// <summary>Is augmented reality mode available?</summary>
  /// <remarks>
  /// For this to be true we must be on a device (not in Editor mode) and the device must have at
  /// least one backward facing camera.
  /// </remarks>
  private bool AugmentedRealityCameraAvailable;
#endregion

#region Start
  /// <summary>
  /// Store the initial position of the mouse (if in Editor) or the gyroscope (if on device with a
  /// gyroscope), so we can detect when movement should be performed.
  /// </summary>
  private void Awake() {
    // Verify required components, skipping setup if any missing.
    if (!VerifyParameters()) {
      // Disable this script to avoid per-frame error spamming.
      enabled = false;
      return;
    }

    // Store Screen's dimensions as a Vector2 for convenience (rather than having to convert from
    // integer Width and Height to floats potentially every frame).
    ScreenDimensions = new Vector2(Screen.width, Screen.height);

    // Get the main Camera in this scene, resetting its rotation in preparation for receiving input
    // from mouse or gyroscope.
    FindAndSetupMainCamera();

    // Find and setup the Background image's Canvas for Augmented Reality mode. If this Canvas
    // could not be found, disable this script and skip further setup.
    FindAndSetupBackgroundCanvas();
    if (BackgroundCanvas == null) {
      enabled = false;
      return;
    }

    // Setup Background image for augmented reality mode.
    SetupBackground();

    // If we are on device , see if the device has a backward facing Camera for rendering an
    // Augmented Reality view of the real world.
    if (Application.isMobilePlatform) {
      // Find all available cameras on the current device.
      WebCamDevice[] deviceCameras = WebCamTexture.devices;

      // If the device has no cameras, disable augmented reality mode.
      AugmentedRealityCameraAvailable = deviceCameras.Length != 0;
      if (AugmentedRealityCameraAvailable) {
        // Find the backward facing camera.
        bool cameraFound = false;
        foreach (WebCamDevice deviceCamera in deviceCameras) {
          if (deviceCamera.isFrontFacing) {
            // Skip cameras facing towards the user.
            continue;
          }

          // Use the first found backward facing camera, and flag that an augmented reality camera
          // was successfully found.
          SetupArCamera(deviceCamera);
          cameraFound = true;
          break;
        }

        // Show an error if no backward facing cameras were found.
        if (!cameraFound) {
          Debug.LogErrorFormat("{0}.{1} was unable to detect a backward facing camera in the "
              + "device's {2} {3}.\nDefaulting to non-AR camera mode.",
              name, GetType(), deviceCameras.Length,
              deviceCameras.Length == 1 ? "camera" : "cameras");
          AugmentedRealityCameraAvailable = false;
        }
      }
      else {
        Debug.LogWarningFormat("{0}.{1} was unable to detect any cameras on the current device.\n"
            + "Defaulting to non-AR camera mode.",
            name, GetType());
      }

      // See if gyroscopic input is available on this device.
      GyroscopeAvailable = SystemInfo.supportsGyroscope;
      if (GyroscopeAvailable) {
        // Make sure the gyroscope is enabled.
        Gyroscope = Input.gyro;
        Gyroscope.enabled = true;
      }
      else {
        Debug.LogWarningFormat("No gyroscope found on the current device, so unable to setup "
            + "gyroscopic Camera controls in {0}.{1}",
            name, GetType());
      }

      // Disable auto-rotating the screen so that when the phone is moved around mode, the screen
      // does not automatically re-orient between portrait and landscape modes.
      Screen.autorotateToPortrait = false;
      Screen.autorotateToPortraitUpsideDown = false;
      Screen.autorotateToLandscapeLeft = false;
      Screen.autorotateToLandscapeRight = false;
      Screen.orientation = ScreenOrientation.Portrait;
    } else {
      // If we are in Editor, or in a Web GL or PC build, store the initial position of the mouse so
      // we can detect when the mouse is moved, as this will be used to drive Camera rotation.
      MouseAvailable = true;
      LastMousePosition = Input.mousePosition;

      // Lock the cursor to allow for mouse based Camera rotation without the cursor moving off the
      // screen.
      LockCursor(true);
    }
  }

  /// <summary>Find and setup <see cref="Camera.main"/>.</summary>
  private void FindAndSetupMainCamera() {
    MainCamera = Camera.main.transform;

    // Make sure the main Camera's up/down rotation is not looking further than straight up or
    // straight down.
    CameraAngles = new Vector3(
      ClampedVerticalAngle(MainCamera.eulerAngles.x),
      MainCamera.eulerAngles.y,
      MainCamera.eulerAngles.z);
    MainCamera.eulerAngles = CameraAngles;
  }

  /// <summary>Find and setup <see cref="Background"/>'s <see cref="Canvas"/>.</summary>
  /// <remarks>
  /// This should be called before <see cref="SetupBackground"/>, to make sure that any changes in
  /// the <see cref="Background"/>'s <see cref="RectTransform"/> reflect the final settings of its
  /// parent <see cref="Canvas"/>.
  /// </remarks>
  private void FindAndSetupBackgroundCanvas() {
    // Get the Canvas of the Background (a Canvas that is a parent of this Raw Image).
    Transform backgroundParent = Background.transform.parent;
    do {
      BackgroundCanvas = backgroundParent.GetComponent<Canvas>();
      if (BackgroundCanvas == null) {
        backgroundParent = backgroundParent.transform.parent;
      } else {
        break;
      }
    } while (backgroundParent != null);

    // If we could not find a parent canvas of Background, print an error to this effect.
    if (BackgroundCanvas == null) {
      Debug.LogErrorFormat("Unable to find the Canvas of {0}.{1}.Background RawImage.\nThe AR "
        + "Background will not be rendered if this RawImage is not under a Canvas.",
        name, GetType());
      return;
    }

    // Make sure the Background's Canvas is a child of the main Camera. This is needed so that
    // the Background canvas can be placed a suitable distance away from the main Camera (relative
    // to the view).
    if (BackgroundCanvas.transform.parent != MainCamera) {
      Debug.LogWarningFormat("Canvas of {0}.{1}.Background ({2}) was not a child of Main Camera "
          + "{3}.\nMaking {2} a child of {3} so Background can be positioned relative to the "
          + "Camera's view.",
          name, GetType(), BackgroundCanvas.name, MainCamera.name);
      BackgroundCanvas.transform.SetParent(MainCamera);
    }

    // Make sure that the Background is rendered in world space. This is because a screen space
    // rendered background would appear in front of all in-scene GameObjects (in the same way that
    // standard UI elements appear in front of the scene's geometry). Instead we want the Background
    // to appear behind all GameObjects in the scene, so that they are rendered in front of the
    // augmented reality background, appearing to be in the world.
    if (BackgroundCanvas.renderMode != RenderMode.WorldSpace) {
      Debug.LogWarningFormat("Canvas of {0}.{1}.Background ({2}) was not set to render in {3}.\n"
          + "Setting {2} to render in {3} so that in-scene GameObjects will be shown in front of "
          + "this Background AR view.",
          name, GetType(), BackgroundCanvas.name, RenderMode.WorldSpace);
      BackgroundCanvas.renderMode = RenderMode.WorldSpace;
    }

    // Determine how much to scale up the Background Canvas so that the Background exactly fills the
    // screen. This is needed because the Background is rendered not in screen space (where we can
    // easily specify dimensions relative to screen size), but in world space (in the scene itself),
    // again so that the Background will be rendered behind any in scene GameObjects.
    // We determine the size of the screen-filling Background Canvas as follows:
    //
    //            C
    //           ╱│╲
    //          ╱θ│θ╲               |B|/2
    //         ╱  │  ╲    tan(θ) = ───────
    //        ╱   │   ╲               D
    //       ╱    │    ╲
    //      ╱     │D    ╲    |B| = 2 × D × tan(θ)
    //     ╱      │      ╲
    //    ╱       │       ╲
    //   ╱        ├─┐ B/2  ╲
    //  ══════════╧═╧════════
    //            B
    //
    // In the above diagram C is the Camera, B is the Background image, and D is the distance
    // between the Camera and the Background image. |B| is the size of the Background, and if the
    // Background's exactly fills the screen, then θ will be half the field of view of the Camera.
    // Inserting this into the above equation gives us:
    float halfFieldOfView = Camera.main.fieldOfView / 2f;
    float backgroundDistance = BackgroundCanvas.transform.localPosition.z;
    float desiredBackgroundHeight
        = 2f * backgroundDistance * Mathf.Tan(halfFieldOfView * Mathf.Deg2Rad);

    // Get the current Background height, and use this to determine how much to scale the Background
    // Canvas so that it exactly fills the screen.
    float backgroundHeight = BackgroundCanvas.GetComponent<RectTransform>().rect.height;
    float differenceInScale = backgroundHeight / desiredBackgroundHeight;
    BackgroundCanvas.transform.localScale = Vector3.one * differenceInScale;
  }

  /// <summary>Setup <see cref="Background"/> to work in augmented reality mode.</summary>
  /// <remarks>
  /// This should be called after <see cref="FindAndSetupBackgroundCanvas"/>, to make sure that any
  /// changes in <see cref="Background"/>'s <see cref="RectTransform"/> reflect the final settings
  /// of its parent <see cref="Canvas"/>.
  /// </remarks>
  private void SetupBackground() {
    // Check if an Aspect Ratio Fitter has been added to the Background UI element, adding one now
    // if not. This component is needed to make sure the Background UI element is correctly
    // displayed for screens with different aspect ratios and orientations.
    BackgroundFitter = Background.GetComponent<AspectRatioFitter>();
    if (BackgroundFitter == null) {
      Debug.LogWarningFormat("No {0} found on {1} defined as {2}.{3}.Background.\nA {0} is needed "
          + "to make sure the rendered AR view is correctly scaled to screens of different aspect "
          + "ratios and orientations.\nAdding an {0} now.",
          typeof(AspectRatioFitter), typeof(RawImage), name, GetType());
      BackgroundFitter = Background.gameObject.AddComponent<AspectRatioFitter>();
      BackgroundFitter.aspectRatio = ScreenDimensions.x / ScreenDimensions.y;
      BackgroundFitter.aspectMode = AspectRatioFitter.AspectMode.HeightControlsWidth;
    }

    // Store Background's RectTransform, and make sure it is stretched to fill its parent Canvas.
    BackgroundRect = Background.rectTransform;
    if (AreDifferent(BackgroundRect.offsetMin, Vector2.zero)
        || AreDifferent(BackgroundRect.offsetMin, Vector2.zero)
        || AreDifferent(BackgroundRect.anchorMin, Vector2.zero)
        || AreDifferent(BackgroundRect.anchorMax, Vector2.one)) {
      Debug.LogWarningFormat("Defined {0}.{1}.Background ({2}) did not fill the screen.\nAdjusting "
          + "{2}'s RectTransform so that the default background image is relative to screen size.",
          name, GetType(), Background.name);
      BackgroundRect.offsetMin = Vector2.zero;
      BackgroundRect.offsetMax = Vector2.zero;
      BackgroundRect.anchorMin = Vector2.zero;
      BackgroundRect.offsetMax = Vector2.one;
    }
  }

  /// <summary>
  /// Connect a given device camera to the <see cref="Background"/> UI element, so the device
  /// camera's output can be used as an augmented reality display.
  /// </summary>
  /// <param name="arCamera">
  /// Device camera to use for augmented reality display (assumed to not be null).
  /// </param>
  private void SetupArCamera(WebCamDevice arCamera) {
    // Create a new texture to hold the rendered output of the camera each frame. The size of the
    // texture is equal to the screen size (so that the entire screen will be covered).
    ArTexture = new WebCamTexture(arCamera.name, Screen.width, Screen.height);

    // Make sure the device camera is rendering, and connect its output to the background UI
    // element.
    ArTexture.Play();
    Background.texture = ArTexture;
  }
#endregion

#region Update
  /// <summary>
  /// Every frame check if <see cref="Camera"/> rotation is required, performing this rotation if so
  /// and using <see cref="OnChange"/> <see cref="UnityEngine.Event"/> if given to inform other
  /// classes of change in <see cref="Camera"/>'s view.
  /// </summary>
  private void Update() {
    // If an augmented reality camera was found, keep its aspect ratio, orientation and angle synced
    // to its in-scene display.
    if (AugmentedRealityCameraAvailable) {
      // Compensate for changes in augmented reality camera's aspect ratio (if screen orientation
      // changes).
      float ratio = GetAspectRatio(ArTexture.width, ArTexture.height);
      BackgroundFitter.aspectRatio = ratio;

      // Compensate for phone being turned upside down.
      float verticalScale = ArTexture.videoVerticallyMirrored ? -1f : 1f;
      Background.rectTransform.localScale = new Vector3(1f, verticalScale, 1f);

      int orientation = -ArTexture.videoRotationAngle;
      Background.rectTransform.localEulerAngles = new Vector3(0f, 0f, orientation);
    }

    // If a gyroscopic sensor was found, use its rotation to rotate the Camera, synchronizing
    // rotation of the phone to rotation of the Camera.
    if (GyroscopeAvailable) {
      // Rotate the Camera based on gyroscopic input.
      MainCamera.localRotation = Gyroscope.attitude * GyroscopeToCamera;

      // Use On Change event to inform other classes of change in view. Note that because this is a
      // Unity Event, it will not generate a null error if no listeners have been specified.
      OnChange.Invoke();
    } else if (MouseAvailable) {
      // If in Editor, see if the mouse has moved this frame, which will trigger Camera movement.
      // Only the x and y (screen space) coordinates of the mouse's position are checked.
      if (!AreDifferentXy(Input.mousePosition, LastMousePosition)) {
        return;
      }

      // Determine the amount of mouse movement this frame.
      Vector2 mouseDelta = Input.mousePosition - LastMousePosition;

      // Multiply by chosen sensitivity, and determine new Camera angles.
      Vector2 cameraDelta = new Vector2(
          mouseDelta.x * EditorSensitivity.x, mouseDelta.y * EditorSensitivity.y);

      // If not inverting y-axis, make sure downward mouse movements aligns to downwards Camera
      // rotation.
      if (!InvertY) {
        cameraDelta.x *= -1f;
      }

      // Apply clamped Camera movement, clamping vertical rotation so that we will not look further
      // than straight up or straight down.
      CameraAngles = new Vector3(
        ClampedVerticalAngle(CameraAngles.x + cameraDelta.y),
        CameraAngles.y + cameraDelta.x,
        CameraAngles.z);
      MainCamera.eulerAngles = CameraAngles;

      // Use On Change event to inform other classes of change in view. Note that because this is a
      // Unity Event, it will not generate a null error if no listeners have been specified.
      OnChange.Invoke();

      // Store the new, last position of the mouse so we can detect when the mouse moves again.
      LastMousePosition = Input.mousePosition;
    }
  }
#endregion

#region Internal
  /// <summary>
  /// Lock/unlock the mouse cursor to allow for mouse based <see cref="Camera"/> movement.
  /// </summary>
  /// <param name="on">True to lock, false to unlock.</param>
  private void LockCursor(bool on) {
    Cursor.lockState = on ? CursorLockMode.Confined : CursorLockMode.None;
    Cursor.visible = !on;
  }

  /// <summary>
  /// Clamp an up/down angle in degrees to make sure it is not looking further than straight down
  /// or further than straight up.
  /// </summary>
  /// <param name="angle">Angle in degrees to clamp.</param>
  /// <returns>
  /// Angle if less than 90 degrees or greater than 270 degrees, a clamped version if between these
  /// values (clamped to the closest).
  /// </returns>
  private float ClampedVerticalAngle(float angle) {
    if (angle > 90f && angle < 270f) {
      return angle < 180f ? 90f : 270f;
    }
    return angle;
  }

  /// <summary>Return an aspect ratio from a given set of screen dimensions.</summary>
  /// <remarks>
  /// Given dimensions are converted from <see cref="int"/> to <see cref="float"/> for an accurate
  /// calculation.
  /// </remarks>
  /// <param name="width">Screen width in pixels.</param>
  /// <param name="height">Screen height in pixels.</param>
  private static float GetAspectRatio(int width, int height) {
    return (float)width / (float)height;
  }

  /// <summary>
  /// Query if two <see cref="Vector3"/>s are different in x and/or y axes (ignoring z axis).
  /// </summary>
  /// <param name="first">First <see cref="Vector3"/> to check.</param>
  /// <param name="second">Second <see cref="Vector3"/> to check.</param>
  private static bool AreDifferentXy(Vector3 first, Vector3 second) {
    return AreDifferent(first.x, second.x) || AreDifferent(first.y, second.y);
  }

  /// <summary>Query if two <see cref="Vector2"/>s are different.</summary>
  /// <param name="first">First <see cref="Vector2"/> to check.</param>
  /// <param name="second">Second <see cref="Vector2"/> to check.</param>
  private static bool AreDifferent(Vector2 first, Vector2 second) {
    return AreDifferent(first.x, second.x) || AreDifferent(first.y, second.y);
  }

  /// <summary>
  /// Query if two <see cref="float"/>s are different within range of <see cref="float"/> rounding
  /// errors.
  /// </summary>
  /// <param name="first">First <see cref="float"/> to check.</param>
  /// <param name="second">Second <see cref="float"/> to check.</param>
  private static bool AreDifferent(float first, float second) {
    return Mathf.Abs(second - first) >= float.Epsilon;
  }

  /// <summary>
  /// Verify that all required parameters have been correctly defined, showing a specific error and
  /// returning false if not.
  /// </summary>
  private bool VerifyParameters() {
    // Verify this scene has a main Camera.
    if (Camera.main == null) {
      Debug.LogError(ExampleErrors.NullMainCamera(this, "to keep the AR Compass Map in front of"));
      return false;
    }

    // Verify a Background UI element has been given, and that it has a default texture (to use as
    // a backup if augmented reality mode is not available).
    if (Background == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, Background, "Background",
          "to show the real world background on device"));
      return false;
    }
    if (Background.texture == null) {
      Debug.LogErrorFormat("No default texture applied to {0}, defined as {1}.{2}.Background.\n"
          + "This UI element must have a default texture to use if AR mode is not available on "
          + "device.",
          Background.name, name, GetType());
      return false;
    }

    // If we have reached this point then we have verified that all required parts are present and
    // properly setup.
    return true;
  }
#endregion
}
