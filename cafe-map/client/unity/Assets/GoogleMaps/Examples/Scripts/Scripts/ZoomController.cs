using System;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Class for connecting a <see cref="Slider"/> to zooming <see cref="Camera.main"/> in and out.
/// </summary>
[RequireComponent(typeof(SliderController))]
public sealed class ZoomController : MonoBehaviour {
  [Tooltip("Distance from ground when camera is considered to be zoomed all the way in.")]
  public float NearDistance = 480f;

  [Tooltip("Distance from ground when camera is considered to be zoomed all the way out.")]
  public float FarDistance = 4000f;

  [Tooltip("Distance camera should start at (must be within range of the above Near and Far "
      + "Distances, or an error will be shown.")]
  public float StartDistance = 480f;

  /// <summary>
  /// Optional <see cref="Action"/> called when the <see cref="ZoomingCamera"/>'s position is
  /// changed by this <see cref="ZoomController"/>.
  /// </summary>
  public Action OnChange;

  /// <summary><see cref="Camera"/> this <see cref="ZoomController"/> operates on.</summary>
  /// <remarks>
  /// This variable is auto-filled from <see cref="Camera.main"/> the first time it is accessed.
  /// <para>
  /// This variable is used instead of <see cref="Camera.main"/> to avoid per-frame searching for an
  /// in scene <see cref="Camera"/> tagged as "Main Camera", instead performing this search only
  /// once and storing the result for future re-use.
  /// </para></remarks>
  public Camera ZoomingCamera {
    get {
      // If we have already found the main Camera (and it is has not yet been destroyed), return it
      // now.
      if (_ZoomingCamera != null) {
        return _ZoomingCamera;
      }

      // Try to find the main Camera, showing an error if it can't be found.
      _ZoomingCamera = Camera.main;
      if (_ZoomingCamera == null) {
        Debug.LogError(ExampleErrors.NullMainCamera(this));
      }
      return _ZoomingCamera;
    }
  }

  /// <summary>Locally stored copy of <see cref="Camera"/>.</summary>
  private Camera _ZoomingCamera;

  /// <summary>
  /// Required component for getting input from slider, which is animated smoothly moving up and
  /// down until player input is detected.
  /// </summary>
  private SliderController SliderController;

  /// <summary>Connect camera to slider.</summary>
  private void Awake() {
    // Make sure the given near and far distances are valid, skipping setup if not.
    if (!VerifyRange()) {
      return;
    }

    // Get required SliderController for getting input from UI Slider.
    SliderController = GetComponent<SliderController>();

    // Make sure that given start distance is within range of given near and far distances.
    if (StartDistance < NearDistance || StartDistance > FarDistance) {
      Debug.LogError(ExampleErrors
          .OutsideRange(this, StartDistance, "Start Distance", NearDistance, FarDistance));
      StartDistance = Mathf.Clamp(StartDistance, NearDistance, FarDistance);
    }

    // Convert start distance to a percent of near and far distances, and adjust slider to this
    // value.
    float sliderFraction = (StartDistance - NearDistance) / (FarDistance - NearDistance);
    OnSlider(sliderFraction);

    // Set slider's starting value, and only after have done so, connect future changes in slider
    // input to zooming in and out.
    SliderController.SetStartingValue(sliderFraction);
    SliderController.OnChange += OnSlider;
  }

  /// <summary>Respond to slider changing value.</summary>
  private void OnSlider(float value) {
    // Set current zoom based on Slider's current value, converted from a 0f to 1f fraction into a
    // value between the given near and far zoom distances.
    float lerpedDistance = Mathf.Lerp(NearDistance, FarDistance, value);
    ZoomTo(lerpedDistance);
  }

  /// <summary>Zoom the <see cref="ZoomingCamera"/> to a given distance.</summary>
  /// <remarks>
  /// Distance to zoom to (assumed to be positive and within range of <see cref="NearDistance"/> and
  /// <see cref="FarDistance"/>.
  /// </remarks>
  private void ZoomTo(float distance) {
    // Zoom to given distance.
    ZoomingCamera.transform.localPosition = Vector3.back * distance;

    // Use On Change Action if given to inform other classes of change in Camera's position.
    if (OnChange != null) {
      OnChange();
    }
  }

  /// <summary>
  /// Start animating zooming in and out, which will continue until input is received from the
  /// player.
  /// </summary>
  public void StartAnimating() {
    SliderController.StartAnimating();
  }

  /// <summary>
  /// Make sure the given <see cref="NearDistance"/> and <see cref="FarDistance"/> define a valid
  /// distance range.
  /// </summary>
  /// <returns>
  /// True if <see cref="NearDistance"/> and <see cref="FarDistance"/> are both positive, and if
  /// <see cref="NearDistance"/> is greater than <see cref="FarDistance"/> (otherwise a specific
  /// error message will be shown).
  /// </returns>
  private bool VerifyRange() {
    // Make sure both given distances are positive.
    if (NearDistance <= 0f) {
      Debug.LogError(ExampleErrors.NotGreaterThanZero(this, NearDistance, "Near Distance"));
      return false;
    }
    if (FarDistance <= 0f) {
      Debug.LogError(ExampleErrors.NotGreaterThanZero(this, FarDistance, "Far Distance"));
      return false;
    }

    // Make sure the given far distance is greater than near distance.
    if (FarDistance <= NearDistance) {
      Debug.LogError(ExampleErrors
          .NotGreaterThan(this, FarDistance, "Far Distance", NearDistance, "Near Distance"));
      return false;
    }

    // If have reached this point then given near and far distances are valid, so return true.
    return true;
  }
}
