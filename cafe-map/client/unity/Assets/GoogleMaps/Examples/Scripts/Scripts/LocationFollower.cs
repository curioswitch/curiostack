using System.Collections;
using UnityEngine;
using Google.Maps;
using Google.Maps.Coord;

/// <summary>
/// Example showing how to have the player's real-world GPS location reflected by on-screen
/// movements.
/// </summary>
/// <remarks>
/// Uses <see cref="ErrorHandling"/> component to display any errors encountered by the
/// <see cref="MapsService"/> component when loading geometry.
/// </remarks>
[RequireComponent(typeof(MapsService), typeof(ErrorHandling))]
public class LocationFollower : MonoBehaviour {

  /// <summary>Start following player's real-world location.</summary>
  private void Start() {
    StartCoroutine(Follow());
  }

  /// <summary>
  /// Follow player's real-world location, as derived from the device's GPS signal.
  /// </summary>
  private IEnumerator Follow() {

    // If location is allowed by the user, start the location service and compass, otherwise abort
    // the coroutine.
    if (Input.location.isEnabledByUser) {
      Input.location.Start();
      Input.compass.enabled = true;
    } else {
      Debug.LogError("Location Services not enabled by the user.");
      yield break;
    }

    // Wait for the location service to start.
    while (true) {
      if (Input.location.status == LocationServiceStatus.Initializing) {
        // Starting, just wait.
        yield return new WaitForSeconds(1f);
      } else if (Input.location.status == LocationServiceStatus.Failed) {
        // Failed, abort the coroutine.
        Debug.LogError("Location Services failed to start.");
        yield break;
      } else if (Input.location.status == LocationServiceStatus.Running) {
        // Started, continue the coroutine.
        break;
      }
    }

    // Get the MapsService component and load it at the device location.
    LatLng previousLocation = new LatLng(
        Input.location.lastData.latitude, Input.location.lastData.longitude);
    MapsService mapsService = GetComponent<MapsService>();
    mapsService.InitFloatingOrigin(previousLocation);
    mapsService.LoadMap(ExampleDefaults.DefaultBounds, ExampleDefaults.DefaultGameObjectOptions);

    // Every second, move the map location to the device location.
    while (true) {
      yield return new WaitForSeconds(1f);

      // Only move the map location if the device has moved more than 2 meters.
      LatLng currentLocation = new LatLng(
          Input.location.lastData.latitude, Input.location.lastData.longitude);
      float distance = Vector3.Distance(
          Vector3.zero, mapsService.Coords.FromLatLngToVector3(currentLocation));
      if (distance > 2) {
        mapsService.MoveFloatingOrigin(currentLocation);
        previousLocation = currentLocation;
      }

    }

  }

}
