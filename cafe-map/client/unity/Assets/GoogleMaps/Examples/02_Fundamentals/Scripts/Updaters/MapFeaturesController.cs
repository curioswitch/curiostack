using Google.Maps.Event;
using Google.Maps.Examples.Shared;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// The purpose of this class is to facilitate displaying and hiding selected map features by
  /// enabling/disabling these features while the map is loaded.
  /// Also, it provides optional placeholders for bucketing the loaded geometry based on map
  /// features categories.
  /// This allows a cleaner partition of the loaded <see cref="GameObject"/>s in the scene.
  /// Note: Re-parented objects will receive updates to the floating origin via the
  /// OnFloatingOriginMoved method. The SDK also retains ownership of these objects, and will
  /// continue to destroy them when unloaded.
  /// </summary>
  public class MapFeaturesController : MonoBehaviour {
    /// <summary>
    /// Reference to the base map loader (which knows about MapsService).
    /// The <see cref="BaseMapLoader"/> handles the basic loading of a map region and provides
    /// default styling parameters and loading errors management.
    /// </summary>
    public BaseMapLoader BaseMapLoader;

    [Tooltip(
        "This optional GameObject if provided, serves as the parent container for " +
        "extruded structures")]
    public GameObject ExtrudedStructuresContainer;

    [Tooltip(
        "This optional GameObject if provided, serves as the parent container for " +
        "modeled structures")]
    public GameObject ModeledStructuresContainer;

    [Tooltip("This optional GameObject if provided, serves as the parent container for regions")]
    public GameObject RegionsContainer;

    [Tooltip(
        "This optional GameObject if provided, serves as the parent container for " +
        "segments (think roads)")]
    public GameObject SegmentsContainer;

    [Tooltip(
        "This optional GameObject if provided, serves as the parent container for water areas")]
    public GameObject AreaWaterContainer;

    [Tooltip(
        "This optional GameObject if provided, serves as the parent container for water lines")]
    public GameObject LineWaterContainer;

    // Flags to control the visibility of map features types
    [Tooltip("When enabled, notifies the map sdk to show extruded structures.")]
    public bool ShowExtrudedStructures = true;

    [Tooltip("When enabled, notifies the map sdk to show modeled structures.")]
    public bool ShowModeledStructures = true;

    [Tooltip("When enabled, notifies the map sdk to show regions.")]
    public bool ShowRegions = true;

    [Tooltip("When enabled, notifies the map sdk to show segments.")]
    public bool ShowSegments = true;

    [Tooltip("When enabled, notifies the map sdk to show water areas.")]
    public bool ShowAreaWater = true;

    [Tooltip("When enabled, notifies the map sdk to show water lines.")]
    public bool ShowLineWater = true;

    private void Awake() {
      // Verify that we have a base map loader available

      // Get the required base map loader
      if (BaseMapLoader == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, BaseMapLoader, "Base Map Loader", "is required for this script to work."));
      }
    }

    /// <summary>
    /// When this event listener is triggered, it deletes the GameObjects in all map features
    /// buckets, if these are enabled.
    /// </summary>
    public void OnMapCleared() {
      foreach (GameObject container in GetContainers()) {
        ClearContainer(container);
      }
    }

    /// <summary>
    /// Destroys all children under the given parent GameObject.
    /// </summary>
    /// <param name="parent"></param>
    private void ClearContainer(GameObject parent) {
      if (parent == null)
        return; // Invalid parent container - ignore

      foreach (Transform child in parent.transform) {
        // Note: destroying SDK created objects should in general not be done without a closely
        // coupled call to GameObjectManager.DidDestroy(). Here, we rely on the fact that we will
        // soon follow with a call to GameObjectManager.DidDestroyAll()
        Destroy(child.gameObject);
      }
    }

    /// <summary>
    /// Clears and reloads the map.
    /// Note that the map sdk is keeping references of the GameObjects it creates each time a map
    /// is loaded.
    /// As such, we need to notify the sdk explicitly when we clear these GameObjects in the scene.
    /// </summary>
    public void OnLoadRequested() {
      ClearAndReload();
    }

    /// <summary>
    /// Register all WillCreate events for each map feature in order to provide show/hide
    /// instructions prior to the loading of the map.
    /// </summary>
    private void OnEnable() {
      // Add/remove all event listeners to control visibility and location of maps features
      if (BaseMapLoader == null)
        return;

      // Register interest for map cleared events
      BaseMapLoader.MapClearedEvent.AddListener(OnMapCleared);

      // Register interest for floating origin movement events
      BaseMapLoader.MapsService.Events.FloatingOriginEvents.Move.AddListener(OnFloatingOriginMoved);

      // Used to control which objects are created or hidden
      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(
          OnWillCreateExtrudedStructure);

      BaseMapLoader.MapsService.Events.ModeledStructureEvents.WillCreate.AddListener(
          OnWillCreateModeledStructure);
      BaseMapLoader.MapsService.Events.RegionEvents.WillCreate.AddListener(OnWillCreateRegion);
      BaseMapLoader.MapsService.Events.SegmentEvents.WillCreate.AddListener(OnWillCreateSegment);

      BaseMapLoader.MapsService.Events.AreaWaterEvents.WillCreate.AddListener(
          OnWillCreateAreaWater);

      BaseMapLoader.MapsService.Events.LineWaterEvents.WillCreate.AddListener(
          OnWillCreateLineWater);

      // Used to bucket GameObjects
      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
          OnExtrudedStructureCreated);

      BaseMapLoader.MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
          OnModeledStructureCreated);
      BaseMapLoader.MapsService.Events.RegionEvents.DidCreate.AddListener(OnRegionCreated);
      BaseMapLoader.MapsService.Events.SegmentEvents.DidCreate.AddListener(OnSegmentCreated);
      BaseMapLoader.MapsService.Events.AreaWaterEvents.DidCreate.AddListener(OnAreaWaterCreated);
      BaseMapLoader.MapsService.Events.LineWaterEvents.DidCreate.AddListener(OnLineWaterCreated);

      ClearAndReload();
    }

    /// <summary>
    /// Clean up. UnRegister all WillCreate events previously created during OnEnable.
    /// </summary>
    void OnDisable() {
      if (BaseMapLoader == null)
        return;

      // Unregister interest for map cleared events
      BaseMapLoader.MapClearedEvent.RemoveListener(OnMapCleared);

      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.WillCreate.RemoveListener(
          OnWillCreateExtrudedStructure);

      BaseMapLoader.MapsService.Events.ModeledStructureEvents.WillCreate.RemoveListener(
          OnWillCreateModeledStructure);
      BaseMapLoader.MapsService.Events.RegionEvents.WillCreate.RemoveListener(OnWillCreateRegion);
      BaseMapLoader.MapsService.Events.SegmentEvents.WillCreate.RemoveListener(OnWillCreateSegment);

      BaseMapLoader.MapsService.Events.AreaWaterEvents.WillCreate.RemoveListener(
          OnWillCreateAreaWater);

      BaseMapLoader.MapsService.Events.LineWaterEvents.WillCreate.RemoveListener(
          OnWillCreateLineWater);

      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.DidCreate.RemoveListener(
          OnExtrudedStructureCreated);

      BaseMapLoader.MapsService.Events.ModeledStructureEvents.DidCreate.RemoveListener(
          OnModeledStructureCreated);
      BaseMapLoader.MapsService.Events.RegionEvents.DidCreate.RemoveListener(OnRegionCreated);
      BaseMapLoader.MapsService.Events.SegmentEvents.DidCreate.RemoveListener(OnSegmentCreated);
      BaseMapLoader.MapsService.Events.AreaWaterEvents.DidCreate.RemoveListener(OnAreaWaterCreated);
      BaseMapLoader.MapsService.Events.LineWaterEvents.DidCreate.RemoveListener(OnLineWaterCreated);

      ClearAndReload();
    }

    /// <summary>
    /// Triggered by the Maps SDK when the floating origin has moved.
    /// As child objects of our containers have been de-parented from the MapsService, their
    /// positions need to be manually adjusted.
    /// </summary>
    void OnFloatingOriginMoved(FloatingOriginMoveArgs args) {
      foreach (GameObject container in GetContainers()) {
        foreach (Transform child in container.transform) {
          // Offset child object position by the amount by which the floating origin has moved.
          child.position += args.Offset;
        }
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for extruded structures needs to be created (or not)
    /// based on the value of the UI flag.
    /// </summary>
    void OnWillCreateExtrudedStructure(WillCreateExtrudedStructureArgs args) {
      args.Cancel = !ShowExtrudedStructures;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for modeled structures needs to be created (or not)
    /// based on the value of the UI flag.
    /// </summary>
    void OnWillCreateModeledStructure(WillCreateModeledStructureArgs args) {
      args.Cancel = !ShowModeledStructures;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for regions needs to be created (or not) based on the
    /// value of the UI flag.
    /// </summary>
    void OnWillCreateRegion(WillCreateRegionArgs args) {
      args.Cancel = !ShowRegions;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for segments needs to be created (or not) based on the
    /// value of the UI flag.
    /// </summary>
    void OnWillCreateSegment(WillCreateSegmentArgs args) {
      args.Cancel = !ShowSegments;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for water areas needs to be created (or not) based on
    /// the value of the UI flag.
    /// </summary>
    void OnWillCreateAreaWater(WillCreateAreaWaterArgs args) {
      args.Cancel = !ShowAreaWater;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Notifies the SDK if the GameObject for water lines needs to be created (or not) based on
    /// the value of the UI flag.
    /// </summary>
    void OnWillCreateLineWater(WillCreateLineWaterArgs args) {
      args.Cancel = !ShowLineWater;
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnExtrudedStructureCreated(DidCreateExtrudedStructureArgs args) {
      if (ExtrudedStructuresContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(ExtrudedStructuresContainer.transform, true);
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnModeledStructureCreated(DidCreateModeledStructureArgs args) {
      if (ModeledStructuresContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(ModeledStructuresContainer.transform, true);
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnRegionCreated(DidCreateRegionArgs args) {
      if (RegionsContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(RegionsContainer.transform, true);
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnSegmentCreated(DidCreateSegmentArgs args) {
      if (SegmentsContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(SegmentsContainer.transform, true);
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnAreaWaterCreated(DidCreateAreaWaterArgs args) {
      if (AreaWaterContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(AreaWaterContainer.transform, true);
      }
    }

    /// <summary>
    /// Triggered by the Maps SDK during the loading process.
    /// Re-parents the newly created GameObject if a container was provided for this map feature
    /// category.
    /// </summary>
    void OnLineWaterCreated(DidCreateLineWaterArgs args) {
      if (LineWaterContainer != null) {
        // Note: Care should be taken when reparenting SDK created object.
        // See note above in ClearContainer()
        args.GameObject.transform.SetParent(LineWaterContainer.transform, true);
      }
    }

    /// <summary>
    /// Destroys all augmented data and map features before reloading the map.
    /// When new events are added to the maps service, the area needs to be reloaded for these to
    /// take effect.
    /// This can also have an impact on all augmented objects in the scene, which need to be updated
    /// accordingly.
    /// In this case, we remove all roads and buildings gizmos if any.
    /// </summary>
    private void ClearAndReload() {
      // Update the map
      BaseMapLoader.ClearMap();
      BaseMapLoader.LoadMap();
    }

    /// <summary>
    /// Convenience method for retrieving an array of all map feature containers for iteration.
    /// </summary>
    /// <returns>An array of all map feature containers.</returns>
    private GameObject[] GetContainers() {
      return new GameObject[] {
        ExtrudedStructuresContainer,
        ModeledStructuresContainer,
        RegionsContainer,
        SegmentsContainer,
        AreaWaterContainer,
        LineWaterContainer
      };
    }
  }
}
