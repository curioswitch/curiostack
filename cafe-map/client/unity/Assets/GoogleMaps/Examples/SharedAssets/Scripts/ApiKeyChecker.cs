using UnityEngine;
#if UNITY_EDITOR
using UnityEditor;

#endif

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Script for checking if a <see cref="MapsService.ApiKey"/> has been configured, and informing
  /// the user if not.
  /// </summary>
  /// <remarks>
  /// This script is only meant to run in Editor. For the moment it works as a
  /// <see cref="GameObject"/> <see cref="Component"/>, so it cannot be placed in an Editor folder,
  /// and instead uses precompiler flags to make it run in the Unity Editor only.
  /// </remarks>
  public sealed class ApiKeyChecker : MonoBehaviour {
    /// <summary>
    /// Website with instructions for getting an <see cref="MapsService.ApiKey"/>.
    /// </summary>
    private const string ApiKeyWebsite =
        "https://developers.google.com/maps/documentation/gaming/quick_start" +
        "#step_5_set_the_api_key_value";

#if UNITY_EDITOR

    // Prompt to get ApiKey from website with press of an EditorUtility.DisplayDialog button.
    private const string WebsiteSolution = "Get an API Key to try out the Maps SDK for Unity with?";
#else
    // Prompt to tell user (developer) to go to website themselves.
    private const string WebsiteSolution = "Please get an API Key by going to: " + ApiKeyWebsite;
#endif

    /// <summary>
    /// On starting a scene, see if there is a <see cref="MapsService"/> <see cref="Component"/>
    /// in the scene missing its <see cref="MapsService.ApiKey"/>, printing a detailed error message
    /// if so.
    /// </summary>
    private void Awake() {
      MapsService[] mapsServices = FindObjectsOfType<MapsService>();
      if (mapsServices.Length != 0) {
        ShowError(mapsServices);
      }
    }

    /// <summary>
    /// Show an error saying that one or more in-scene <see cref="MapsService"/>
    /// <see cref="Component"/> were missing an <see cref="MapsService.ApiKey"/>, and offer
    /// potential solutions.
    /// </summary>
    internal static void ShowError(params MapsService[] mapsServices) {
      string mapsServicesWithoutApiKey = "";
      string mapsServicesWithApiKey = "";
      foreach (MapsService mapsService in mapsServices) {
        string apiKey = mapsService.ResolveApiKey();
        string messageLine = "  " + GameObjectPath(mapsService.gameObject) + "\n";
        if (string.IsNullOrEmpty(apiKey)) {
          mapsServicesWithoutApiKey += messageLine;
        } else {
          mapsServicesWithApiKey += messageLine;
        }
      }

      if (mapsServices.Length != 0 && mapsServicesWithoutApiKey.Length == 0) {
        // If MapsServices were provided and they all have API keys, there's no error to show.
        return;
      }

      string message;
      if (mapsServicesWithoutApiKey.Length == 0) {
        message = "No API Key found.\n";
      } else {
        message = "No API Key found on the MapsService components of these objects:\n\n" +
                  mapsServicesWithoutApiKey;
      }

      if (mapsServicesWithApiKey.Length == 0) {
        message += "\nThe Maps SDK for Unity needs an API Key to run.\n\n" + WebsiteSolution;
        ShowErrorWebsite("API Key Missing", message);
      } else {
        message += "\nAPI keys were found on MapsService components of the following objects:\n\n" +
                   mapsServicesWithApiKey +
                   "\nPlease make sure all Maps Services have their API Keys set.";
        ShowErrorNotAll("API Key Missing", message);
      }
    }

    /// <summary>
    /// Returns the path of a GameObject; i.e. the names of its parent objects up to the root,
    /// separated by slashes.
    /// </summary>
    private static string GameObjectPath(GameObject gameObject) {
      string name = gameObject.name;
      if (gameObject.transform.parent != null) {
        name = GameObjectPath(gameObject.transform.parent.gameObject) + "/" + name;
      }

      return name;
    }

    /// <summary>
    /// Show an error linking to website where can get an <see cref="MapsService.ApiKey"/>.
    /// </summary>
    private static void ShowErrorWebsite(string errorTitle, string errorMessage) {
#if UNITY_EDITOR

      // Show EditorUtility.DisplayDialog with two options, 'Yes' and 'Cancel'.
      if (EditorUtility.DisplayDialog(errorTitle, errorMessage, "Yes", "Cancel")) {
        // Pressing 'Yes' (in response to question 'Get an ApiKey to try out the Maps SDK for Unity
        // with?') stops play and opens website where can get an ApiKey.
        EditorApplication.isPlaying = false;
        Application.OpenURL(ApiKeyWebsite);
      } else {
        // Pressing 'Cancel' stops play immediately (as cannot run the scene without a ApiKey.
        EditorApplication.isPlaying = false;
      }
#else
      Debug.LogError(errorMessage);
#endif
    }

    /// <summary>
    /// Show an error saying that an <see cref="MapsService.ApiKey"/> was found some, but not all
    /// <see cref="MapsService"/>s in this scene.
    /// </summary>
    private static void ShowErrorNotAll(string errorTitle, string errorMessage) {
#if UNITY_EDITOR

      // Show EditorUtility.DisplayDialog and end play session, so when 'Cancel' (only button) is
      // pressed, play stops (as the Maps SDK for Unity cannot run without ApiKeys on all elements).
      EditorUtility.DisplayDialog(errorTitle, errorMessage, "Cancel");
      EditorApplication.isPlaying = false;
#else
      Debug.LogError(errorMessage);
#endif
    }
  }
}
