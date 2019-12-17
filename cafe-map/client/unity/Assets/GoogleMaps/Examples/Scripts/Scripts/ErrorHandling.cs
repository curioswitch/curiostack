using Google.Maps;
using Google.Maps.Event;
using UnityEngine;

/// <summary>
/// Error handling Component, which handles and debugs any errors encountered by the Maps Unity SDK.
/// </summary>
/// <remarks>
/// This Component handles the most common errors (unsupported client version, or network errors),
/// otherwise showing the error message sent from the Maps Unity SDK.
/// </remarks>
[RequireComponent(typeof(MapsService))]
public sealed class ErrorHandling : MonoBehaviour {
  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry, setting the widths of all roads by their type.
  /// </summary>
  private void Awake() {
    // Get the required Maps Service on this GameObject.
    MapsService mapsService = GetComponent<MapsService>();

    // Sign up to event called whenever an error occurs. Note that this event must be set now during
    // Awake, so that when Dynamic Maps Service starts loading the map during Start, this event will
    // be triggered on any error.
    mapsService.Events.MapEvents.LoadError.AddListener(args => {
      // Check for the most common errors, showing specific error message in these cases.
      switch (args.DetailedErrorCode) {
        case MapLoadErrorArgs.DetailedErrorEnum.NetworkError:
          // Debug.LogError("This demo version only works with offline data");
          // DONT LOG NETWORK ERRORS. Musk lite shouldn't have access to the internet.
          // Handle errors caused by a lack of internet connectivity (or other network problems).
          // if (Application.internetReachability == NetworkReachability.NotReachable) {
          //   Debug.LogError("The Maps Unity SDK must have internet access in order to run.");
          // } else {
          //   Debug.LogErrorFormat("The Maps Unity SDK was not able to get a HTTP response after {0} "
          //       + "attempts.\nThis suggests an issue with the network, or with the online Vector "
          //       + "Tile API, or that the request exceeded its deadline (consider using "
          //       + "MapLoadErrorArgs.TimeoutSeconds).\n{1}",
          //     args.Attempts, string.IsNullOrEmpty(args.Message)
          //     ? string.Concat("Specific error message received: ", args.Message)
          //     : "No error message received.");
          // }
          return;

        // Handle errors caused by the specific version of the Maps Unity SDK being used.
        case MapLoadErrorArgs.DetailedErrorEnum.UnsupportedClientVersion:
          string url = "https://cloud.google.com/maps-platform/gaming/";
          Debug.LogError(
              "This demo has expired. "  +
              "Please visit " + url + " for more information.");
          // Debug.LogError("The specific version of the Maps Unity SDK being used is no longer "
          //     + "supported (possibly in combination with the specific API key used).");
          return;
      }

      // For all other types of errors, just show the given error message, as this should describe
      // the specific nature of the problem.
      Debug.LogError(args.DetailedErrorCode + ": " +  args.Message);

      // Note that the Maps Unity SDK will automatically retry failed attempts, unless args.Retry
      // is specifically set to false during this callback.
    });
  }
}
