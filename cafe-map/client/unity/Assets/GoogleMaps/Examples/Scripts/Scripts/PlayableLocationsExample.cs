using System.IO;
using Google.Maps;
using UnityEngine;

/// <summary>
/// Example demonstrating usage of the Playable Locations Service.
/// </summary>
public sealed class PlayableLocationsExample : MonoBehaviour {

  /// <summary>
  /// Option to clear all cached impressions at startup.
  /// </summary>
  public bool ClearCachedImpressionsAtStartup;

  /// <summary>
  /// Service used for reporting bad places.
  /// </summary>
  private PlayableLocationsService PlayableLocationsService;

  private void Awake() {
    var stateSaveFile = Path.Combine(Application.persistentDataPath, "impressions.dat");
    if (ClearCachedImpressionsAtStartup && File.Exists(stateSaveFile)) {
      Debug.Log("Deleting saved file");
      File.Delete(stateSaveFile);
    }
  }

  private void Start() {
    PlayableLocationsService = gameObject.AddComponent<PlayableLocationsService>();

    MapsService.EnableVerboseLogging(true);

    ReportBadPointExample();

    ReportImpressions();
  }

  /// <summary>
  /// Submits a player report and logs whether the request was successful.
  /// </summary>
  private void ReportBadPointExample() {
    PlayableLocationsService.ReportBadPoint("xyz+test",
        PlayableLocationsService.BadLocationReason.NotPedestrianAccessible,
        "in the middle of the lake",
        status => Debug.LogFormat("ReportBadPoint request status: IsError={0}, Error={1}",
            status.IsError, status.Error));
  }

  private void ReportImpressions() {
    const int monsterGameObjectType = 0;
    const int powerUpGameObjectType = 1;

    // Player sees a monster at location a and a power-up at location b.
    PlayableLocationsService.ReportPresentedPlace(monsterGameObjectType, "a");
    PlayableLocationsService.ReportPresentedPlace(powerUpGameObjectType, "b");

    // Next frame the player still sees the same things. Developer is free to call the same methods
    // again. They will not result in any network requests.
    PlayableLocationsService.ReportPresentedPlace(monsterGameObjectType, "a");
    PlayableLocationsService.ReportPresentedPlace(powerUpGameObjectType, "b");

    // Player picks up the power-up.
    PlayableLocationsService.ReportInteractedPlace(powerUpGameObjectType, "b");

    // The power-up location is not active any more so the player only sees the monster.
    PlayableLocationsService.ReportPresentedPlace(monsterGameObjectType, "a");

    // After some time the power-up becomes active again.
    PlayableLocationsService.ReportPresentedPlace(monsterGameObjectType, "a");
    PlayableLocationsService.ReportPresentedPlace(powerUpGameObjectType, "b");

    // Second location now has both a monster and power-up.
    PlayableLocationsService.ReportPresentedPlace(monsterGameObjectType, "b");
    PlayableLocationsService.ReportPresentedPlace(powerUpGameObjectType, "b");
  }
}
