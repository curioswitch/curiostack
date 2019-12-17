using Google.Maps;
using Google.Maps.Event;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Controller for displaying the progress of <see cref="MapsService"/> loading on an
/// <see cref="Image"/> element.
/// </summary>
[RequireComponent(typeof(MapsService))]
public class LoadingBarController : MonoBehaviour {
  [Tooltip("The Image used to display loading progress.")]
  public Image FillImage;

  /// <summary>
  /// Tracks whether an <see cref="Google.Maps.Event.MapEvents.Progress"/> event was fired since the
  /// last time loading finished.
  /// </summary>
  /// <remarks>Helps avoid flashing the loading bar.</remarks>
  private bool Loading;

  /// <summary>
  /// Make sure all required parameters are given, and connect to <see cref="MapsService"/>'s
  /// <see cref="Google.Maps.Event.MapEvents.Progress"/> event so we can display loading progress
  /// on screen.
  /// </summary>
  private void Awake() {
    // Verify an image has been given to use for showing progress.
    if (FillImage == null) {
      // Note: 'name' and 'GetType()' just give the name of the GameObject this script is on, and
      // the name of this script respectively.
      Debug.LogErrorFormat("No Image defined for {0}.{1}, which needs an image to operate!\n"
          + "Please define an Image for {0}.{1}.FillImage to be used to display loading progress.",
          name, GetType());
      return;
    }

    // Sign up to event called whenever progress is updated, using the defined image to display
    // this progress.
    GetComponent<MapsService>().Events.MapEvents.Progress.AddListener(OnMapLoadProgress);
  }

  /// <summary>
  /// Updates the loading bar image based on the progress from a
  /// <see cref="Google.Maps.Event.MapEvents.Progress"/> event.
  /// </summary>
  /// <param name="args"><see cref="Google.Maps.Event.MapEvents.Progress"/></param>
  private void OnMapLoadProgress(MapLoadProgressArgs args) {
    if (args.Progress < 1.0f) {
      // If progress is less than 100%, fill the appropriate amount of the image.
      FillImage.fillAmount = args.Progress;
      Loading = true;
    } else if (Loading) {
      // If loading has finished, hide the loading bar.
      FillImage.fillAmount = 1.0f;
      StartCoroutine(HideLoadingBar());
      Loading = false;
    }
  }

  /// <summary>Hides the loading bar after waiting for half a second.</summary>
  private IEnumerator HideLoadingBar() {
    yield return new WaitForSeconds(0.5f);
    FillImage.fillAmount = 0;
  }
}
