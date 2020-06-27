using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using Google.Maps.Event;
using Google.Maps.Examples.Shared;

namespace Google.Maps.Examples {
  /// <summary>
  /// Responsible for:
  /// <list type="bullet">
  /// <item><description>
  /// Providing a list of valid place ids to the drop down.
  /// </description></item>
  /// <item><description>Handling a selected place id (camera move + pointer).</description></item>
  /// </summary>
  public class SearchByPlaceIdUpdater : MonoBehaviour {
    [Tooltip("The Base Map Loader handles the Maps Service initialization and basic event flow.")]
    public BaseMapLoader BaseMapLoader;

    /// <summary>
    /// Keeps a list of associations between place ids and map <see cref="GameObject"/>s.
    /// </summary>
    public Dictionary<string, GameObject> PlaceIdToGameObjectDict =
        new Dictionary<string, GameObject>();

    /// <summary>
    /// Use <see cref="MapsService"/> to load geometry, labelling all created roads with their
    /// names.
    /// </summary>
    void Awake() {
      // Get the required base map loader.
      if (BaseMapLoader == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, BaseMapLoader, "Base Map Loader", "is required for this script to work."));
      }

      // Register listeners prior to loading the map.
      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          OnExtrudedStructureCreated);
      BaseMapLoader.MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
          OnModeledStructureCreated);
      BaseMapLoader.MapsService.Events.MapEvents.Loaded.AddListener(OnMapLoaded);
    }

    /// <summary>
    /// OnMapLoaded is called each time the map region is updated - either loaded or unloaded.
    /// When this happens, some of the <see cref="GameObject"/>s related to map features might have
    /// been removed from the scene.
    /// Therefore we need to perform some clean up on all collections that might have references to
    /// these objects.
    /// </summary>
    public void OnMapLoaded(MapLoadedArgs args) {
      // Adjust the content of our dictionary with all GameObjects actually in the scene.
      List<string> toRemove = PlaceIdToGameObjectDict.Where(pair => pair.Value == null)
                                  .Select(pair => pair.Key)
                                  .ToList();

      if (toRemove.Count > 0) {
        foreach (string placeId in toRemove) {
          PlaceIdToGameObjectDict.Remove(placeId);
        }
      }
    }

    /// <summary>
    /// When an extruded structure has been loaded, monitor its destroy phase by adding a custom
    /// <see cref="ListenToDestroy"/> component.
    /// This will be used to keep the dictionary of loaded place ids up-to-date.
    /// </summary>
    void OnExtrudedStructureCreated(DidCreateExtrudedStructureArgs args) {
      if (!PlaceIdToGameObjectDict.ContainsKey(args.MapFeature.Metadata.PlaceId)) {
        PlaceIdToGameObjectDict.Add(args.MapFeature.Metadata.PlaceId, args.GameObject);

        // We are interested in the lifecycle of this GameObject
        // Let's attach a component listening to its OnDestroy event
        // When destroyed the object will notify our local cache for cleanup
        if (args.GameObject.GetComponent<ListenToDestroy>() == null) {
          ListenToDestroy ltd = args.GameObject.AddComponent<ListenToDestroy>();

          if (ltd != null) {
            ltd.PlaceId = args.MapFeature.Metadata.PlaceId;

            if (ltd.MapFeatureDestroyed == null)
              ltd.MapFeatureDestroyed = new MapFeatureDestroyedEvent();
            else
              ltd.MapFeatureDestroyed.AddListener(OnMapFeatureDestroyed);
          }
        }
      } else {
        PlaceIdToGameObjectDict[args.MapFeature.Metadata.PlaceId] = args.GameObject;
      }
    }

    /// <summary>
    /// Triggered when a map feature is about to be destroyed. Clears the local cache.
    /// </summary>
    /// <param name="placeId"></param>
    void OnMapFeatureDestroyed(string placeId) {
      if (string.IsNullOrEmpty(placeId)) {
        return;
      }

      Debug.Log(string.Format("Clearing {0}", placeId));

      if (PlaceIdToGameObjectDict.ContainsKey(placeId)) {
        PlaceIdToGameObjectDict.Remove(placeId);
      }
    }

    /// <summary>
    /// When a modeled structure has been loaded, monitor its destroy phase by adding a custom
    /// <see cref="ListenToDestroy"/> component.
    /// </summary>
    /// <remarks>
    /// This will be used to keep the dictionary of loaded place ids up-to-date.
    /// </remarks>
    void OnModeledStructureCreated(DidCreateModeledStructureArgs args) {
      if (!PlaceIdToGameObjectDict.ContainsKey(args.MapFeature.Metadata.PlaceId)) {
        PlaceIdToGameObjectDict.Add(args.MapFeature.Metadata.PlaceId, args.GameObject);

        // We are interested in the lifecycle of this GameObject
        // Let's attach a component listening to its OnDestroy event
        // When destroyed the object will notify our local cache for cleanup
        if (args.GameObject.GetComponent<ListenToDestroy>() == null) {
          ListenToDestroy ltd = args.GameObject.AddComponent<ListenToDestroy>();

          if (ltd != null) {
            ltd.PlaceId = args.MapFeature.Metadata.PlaceId;

            if (ltd.MapFeatureDestroyed == null)
              ltd.MapFeatureDestroyed = new MapFeatureDestroyedEvent();
            else
              ltd.MapFeatureDestroyed.AddListener(OnMapFeatureDestroyed);
          }
        }
      } else {
        PlaceIdToGameObjectDict[args.MapFeature.Metadata.PlaceId] = args.GameObject;
      }
    }
  }
}
