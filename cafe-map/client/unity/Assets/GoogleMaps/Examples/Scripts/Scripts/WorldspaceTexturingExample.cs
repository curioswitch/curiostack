using System.Collections.Generic;
using UnityEngine;
using Google.Maps;
using Google.Maps.Feature.Style;

/// <summary>
/// Component to update world space textures whenever <see cref="Google.Maps.MapsService"/>'s
/// Floating Origin is updated, the end result being that world space textures are kept aligned to
/// world geometry.
/// <para>
/// Uses <see cref="FloatingOriginUpdater"/> to periodically recenter the world, moving the world
/// until the player is back at the origin (0f, 0f, 0f).
/// </para></summary>
/// <remarks>
/// Uses <see cref="DynamicMapsService"/> to allow navigation around the world, with the
/// <see cref="Google.Maps.MapsService"/> keeping only the viewed part of the world loaded at all
/// times.
/// </remarks>
[RequireComponent(typeof(DynamicMapsService), typeof(FloatingOriginUpdater))]
public sealed class WorldspaceTexturingExample : MonoBehaviour {
  /// <summary>Width in meters of borders around roads and around buildings.</summary>
  /// <remarks>This value must be greater than 0f.</remarks>
  private const float BorderWidth = 2f;

  /// <summary>
  /// Name of the vector property within all given world space textured <see cref="Material"/>s,
  /// used for offsetting the world space texture coordinates.
  /// </summary>
  /// <remarks>
  /// For more info on how this is achieved, examine the shaders in the folder:
  /// Assets/GoogleMaps/Examples/Materials/WorldspaceTextured/
  /// </remarks>
  private const string MaterialOffsetProperty = "_Offset";

  /// <summary>
  /// Id of the vector property within all given world space textured <see cref="Material"/>s, used
  /// for offsetting the world space texture coordinates.
  /// </summary>
  /// <remarks>
  /// This value is taken from <see cref="MaterialOffsetProperty"/>, converted from a
  /// <see cref="string"/> to an <see cref="int"/> for easier comparison.
  /// </remarks>
  private static int MaterialOffsetPropertyID = Shader.PropertyToID(MaterialOffsetProperty);

  [Tooltip("Material to apply to buildings. If this is a world space textured Material, its Shader "
      + "must have an _Offset Vector, as this will be used for updating the origin of the world.")]
  public Material Buildings;

  [Tooltip("Material to apply to road segments. If this is a world space textured Material, its "
      + "Shader must have an _Offset Vector, as this will be used for updating the origin of the "
      + "world.")]
  public Material Roads;

  [Tooltip("Material to apply to the borders of road segments and buildings. If this is a "
      + "worldspace textured Material, its Shader must have an _Offset Vector, as this will be "
      + "used for updating the origin of the world.")]
  public Material Borders;

  [Tooltip("Material to apply to ground regions. If this is a world space textured Material, its "
      + "Shader must have an _Offset Vector, as this will be used for updating the origin of the "
      + "world.")]
  public Material Ground;

  [Tooltip("Material to apply to water. If this is a world space textured Material, its Shader "
      + "must have an _Offset Vector, as this will be used for updating the origin of the world.")]
  public Material Water;

  /// <summary>Reference to all shared <see cref="Material"/>s controlled by this class.</summary>
  private static readonly HashSet<Material> WorldspaceMaterials = new HashSet<Material>();

  /// <summary>The amount the Floating Origin has been moved.</summary>
  /// <remarks>
  /// This offset is applied to the world space coordinates of all world space textured
  /// <see cref="Material"/>s, in order to keep their textures aligned to in-scene geometry.
  /// </remarks>
  private Vector3 Offset;

