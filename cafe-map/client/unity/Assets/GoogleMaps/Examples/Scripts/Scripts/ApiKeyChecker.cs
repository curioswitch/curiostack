using System.Collections.Generic;
using Google.Maps;
using UnityEngine;

#if UNITY_EDITOR
using UnityEditor;
#endif

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
  private const string ApiKeyWebsite = "https://developers.google.com/maps/tt/games/unity#api_key";

#if UNITY_EDITOR
  // Prompt to get ApiKey from website with press of an EditorUtility.DisplayDialog button.
  private const string WebsiteSolution = "Get an API Key to try out the Maps Unity SDK with?";
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
    // MapsService[] mapsServices = FindObjectsOfType<MapsService>();
    // foreach (MapsService mapsService in mapsServices) {
    //   if (mapsService.ResolveApiKey() == "") {
    //     ShowError(mapsServices);
    //     return;
    //   }
    // }
  }

  /// <summary>
  /// Show an error saying that one or more in-scene <see cref="MapsService"/>
  /// <see cref="Component"/> were missing an <see cref="MapsService.ApiKey"/>, and offer potential
  /// solutions.
  /// </summary>
  internal static void ShowError(params MapsService[] mapsServices) {
    if (mapsServices == null || mapsServices.Length <= 1) {
      ShowErrorWebsite("API Key Missing",
          string.Concat("No API Key found.\n\nThe Maps Unity SDK needs an API Key to run.\n\n",
              WebsiteSolution));
      return;
    }

    // If multiple Maps Services given, print a more elaborate error message describing which
    // specific Maps Services are missing their Api Key. We start by finding out which MapsServices
    // are missing their ApiKeys.
    var keylessMapsServices = new List<MapsService>(mapsServices.Length);
    string foundApiKey = "";
    foreach (MapsService mapsService in mapsServices) {
      if (string.IsNullOrEmpty(mapsService.ApiKey)) {
        keylessMapsServices.Add(mapsService);
      } else {
        foundApiKey = mapsService.ApiKey;
      }
    }

    // Show error saying that all in-scene MapsServices are missing their ApiKeys, and link to
    // website for getting one.
    if (keylessMapsServices.Count == mapsServices.Length) {
      ShowErrorWebsite(
        "API Keys Missing",
        string.Format("No API Key found on any of the {0} Maps Services in the current scene.\n\n"
            + "The Maps Unity SDK needs an API Key to run.\n\n{1}",
            mapsServices.Length, WebsiteSolution));
      return;
    }

    // Show an error saying only one MapsService is missing its ApiKey, saying which GameObject
    // it is on.
    if (keylessMapsServices.Count == 1) {
      ShowErrorNotAll(
        "API Key Missing",
        string.Format("No API Key found on 1 of {0} MapsService components in this scene, "
            + "specifically Maps Service on gameObject named {1}.\n\nAll MapsService components "
            + "needs an API Key to run - please make sure {1} also has API Key set to: {2}",
            mapsServices.Length, keylessMapsServices[0].gameObject.name, foundApiKey));
      return;
    }

    // Show an error saying multiple (but not all) MapsServices are missing their ApiKeys, listing
    // the names of their GameObjects.
    ShowErrorNotAll(
      "API Keys Missing",
      string.Format("No API Key found on {0} of {1} MapsService components in this scene, "
          + "specifically Maps Services on gameObject named {2}.\n\nThe Maps Unity SDK needs an "
          + "API Key to run - please make sure all Maps Services have their API Keys set to: {3}",
          keylessMapsServices.Count, mapsServices.Length, ListMapsServiceNames(keylessMapsServices),
          foundApiKey));
  }

  /// <summary>
  /// Show an error linking to website where can get an <see cref="MapsService.ApiKey"/>.
  /// </summary>
  private static void ShowErrorWebsite(string errorTitle, string errorMessage) {
#if UNITY_EDITOR
    // Show EditorUtility.DisplayDialog with two options, 'Yes' and 'Cancel'.
    if (EditorUtility.DisplayDialog(errorTitle, errorMessage, "Yes", "Cancel")) {
      // Pressing 'Yes' (in response to question 'Get an ApiKey to try out the Maps Unity SDK
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
    // pressed, play stops (as the Maps Unity SDK cannot run without ApiKeys on all elements).
    EditorUtility.DisplayDialog(errorTitle, errorMessage, "Cancel");
    EditorApplication.isPlaying = false;
#else
    Debug.LogError(errorMessage);
#endif
  }


  /// <summary>
  /// Return a string listing all the <see cref="GameObject"/> names in a list of
  /// <see cref="MapsService"/>s.
  /// </summary>
  /// <remarks>
  /// Assumes there is more than one entry in the list of <see cref="MapsService"/>s.
  /// </remarks>
  private static string ListMapsServiceNames(List<MapsService> mapsServices) {
    string mapsServiceNames = mapsServices[0].gameObject.name;
    for (int i = 1; i < mapsServices.Count - 1; i ++) {
      mapsServiceNames = string.Format(
        "{0}, {1}",
        mapsServiceNames,
        mapsServices[i].gameObject.name);
    }
    return string.Format(
      "{0} and {1}",
      mapsServiceNames,
      mapsServices[mapsServices.Count - 1].gameObject.name);
  }
}
