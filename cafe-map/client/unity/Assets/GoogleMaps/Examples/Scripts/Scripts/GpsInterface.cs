using System;
using System.Collections;
using Google.Maps.Coord;
using UnityEngine;
using UnityEngine.Events;

/// <summary>Component for providing/simulating a player's real world location.</summary>
/// <remarks>
/// This component uses gps Location Service when on device, and WASD-keyboard input when in
/// Editor, PC or WebGL.
/// </remarks>
public sealed class GpsInterface : MonoBehaviour {
#region Events
  /// <summary>Triggered whenever movement is detected.</summary>
  public LocationEvent OnMove = new LocationEvent();

  /// <summary>Triggered when movement is detected for the first time in this scene.</summary>
  /// <remarks>
  /// <see cref="OnMove"/> will also be called on this first frame of movement if
  /// <see cref="OnMove"/> is also specified.
  /// </remarks>
  public LocationEvent OnFirstMove = new LocationEvent();

  /// <summary>Triggered when gps fails to start on device.</summary>
  /// <remarks>
  /// Passes in true if gps fails because the user has not enabled Location Service, and a
  /// <see cref="string"/> error describing the cause of the failure.
  /// </remarks>
  public ErrorEvent OnFail = new ErrorEvent();
#endregion

#region Parameters
  [Tooltip("Desired accuracy of user's real world location (in meters).")]
  public float Accuracy = 0.1f;

  [Tooltip("Distance of real world movement required to trigger an update of gps position (in "
      + "meters).")]
  public float UpdateDistance = 0.1f;

  [Tooltip("Timeout to wait in seconds for gps Location Service to initialize before declaring "
      + "that Location Service have failed to start.")]
  public int Timeout = 20;

  [Tooltip("Wait until StartLocationService() is called before starting Location Service?"
      + "Otherwise Location Service will be started automatically on Start.")]
  public bool WaitToStart;

  [Header("Editor"), Tooltip("Starting latitude when in Editor mode.")]
  public double Latitude = 40.6892199;

  [Tooltip("Starting longitude when in Editor mode.")]
  public double Longitude = -74.044601;

  [Tooltip("Speed of keyboard based movement in Editor Mode (achieved using arrow keys) in "
      + "degrees of latitude/longitude per second. Note that small values should be used (in the "
      + "scale of 0.001 degrees) to avoid massive jumps in movement.")]
  public double EditorSpeed = 0.001;
#endregion

#region Variables
  /// <summary>The various potential states of the Location Service.</summary>
  /// <remarks>
  /// Used to prevent redundantly starting Location Services when it is already started/starting.
  /// </remarks>
  private enum ServiceStatus {
    Inactive,
    Starting,
    Active
  }

  /// <summary>The current state of the Location Service.</summary>
  private ServiceStatus Status = ServiceStatus.Inactive;

  /// <summary>Has at least one gps location already been detected?</summary>
  /// <remarks>
  /// This <see cref="bool"/> is used to detect the first gps location found, so that it can
  /// optionally be passed into <see cref="OnFirstMove"/> <see cref="UnityEvent"/> as the real world
  /// location to load.
  /// </remarks>
  private bool HavePreviousLocation;

  /// <summary>Last detected/simulated latitude.</summary>
  private double CurrentLatitude;

  /// <summary>Last detected/simulated longitude.</summary>
  private double CurrentLongitude;

  /// <summary>Are we simulating gps coordinates using keyboard input?</summary>
  /// <remarks>This allows gps-dependent code to be debugged in the Unity Editor.</remarks>
  private bool SimulatingGps;
#endregion

#region Start
  /// <summary>Starts <see cref="Input.location"/> if appropriate.</summary>
  private void Awake() {
    // Detect if we are running in the Unity Editor (or a Web GL or PC build) or on device, as this
    // will determine if we get gps coordinates from the device, or simulate them using keyboard
    // input.
    SimulatingGps = !Application.isMobilePlatform;

    // Automatically start Location Service if the user has enabled this.
    if (!WaitToStart) {
      StartLocationService();
    }
  }

