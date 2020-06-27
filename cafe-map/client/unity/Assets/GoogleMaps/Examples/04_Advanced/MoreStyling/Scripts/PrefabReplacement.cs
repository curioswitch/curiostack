using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Prefab replacement example, demonstrating how to use the Maps SDK for Unity's events to
  /// replace specific buildings.
  /// </summary>
  /// <remarks>
  /// Shows how to use a defined prefab to replace <see cref="ExtrudedStructure"/>s and
  /// <see cref="ModeledStructure"/>s of given <see cref="StructureMetadata.UsageType"/>s; or
  /// <see cref="ModeledStructure"/>s that were suppressed by the SDK because they have a vertex
  /// count that exceeds Unity's maximum support vertex count (65,000 vertices per mesh). <para>
  /// This scene starts focused on Westminster Abbey (London), as this model is currently too
  /// detailed for Unity to handle, and so is suppressed.
  /// </para>
  /// This script uses <see cref="DynamicMapsService"/> to allow navigation around the world, with
  /// the <see cref="Google.Maps.MapsService"/> keeping only the viewed part of the world loaded at
  /// all times. <para> This script also uses <see cref="ErrorHandling"/> component to display any
  /// errors encountered by the <see cref="MapsService"/> component when loading geometry.
  /// </para></remarks>
  [RequireComponent(typeof(DynamicMapsService), typeof(ErrorHandling))]
  public sealed class PrefabReplacement : MonoBehaviour {
    [Tooltip("Prefab to use to replace buildings.")]
    public GameObject Prefab;

    [Header("Options"), Tooltip("Replace bars with a prefab?")]
    public bool ReplaceBars;

    [Tooltip("Replace banks with a prefab?")]
    public bool ReplaceBanks = true;

    [Tooltip("Replace lodgings with a prefab?")]
    public bool ReplaceLodgings;

    [Tooltip("Replace cafes with a prefab?")]
    public bool ReplaceCafes;

    [Tooltip("Replace restaurants with a prefab?")]
    public bool ReplaceRestaurants;

    [Tooltip("Replace event venues with a prefab?")]
    public bool ReplaceEventVenues;

    [Tooltip("Replace tourist destinations with a prefab?")]
    public bool ReplaceTouristDestinations;

    [Tooltip("Replace shops with a prefab?")]
    public bool ReplaceShops;

    [Tooltip("Replace schools with a prefab?")]
    public bool ReplaceSchools;

    [Tooltip("Replace buildings without a specified usage type with a prefab?")]
    public bool ReplaceUnspecifieds;

    [Tooltip(
        "Replace very high poly buildings with this prefab? Note that Unity is unable " +
        "to display meshes with over 65,000 vertices, so buildings with more vertices are " +
        "replaced with null meshes. If this toggle is enabled, these buildings will be replaced " +
        "with a prefab instead.")]
    public bool ReplaceSuppressed = true;

    /// <summary>
    /// Use <see cref="DynamicMapsService"/> to load geometry, replacing any buildings as needed.
    /// </summary>
    private void Awake() {
      // Make sure a prefab has been specified.
      if (Prefab == null) {
        Debug.LogError(ExampleErrors.MissingParameter(
            this, Prefab, "Prefab", "to replace specific buildings with"));

        return;
      }

      // Get required DynamicMapsService component on this GameObject.
      DynamicMapsService dynamicMapsService = GetComponent<DynamicMapsService>();

      // See if any options have been set indicating which types of buildings to replace, signing up
      // to WillCreate events if so.
      if (ReplaceBars || ReplaceBanks || ReplaceLodgings || ReplaceCafes || ReplaceRestaurants ||
          ReplaceEventVenues || ReplaceTouristDestinations || ReplaceShops || ReplaceSchools ||
          ReplaceUnspecifieds) {
        // Create styles for ExtrudedStructure and ModeledStructure type buildings that are to be
        // replaced with a prefab.
        ExtrudedStructureStyle extrudedPrefabStyle =
            new ExtrudedStructureStyle.Builder { Prefab = Prefab }.Build();

        ModeledStructureStyle modeledPrefabStyle =
            new ModeledStructureStyle.Builder { Prefab = Prefab }.Build();

        // Sign up to events called just before any new building is loaded, so we can check each
        // building's usage type and replace it with prefab if needed. Note that:
        // - DynamicMapsService.MapsService is auto-found on first access (so will not be null).
        // - These events must be set now during Awake, so that when DynamicMapsService starts
        //   loading the map during Start, these event will be triggered for all ExtrudedStructures
        //   and ModeledStructures.
        dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(
            args => {
              if (ShouldReplaceBuilding(args.MapFeature.Metadata.Usage)) {
                args.Style = extrudedPrefabStyle;
              }
            });

        dynamicMapsService.MapsService.Events.ModeledStructureEvents.WillCreate.AddListener(
            args => {
              if (ShouldReplaceBuilding(args.MapFeature.Metadata.Usage)) {
                args.Style = modeledPrefabStyle;
              }
            });
      }

      // See if we should be replacing any suppressed buildings with prefab, signing up to DidCreate
      // event if so.
      if (ReplaceSuppressed) {
        // Sign up to event called just after any new building is loaded, so we can check if the
        // building's mesh has been suppressed and should be replaced with a prefab.
        dynamicMapsService.MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
            args => TryReplaceSuppressedBuilding(args.GameObject));

        dynamicMapsService.MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
            args => TryReplaceSuppressedBuilding(args.GameObject));
      }
    }

    /// <summary>
    /// Check if a building of a given <see cref="StructureMetadata.UsageType"/> should be replaced
    /// with a prefab.
    /// </summary>
    /// <param name="usage"><see cref="StructureMetadata.UsageType"/> of this building.</param>
    private bool ShouldReplaceBuilding(StructureMetadata.UsageType usage) {
      switch (usage) {
        case StructureMetadata.UsageType.Bar:
          return ReplaceBars;

        case StructureMetadata.UsageType.Bank:
          return ReplaceBanks;

        case StructureMetadata.UsageType.Lodging:
          return ReplaceLodgings;

        case StructureMetadata.UsageType.Cafe:
          return ReplaceCafes;

        case StructureMetadata.UsageType.Restaurant:
          return ReplaceRestaurants;

        case StructureMetadata.UsageType.EventVenue:
          return ReplaceEventVenues;

        case StructureMetadata.UsageType.TouristDestination:
          return ReplaceTouristDestinations;

        case StructureMetadata.UsageType.Shopping:
          return ReplaceShops;

        case StructureMetadata.UsageType.School:
          return ReplaceSchools;

        case StructureMetadata.UsageType.Unspecified:
          return ReplaceUnspecifieds;

        default:
          Debug.LogErrorFormat(
              "{0}.{1} encountered an unhandled {2} of '{3}' for building.\n{0} is " +
                  "not yet setup to handle {3} type buildings.",
              name,
              GetType(),
              typeof(StructureMetadata.UsageType),
              usage);

          return false;
      }
    }

    /// <summary>Replace a given building with a prefab if its mesh has been suppressed.</summary>
    /// <param name="building"><see cref="GameObject"/> of this building.</param>
    private void TryReplaceSuppressedBuilding(GameObject building) {
      // Check if this building's geometry has been suppressed by the MapsService. The MapsService
      // suppresses any mesh with over 65,000 vertices, which exceeds Unity's maximum supported
      // vertex count. Suppressed geometry can be detected by checking if the MeshFilter.sharedMesh
      // on a created is null (that is, if the mesh was deliberately not loaded for this building).
      if (building.GetComponent<MeshFilter>().sharedMesh != null) {
        return;
      }

      // To replace building, we start by hiding the original building we're going to replace. Note
      // that we don't do this by setting it's GameObject to inactive, as this would hide any
      // children, including the prefab we're about to create and make a child of this building.
      // Instead we hide this building by disabling all its MeshRenderers.
      foreach (MeshRenderer meshRenderer in building.GetComponentsInChildren<MeshRenderer>()) {
        meshRenderer.enabled = false;
      }

      // Created an instance of the prefab we'll use to replace this building's geometry.
      Transform prefabTransform = Instantiate(Prefab).transform;

      // Make this just created prefab instance a child of this building's original GameObject. We
      // do this (instead of just destroying the building's original GameObject), so that if the
      // MapsService wants to remove this building (if it has moved offscreen and needs to be
      // unloaded), the MapsService will still be able to find and remove this building's
      // GameObject, which should also remove the child prefab we're now placing under it.
      prefabTransform.SetParent(building.transform);

      // Move the prefab to the center of the building. Note that only the prefab's x and z
      // coordinates are moved, but its y coordinate (it's height) is maintained. We do this to
      // allow for prefabs that are meant to be hovering off the ground.
      prefabTransform.localPosition = new Vector3(0f, prefabTransform.localPosition.y, 0f);
    }
  }
}
