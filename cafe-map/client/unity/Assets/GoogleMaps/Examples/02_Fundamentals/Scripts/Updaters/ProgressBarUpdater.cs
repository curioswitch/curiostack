using UnityEngine;
using UnityEngine.UI;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class updates a progress bar widget based on values provided by the Maps SDK during the
  /// loading process.
  /// </summary>
  public class ProgressBarUpdater : MonoBehaviour {
    /// <summary>
    /// Reference to the <see cref="BaseMapLoader"/> (which knows about <see cref="MapsService"/>).
    /// </summary>
    /// <remarks>
    /// The <see cref="BaseMapLoader"/> handles the basic loading of a map region and provides
    /// default styling parameters and loading errors management.
    /// </remarks>
    public BaseMapLoader BaseMapLoader;

    [Tooltip("The Image used to display loading progress.")]
    public Scrollbar ProgressBar;

    [Tooltip("The current progression value as a percentage.")]
    public Text ProgressBarText;

    /// <summary>
    /// Make sure all required parameters are given, and connect to <see cref="MapsService"/>'s
    /// <see cref="Google.Maps.Event.MapEvents.Progress"/> event so we can display loading progress
    /// on screen.
    /// </summary>
    void Awake() {
      if (BaseMapLoader == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, BaseMapLoader, "Base Map Loader", "is required for this script to work."));

        return;
      }

      HideProgressBar();
    }

    /// <summary>
    /// Registers interests to loading and progress events with the Maps SDK.
    /// </summary>
    /// <remarks>
    /// Triggers a reloading of the map to reflect the current instructions.
    /// </remarks>
    private void OnEnable() {
      // Sign up to event called whenever progress is updated, using the defined image to display
      // this progress.
      BaseMapLoader.MapsService.Events.MapEvents.Progress.AddListener(OnMapLoadProgress);
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.AddListener(OnMapLoadComplete);
      ClearAndReload();
    }

    /// <summary>
    /// UnRegisters interests to loading and progress events with the Maps SDK.
    /// </summary>
    /// <remarks>
    /// Triggers a reloading of the map to reflect the current instructions.
    /// </remarks>
    private void OnDisable() {
      BaseMapLoader.MapsService.Events.MapEvents.Progress.RemoveListener(OnMapLoadProgress);
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.RemoveListener(OnMapLoadComplete);
      ClearAndReload();
    }

    /// <summary>
    /// On loading started, resets the progress bar.
    /// </summary>
    void OnMapLoadStarted() {
      // Reset the progress bar.
      if (ProgressBar != null) {
        ProgressBar.size = 0;
      }

      if (ProgressBarText != null) {
        ProgressBarText.text = "0%";
      }
    }

    /// <summary>
    /// On loading completed, hide the progress bar.
    /// </summary>
    void OnMapLoadComplete(MapLoadedArgs args) {
      HideProgressBar();
    }

    /// <summary>
    /// Updates the loading bar image based on the progress from a
    /// <see cref="Google.Maps.Event.MapEvents.Progress"/> event.
    /// </summary>
    /// <param name="args"><see cref="Google.Maps.Event.MapEvents.Progress"/>.</param>
    void OnMapLoadProgress(MapLoadProgressArgs args) {
      ShowProgressBar(args.Progress);
    }

    /// <summary>
    /// Destroys all augmented data and map features before reloading the map.
    /// </summary>
    /// <remarks><para>
    /// When new events are added to the maps service, the area needs to be reloaded for these to
    /// take effect.
    /// </para><para>
    /// This can also have an impact on all augmented objects in the scene, which need to be
    /// updated accordingly. In this case, we remove all roads and buildings gizmos if any.
    /// </para></remarks>
    private void ClearAndReload() {
      // Update the map.
      BaseMapLoader.ClearMap();
      BaseMapLoader.LoadMap();
    }

    /// <summary>
    /// Displays the progress bar and updates its values.
    /// </summary>
    /// <param name="value">The value to display in the progress bar.</param>
    private void ShowProgressBar(float value) {
      if (ProgressBar != null) {
        ProgressBar.gameObject.SetActive(true);
        ProgressBar.size = value;
      }

      if (ProgressBarText != null) {
        ProgressBarText.text = (value * 100).ToString("N0") + "%";
      }
    }

    /// <summary>
    /// Hides the progress bar and resets its values.
    /// </summary>
    private void HideProgressBar() {
      if (ProgressBar != null) {
        ProgressBar.gameObject.SetActive(false);
        ProgressBar.size = 0;
      }

      if (ProgressBarText != null) {
        ProgressBarText.text = "0%";
      }
    }
  }
}