  /// <summary>Starts <see cref="Input.location"/> if it has not already been started.</summary>
  /// <remarks>
  /// This method can safely be called repeatedly if <see cref="Input.location"/> fails to start.
  /// </remarks>
  /// <param name="onFail">Optional <see cref="ErrorEvent"/> to call on failure.</param>
  public void StartLocationService(ErrorEvent onFail = null) {
    // Store event to be called on failure (if given).
    OnFail = onFail;

    // Skip starting Location Service if it is already started/starting.
    if (Status != ServiceStatus.Inactive) {
      return;
    }

    // If in Editor, WebGL or PC build, set a default location and flag that Location Service is now
    // active (so we can use the keyboard to simulate changes in gps location).
    if (SimulatingGps) {
      // Flag that Location Service have now been 'started'. This boolean is used to prevent
      // simulating gps locations until gps would have been started on device (either by setting
      // Wait To Start to false, or by calling StartLocationService from an external class).
      Status = ServiceStatus.Active;

      // Set starting location from given Editor only values, then immediately return this as the
      // first 'received' gps location.
      CurrentLatitude = Latitude;
      CurrentLongitude = Longitude;
      HandleMovement(new LatLng(CurrentLatitude, CurrentLongitude));
      return;
    }

    // If on device, check if the Location Service can be enabled. If on device, this means checking
    // if the user has given permission for this application to access the user's location.
    if (Input.location.isEnabledByUser) {
      // Start Location Service, awaiting a response from the device to confirm Location Service has
      // started.
      Status = ServiceStatus.Starting;
      StartCoroutine(AwaitLocationServiceStart());
      return;
    }

    // If Location Service cannot be enabled, this is either because the user has not given
    // permission to access the user's location, or we are not on device. Pass this reason to
    // error handling method for showing and optionally for passing into On Fail event (if event has
    // been provided).
    string reasonForFailure = Application.isMobilePlatform
        ? "the user has not given permission for this application to access their location"
        : "we are not currently on a mobile device";
    string errorMessage = string.Concat("Location Service failed to start, as ", reasonForFailure);
    HandleError(errorMessage);
  }
#endregion

#region Update
  /// <summary>
  /// Detect/simulate gps movement (based on whether on a gps enabled device or not.
  /// </summary>
  private void Update() {
    // Await the start of Location Service. This requires Start Location Service to be called, or
    // Wait To Start to be disabled. If on device, this also requires confirmation from the device
    // that Location Service has successfully started.
    if (Status != ServiceStatus.Active) {
      return;
    }

    // If not on device, simulate gps with keyboard input, otherwise detect gps input from device.
    if (SimulatingGps) {
      SimulateGps();
    } else {
      DetectGps();
    }
  }

  /// <summary>Use keyboard input to simulate gps movement.</summary>
  private void SimulateGps() {
    // Get smoothed keyboard input, skipping gps movement if no input this frame.
    Vector2 input = new Vector2(Input.GetAxis("Vertical"), Input.GetAxis("Horizontal"));
    bool haveHorizontalInput = IsZero(input.x);
    bool haveVerticalInput = IsZero(input.y);
    if (!haveHorizontalInput && !haveVerticalInput) {
      return;
    }

    // Convert keyboard input into simulated gps movement.
    if (haveHorizontalInput) {
      CurrentLongitude += input.x * Time.smoothDeltaTime * EditorSpeed;
    }
    if (haveVerticalInput) {
      CurrentLatitude += input.y * Time.smoothDeltaTime * EditorSpeed;
    }

    // Use events to inform other classes of movement of the user's simulated gps location.
    HandleMovement(new LatLng(CurrentLatitude, CurrentLongitude));
  }

  /// <summary>Detect gps movement from device.</summary>
  private void DetectGps() {
    // Get input from device's sensor.
    LatLng input = new LatLng(Input.location.lastData.latitude, Input.location.lastData.longitude);

    // If this is not the first location detected by the gps, make sure it is not the same as the
    // last detected gps location.
    if (HavePreviousLocation
        && AreEqual(input.Lat, CurrentLatitude) && AreEqual(input.Lng, CurrentLongitude)) {
      return;
    }

    // Use events to inform other classes of movement of the user's gps location.
    HandleMovement(new LatLng(CurrentLatitude, CurrentLongitude));
  }
#endregion

#region Internal
  /// <summary>
  /// Start <see cref="Input.location"/> on the current mobile device, awaiting confirmation of
  /// successful start, an error, or a timeout based on  provided <see cref="Timeout"/> value.
  /// </summary>
  private IEnumerator AwaitLocationServiceStart() {
    // Start device's Location Service with specified accurately and update values
    Input.location.Start(Accuracy, UpdateDistance);

    // Await a response from the device, timing out if no response is received after defined Timeout
    // period.
    int timeoutRemaining = Timeout;
    while (Input.location.status == LocationServiceStatus.Initializing && timeoutRemaining > 0) {
      yield return new WaitForSecondsRealtime(1f);
      timeoutRemaining--;
    }

    // Handle device response to see if Location Service successfully started, failed to start, or
    // timed out.
    switch (Input.location.status) {
      // If Service has successfully started, update its status so that Update routine can start
      // returning the user's gps location.
      case LocationServiceStatus.Running:
        Status = ServiceStatus.Active;
        break;

      // If Service is still trying to start even after timeout has expired, consider Service to
      // have timed out.
      case LocationServiceStatus.Initializing:
        string errorMessage = string.Format("Location Service failed to start, timing out after "
          + "{0} seconds.",
          Timeout);
        HandleError(errorMessage);
        break;

      // If an error occured, handle this error, showing it to the user and optionally passing it
      // into On Fail event (if event has been provided).
      default:
        HandleStoppage();
        break;
    }

    // Flag location services is no longer starting (so it can be restarted if desired).
    Status = ServiceStatus.Inactive;
  }