  /// <summary>
  /// Connect to <see cref="FloatingOriginUpdater.OnFloatingOriginUpdate"/>, so that whenever the
  /// world's Floating Origin is moved, all controlled world space textured
  /// <see cref="WorldspaceMaterials"/> are realigned to the world's new Floating Origin.
  /// </summary>
  private void Start() {
    // Verify all required parameters are defined, skipping further setup if not.
    if (!VerifyParameters()) {
      enabled = false;
      return;
    }

    // Get the required Floating Origin component on this GameObject.
    FloatingOriginUpdater floatingOriginUpdater = GetComponent<FloatingOriginUpdater>();

    // Make sure that whenever the Floating Origin is updated, Materials are updated in sync.
    floatingOriginUpdater.OnFloatingOriginUpdate.AddListener(UpdateWorldspaceMaterialOffsets);

    // Store all Materials that are to be updated. This function only stores Materials that have an
    // _Offset Vector, which will be used to offset the Material's world space coordinates to align
    // to the world's moved Floating Origin.
    TryAddMaterial(Buildings);
    TryAddMaterial(Roads);
    TryAddMaterial(Ground);
    TryAddMaterial(Water);

    // Because the Floating Origin has not yet been moved, there is no need to apply an _Offset to
    // these Materials yet. Instead we make sure these Material's _Offsets start at (0, 0, 0).
    UpdateWorldspaceMaterialOffsets(Vector3.zero);

    // Create styles to assign world space textured Materials to geometry as it is created.
    ExtrudedStructureStyle extrudedStructureStyle = new ExtrudedStructureStyle.Builder {
      WallMaterial = Buildings,
      RoofMaterial = Buildings
    }.Build();
    ModeledStructureStyle modeledStructureStyle = new ModeledStructureStyle.Builder {
      Material = Buildings
    }.Build();
    SegmentStyle roadsStyle = new SegmentStyle.Builder {
      Material = Roads,
      BorderMaterial = Borders,
      Width = 7.0f,
      BorderWidth = BorderWidth
    }.Build();
    RegionStyle groundStyle = new RegionStyle.Builder {
      FillMaterial = Ground
    }.Build();
    AreaWaterStyle areaWaterStyle = new AreaWaterStyle.Builder {
      FillMaterial = Water
    }.Build();
    LineWaterStyle lineWaterStyle = new LineWaterStyle.Builder {
      Material = Water,
      Width = 7.0f
    }.Build();

    GameObjectOptions renderingStyles = ExampleDefaults.DefaultGameObjectOptions;
    renderingStyles.ExtrudedStructureStyle = extrudedStructureStyle;
    renderingStyles.ModeledStructureStyle = modeledStructureStyle;
    renderingStyles.SegmentStyle = roadsStyle;
    renderingStyles.RegionStyle = groundStyle;
    renderingStyles.AreaWaterStyle = areaWaterStyle;
    renderingStyles.LineWaterStyle = lineWaterStyle;

    DynamicMapsService dynamicMapsService = floatingOriginUpdater.DynamicMapsService;
    dynamicMapsService.RenderingStyles = renderingStyles;

    // Make sure that if any new Materials are cloned by the Maps Service (which can occur to
    // resolve z-fighting issues), that these new cloned materials are added to the list of managed
    // Materials.
    MapsService mapsService = dynamicMapsService.MapsService;
    mapsService.Events.RegionEvents.DidCreate.AddListener(
        args => TryAddMaterialFrom(args.GameObject));
    mapsService.Events.AreaWaterEvents.DidCreate.AddListener(
        args => TryAddMaterialFrom(args.GameObject));
    mapsService.Events.LineWaterEvents.DidCreate.AddListener(
        args => TryAddMaterialFrom(args.GameObject));
    mapsService.Events.SegmentEvents.DidCreate.AddListener(
        args => TryAddMaterialFrom(args.GameObject));
    mapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
        args => TryAddMaterialFrom(args.GameObject));

