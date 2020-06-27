using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// An extension of the generic <see cref="Labeller"/> that adds support for updating label
  /// positions when the map's floating origin changes and removing labels from unloaded regions.
  /// </summary>
  public class MapLabeller : Labeller {
    [Tooltip(
        "The BaseMapLoader containing the FloatingOriginUpdater that tells us when the map" +
        "has moved.")]
    public BaseMapLoader BaseMapLoader;

    /// <summary>
    /// Check that we have a <see cref="BaseMapLoader"/> so that we can register appropriate
    /// listeners in OnEnable.
    /// </summary>
    protected override void Awake() {
      if (BaseMapLoader == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, BaseMapLoader, "Base Map Loader", "is required for this script to work."));
      }

      base.Awake();
    }

    /// <summary>
    /// Register listeners for floating origin update and unload events so we can move or remove
    /// labels as appropriate.
    /// </summary>
    void OnEnable() {
      FloatingOriginUpdater updater =
          BaseMapLoader.gameObject.GetComponent<FloatingOriginUpdater>();
      if (updater != null) {
        updater.OnFloatingOriginUpdate.AddListener(OnFloatingOriginUpdated);
      }

      DynamicMapsUpdater dynamicUpdater =
          BaseMapLoader.gameObject.GetComponent<DynamicMapsUpdater>();
      if (dynamicUpdater != null) {
        dynamicUpdater.UnloadedEvent.AddListener(OnRegionUnloaded);
      }

      ClearAndReload();
    }

    /// <summary>
    /// Remove listeners from floating origin update and unload events.
    /// </summary>
    void OnDisable() {
      FloatingOriginUpdater floatingUpdater =
          BaseMapLoader.gameObject.GetComponent<FloatingOriginUpdater>();
      if (floatingUpdater != null) {
        floatingUpdater.OnFloatingOriginUpdate.RemoveListener(OnFloatingOriginUpdated);
      }

      DynamicMapsUpdater dynamicUpdater =
          BaseMapLoader.gameObject.GetComponent<DynamicMapsUpdater>();
      if (dynamicUpdater != null) {
        dynamicUpdater.UnloadedEvent.RemoveListener(OnRegionUnloaded);
      }

      ClearAndReload();
    }

    /// <summary>
    /// When the floating origin has changed, reposition all object labels accordingly.
    /// </summary>
    /// <param name="offset">The amount to move each label by.</param>
    void OnFloatingOriginUpdated(Vector3 offset) {
      MoveNames(offset);
    }

    /// <summary>
    /// When a region is unloaded, delete all impacted object labels.
    /// </summary>
    /// <param name="center">The center of the region from which to delete labels from.</param>
    /// <param name="radius">The radius of the region from which to delete labels from.</param>
    void OnRegionUnloaded(Vector3 center, float radius) {
      // Remove all names outside the circle area
      ClearNamesOutsideRegion(center, radius);
    }

    /// <summary>
    /// Destroys all current labels, and reloads the map.
    /// </summary>
    private void ClearAndReload() {
      // Clear objects
      ClearNames();

      // Update the map
      BaseMapLoader.ClearMap();
      BaseMapLoader.LoadMap();
    }

    /// <summary>
    /// Moves all object name <see cref="GameObject"/>s by the given offset.
    /// </summary>
    /// <param name="offset">The offset to move the object names by.</param>
    private void MoveNames(Vector3 offset) {
      foreach (Transform child in Canvas.transform) {
        child.position += offset;
      }
    }

    /// <summary>
    /// Finds all objects that are outside the circle identified by the given center and radius
    /// and deletes them from the cache and the scene.
    /// </summary>
    /// <param name="center">The center of the circle to delete labels outside of.</param>
    /// <param name="radius">The radius of the circle to delete labels outside of.</param>
    private void ClearNamesOutsideRegion(Vector3 center, float radius) {
      // Get the list of candidates for deletion
      List<string> keysToUnload = new List<string>();

      foreach (string k in LabelsByKey.Keys) {
        float d = Vector3.Distance(LabelsByKey[k].transform.position, center);

        if (d > radius) {
          keysToUnload.Add(k);
        }
      }

      // Wipe out: first from the scene, then from the internal cache.
      foreach (string k in keysToUnload) {
        Destroy(LabelsByKey[k].gameObject);
        LabelsByKey.Remove(k);
      }
    }
  }
}
