using System.Collections.Generic;
using Google.Maps;
using Google.Maps.Event;
using UnityEngine;

/// <summary>
/// Example script for displaying an error icon in the centre of each area that fails to load.
/// </summary>
/// <remarks>
/// The easiest way to test this is to turn off networking while running this script, move the
/// camera and wait for some error images to appear, then turn the network back on.
/// </remarks>
[RequireComponent(typeof(DynamicMapsService), typeof(FloatingOriginUpdater))]
public sealed class LoadFailuresExample : MonoBehaviour {
  [Tooltip("The GameObject to instantiate in the center of areas that fail to load.")]
  public GameObject ErrorIndicator;

  /// <summary>
  /// Convenience reference to the <see cref="MapsService"/> in the required
  /// <see cref="DynamicMapsService"/>.
  /// </summary>
  private MapsService MapsService;

  /// <summary>List of all instantiated error images.</summary>
  private readonly LinkedList<GameObject> ErrorImages = new LinkedList<GameObject>();

  /// <summary>
  /// Use <see cref="MapsService"/> to load geometry, displaying error icon in the center of any
  /// area that failed to load.
  /// </summary>
  private void Start() {
    // Get required Dynamic Maps Service component on this GameObject.
    MapsService = GetComponent<DynamicMapsService>().MapsService;
    MapsService.Events.MapEvents.LoadError.AddListener(OnMapLoadError);
    MapsService.Events.MapEvents.Progress.AddListener(OnMapLoadProgress);
  }

  /// <summary>
  /// Shows an error image in the centre of the Bounds contained in the given
  /// <see cref="MapLoadErrorArgs"/>.
  /// </summary>
  /// <param name="args">
  /// The <see cref="MapLoadErrorArgs"/> from the <see cref="MapEvents.LoadError"/> event.
  /// </param>
  private void OnMapLoadError(MapLoadErrorArgs args) {
    Bounds bounds = args.GetBounds(MapsService.Coords);
    foreach (GameObject errorImage in ErrorImages) {
      // If there is already an error image in the area that failed to load, don't show another one.
      if (bounds.Contains(errorImage.transform.position)) {
        return;
      }
    }
    GameObject newImage = Instantiate(ErrorIndicator, bounds.center, new Quaternion());
    ErrorImages.AddLast(newImage);
    // Ensure the FloatingOriginUpdater knows about all the error images so they can be
    // repositioned if necessary.
    GetComponent<FloatingOriginUpdater>().SetAdditionalGameObjects(ErrorImages);
  }

  /// <summary>
  /// Checks the Bounds of loaded areas and removes the corresponding error image if it is in the
  /// same place.
  /// </summary>
  /// <param name="args">
  /// The <see cref="MapLoadProgressArgs"/> from the <see cref="MapEvents.Progress"/> event.
  /// </param>
  private void OnMapLoadProgress(MapLoadProgressArgs args) {
    var imagesToRemove = new List<LinkedListNode<GameObject>>();
    foreach (Bounds bounds in args.GetBounds(MapsService.Coords)) {
      LinkedListNode<GameObject> node = ErrorImages.First;
      while (node != null) {
        GameObject errorImage = node.Value;
        if (bounds.Contains(errorImage.transform.position)) {
          // If the area that successfully loaded contains an error image, mark it for removal.
          imagesToRemove.Add(node);
        }
        node = node.Next;
      }
    }

    // Actually destroy and remove the marked error images.
    foreach (LinkedListNode<GameObject> toRemove in imagesToRemove) {
      Destroy(toRemove.Value);
      ErrorImages.Remove(toRemove);
    }

    // Ensure the FloatingOriginUpdater knows about all the error images so they can be
    // repositioned if necessary.
    GetComponent<FloatingOriginUpdater>().SetAdditionalGameObjects(ErrorImages);
  }
}