  /// <summary>
  /// Handle an unexpected stoppage of Location Service, showing an error or using defined
  /// <see cref="OnFail"/> <see cref="UnityEvent"/> to inform other classes of failure.
  /// </summary>
  private void HandleStoppage() {
    // Start by checking if Location Service was starting or running when this unexpected stoppage
    // occured.
    string errorContext;
    switch (Status) {
      case ServiceStatus.Starting:
        errorContext = "Location Service was trying to start running when it";
        break;

      case ServiceStatus.Active:
        errorContext = "Location Service was running when it";
        break;

      default:
        throw new ArgumentOutOfRangeException("Status", Status, "Unexpected error status.");
    }

    // Determine the cause of the unexpected stoppage from Location Service's status code.
    string errorCause;
    switch (Input.location.status) {
      case LocationServiceStatus.Failed:
        errorCause = "failed";
        break;

      case LocationServiceStatus.Stopped:
        errorCause = "was stopped unexpectedly";
        break;

      default:
        throw new ArgumentOutOfRangeException(
            "Input.location.status", Input.location.status, "Unexpected stopped status.");
    }

    // Combine into an error message, to be shown and optionally to be passed into On Fail event (if
    // event has been provided).
    string errorMessage
        = string.Format("{0} {1} ({2}.{3} received a Input.location.status of {4}).",
        errorContext, errorCause, name, GetType(), Input.location.status);
    HandleError(errorMessage);
  }
  /// <summary>
  /// Show a given error message and (if provided) pass message into <see cref="OnFail"/> event.
  /// </summary>
  /// <param name="errorMessage">Description of error that has occured.</param>
  private void HandleError(string errorMessage) {
    // Show given error message.
    Debug.LogError(errorMessage);

    // Use On Fail event if provided to inform other classes of failure. Note that because this is a
    // Unity Event, a null error will not occur if no listeners have been provided.
    OnFail.Invoke(errorMessage);
  }

  /// <summary>
  /// Try to use <see cref="OnMove"/> <see cref="UnityEvent"/> (if given) to inform other classes of
  /// change in gps location.
  /// </summary>
  /// <remarks>
  /// If this is the first time a gps location has been returned, <see cref="OnFirstMove"/> will be
  /// called as well (if this <see cref="UnityEvent"/> has been given).
  /// </remarks>
  private void HandleMovement(LatLng location) {
    if (!HavePreviousLocation) {
      OnFirstMove.Invoke(location);
      HavePreviousLocation = true;
    }
    OnMove.Invoke(location);
  }

  /// <summary>
  /// Query if a <see cref="float"/> is equal to zero (within range of <see cref="float"/> rounding
  /// errors.
  /// </summary>
  /// <param name="value"><see cref="float"/> to check.</param>
  private static bool IsZero(float value) {
    return Mathf.Abs(value) <= float.Epsilon;
  }

  /// <summary>Query if two <see cref="double"/>s are equal.</summary>
  /// <param name="first">First <see cref="double"/> to check.</param>
  /// <param name="second">Second <see cref="double"/> to check.</param>
  private static bool AreEqual(double first, double second) {
    double difference = second - first;
    double absoluteDifference = difference >= 0.0 ? difference : -difference;
    return absoluteDifference <= double.Epsilon;
  }

  /// <summary>
  /// Style of <see cref="UnityEvent"/> to use for <see cref="OnMove"/> and
  /// <see cref="OnFirstMove"/>.
  /// </summary>
  [Serializable]
  public class LocationEvent : UnityEvent<LatLng> { }

  /// <summary>Style of <see cref="UnityEvent"/> to use for <see cref="OnFail"/>.</summary>
  /// <remarks>This <see cref="UnityEvent"/> passes a <see cref="string"/> error message.</remarks>
  [Serializable]
  public class ErrorEvent : UnityEvent<string> { }
#endregion
}