    // For extruded buildings, also create borders around the edges of these buildings, to match
    // borders around roads.
    mapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(args => {
      TryAddMaterialFrom(args.GameObject);
      Extruder.AddBuildingBorder(args.GameObject, args.MapFeature.Shape, Borders, BorderWidth);
    });
  }

  /// <summary>
  /// Add a given <see cref="Material"/> to the managed <see cref="WorldspaceMaterials"/> if
  /// appropriate.
  /// </summary>
  /// <remarks>
  /// The given <see cref="Material"/> will only be added if it has a
  /// <see cref="MaterialOffsetProperty"/>, and also if <see cref="WorldspaceMaterials"/> does not
  /// already contain a copy of this specific <see cref="Material"/>.
  /// </remarks>
  /// <param name="material"><see cref="Material"/> to try to add.</param>
  private void TryAddMaterial(Material material) {
    // If this Material does not have the required _Offset Vector, skip adding it, as this Vector is
    // used to update the Floating Origin on world space textured Materials. If the Material doesn't
    // have this Vector, it is either not a world space textured Material, or it is not set up so
    // that this script can interact with it.
    if (!material.HasProperty(MaterialOffsetProperty)) {
      return;
    }

    // Add new Material to the list of managed Materials. Note that because these Materials are
    // stored as a HashSet, adding will be skipped if this specific Material has already been added.
    WorldspaceMaterials.Add(material);
  }

  /// <summary>
  /// Add any <see cref="Material"/>s from the supplied <see cref="GameObject"/> and its children if
  /// appropriate.
  /// </summary>
  /// <remarks>
  /// Any found <see cref="Material"/>s will only be added if they have a
  /// <see cref="MaterialOffsetProperty"/>, and also if <see cref="WorldspaceMaterials"/> does not
  /// already contain a copy of that specific <see cref="Material"/>.
  /// </remarks>
  /// <param name="geoGameObject">
  /// <see cref="GameObject"/> to try to find one or more <see cref="MeshRenderer.sharedMaterial"/>s
  /// under.
  /// </param>
  private void TryAddMaterialFrom(GameObject geoGameObject) {
    // Verify given GameObject is not null.
    if (geoGameObject == null) {
      return;
    }

    // If given GameObject has any MeshRenderers, see if their shared Materials need to be managed.
    foreach (MeshRenderer meshRenderer in geoGameObject.GetComponentsInChildren<MeshRenderer>()) {
      // If this Mesh Renderer has a shared Material, and if said shared Material's Shader has an
      // _Offset property, and if this specific shared Material has not already been stored, store
      // it now.
      Material material = meshRenderer.sharedMaterial;
      if (material != null) {
        TryAddMaterial(material);
      }
    }
  }

  /// <summary>Update the offset of all managed <see cref="WorldspaceMaterials"/>.</summary>
  /// <param name="delta">Amount to add to offset.</param>
  private void UpdateWorldspaceMaterialOffsets(Vector3 delta) {
    // Add the new offset amount to the total amount the world's geometry has been offset so far,
    // then apply this offset to all world space textured Materials. This makes sure world space
    // textured Materials stay aligned to their geometry, even after said geometry is moved by Maps
    // Service when the Floating Origin is recentered.
    Offset += delta;

    foreach (Material material in WorldspaceMaterials) {
      Vector3 modularOffset = Offset;
      modularOffset.x %= material.mainTextureScale.x;
      modularOffset.z %= material.mainTextureScale.y;

      material.SetVector(MaterialOffsetPropertyID, modularOffset);
    }
  }

  /// <summary>
  /// Verify that all required parameters have been correctly defined, returning false if not.
  /// </summary>
  private bool VerifyParameters() {
    // Verify that all required Materials have been given.
    if (Buildings == null) {
      Debug.LogError(ExampleErrors.MissingParameter(
        this, Buildings, "Buildings", "to assign to buildings"));
      return false;
    }
    if (Roads == null) {
      Debug.LogError(ExampleErrors.MissingParameter(
        this, Roads, "Roads", "to assign to road segments"));
      return false;
    }
    if (Borders == null) {
      Debug.LogError(ExampleErrors.MissingParameter(
        this, Borders, "Borders", "to assign to the borders of road segments and buildings"));
      return false;
    }
    if (Ground == null) {
      Debug.LogError(ExampleErrors.MissingParameter(
        this, Ground, "Ground", "to assign to ground regions"));
      return false;
    }
    if (Water == null) {
      Debug.LogError(ExampleErrors.MissingParameter(this, Water, "Water", "to assign to water"));
      return false;
    }

    // Verify at least one of the given Materials' Shader has a Vector that can be used for
    // providing the world's Floating Origin.
    if (!Buildings.HasProperty(MaterialOffsetProperty)
        && !Roads.HasProperty(MaterialOffsetProperty)
        && !Borders.HasProperty(MaterialOffsetProperty)
        && !Ground.HasProperty(MaterialOffsetProperty)
        && !Water.HasProperty(MaterialOffsetProperty)) {
      Debug.LogErrorFormat("No '{0}' property found on any of the Shaders of any of the Materials "
          + "given to {1}.{2}.\n{2} is used to adjust the offset of this Material's world space "
          + "texture coordinates, but given none of the given Materials had a '{0}' property to "
          + "use for this purpose, {1}.{2} will be disabled.",
          MaterialOffsetProperty, name, GetType());
    }

    // If have reached this point then have verified that all required parts are present and
    // properly setup.
    return true;
  }
}

