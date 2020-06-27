using System;
using System.Collections.Generic;
using System.Linq;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using UnityEngine;
using UnityEngine.UI;
using Google.Maps.Feature.Style;

namespace Google.Maps.Examples {
  /// TODO(b/149063230): Improve builder pattern usage.
  /// <summary>
  /// This adapter handles the UI style configuration for extruded structures and selected choices
  // to the <see cref="MapsService"/>.
  /// Changes made in the UI are directly applied to the GameObjectOptions attribute of the
  // <see cref="MapBaseLoader"/>.
  /// The attribute is used when loading a new map through maps service in
  /// <see cref="MapsStylingEditorView"/>.
  /// </summary>
  public class ExtrudedStructureConfigView : BaseConfigView {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Keeps track of the fixed height value")]
    public Slider fixedHeightSlider;

    [Tooltip("Keeps track of the footprint height value")]
    public Slider footprintHeightSlider;

    [Tooltip("Keeps track of the available choices for prefabs replacement")]
    public List<GameObject> prefabs;

    [Tooltip("Keeps track of the available choices for walls materials")]
    public List<Material> wallMaterials;

    [Tooltip("Keeps track of the available choices for roof materials")]
    public List<Material> roofMaterials;

    [Tooltip("Keeps track of the RoofMaterialAlignment type")]
    public Toggle isAlignToWorld;

    [Tooltip("Indicates if we need to apply a fixed height to all structures")]
    public Toggle applyFixedHeight;

    // Keeping references to all toggles help us initialize these components even when this
    // GameObject is inactive.
    public List<Toggle> prefabToggles;
    public List<Toggle> wallMaterialsToggles;
    public List<Toggle> roofMaterialsToggles;

    /// <summary>
    /// When applied to the style, all extruded structures returned by the request will be
    // replaced by the selection.
    /// </summary>
    private GameObject Prefab;

    /// <summary>
    /// When applied to the style, all extruded structures roof materials will be replaced by the
    // selection.
    /// </summary>
    private Material RoofMaterial;

    /// <summary>
    /// When applied to the style, all extruded structures wall materials will be replaced by the
    // selection.
    /// </summary>
    private Material WallMaterial;

    /// <summary>
    /// Keeps track of this configuration
    /// </summary>
    private ExtrudedStructureStyle StyleConfiguration;

    public override void ApplyStyle(GameObjectOptions options) {
      // The config panel might not have been initialized yet if it was inactive when this call was
      // made.
      if (StyleConfiguration == null)
        InitMapConfig();

      options.ExtrudedStructureStyle = StyleConfiguration;
    }

    protected override void InitView() {
      base.InitView();

      // This event listener allows us to apply conditional styling.
      // In this case,
      BaseMapLoader.MapsService.Events.ExtrudedStructureEvents.WillCreate.AddListener(arg0 => {
        // Apply all style parameters directly
        // if this map object verifies filter conditions
        if (useMetaDataAsFilters != null && useMetaDataAsFilters.isOn &&
            EvaluateFilters(arg0.MapFeature.Metadata.Usage.ToString())) {
          arg0.Style = StyleConfiguration;
        }
      });
    }

    protected override void InitMapConfig() {
      Debug.Log("Init +++");
      base.InitMapConfig();

      // Select last prefab (no selection)
      for (int i = 0; i < prefabToggles.Count; i++) {
        prefabToggles[i].isOn = i == (prefabToggles.Count - 1);
      }

      // Select the first wall material by default.
      for (int i = 0; i < wallMaterialsToggles.Count; i++) {
        wallMaterialsToggles[i].isOn = i == 0;
      }

      // Select the first roof material by default.
      for (int i = 0; i < roofMaterialsToggles.Count; i++) {
        roofMaterialsToggles[i].isOn = i == 0;
      }

      StyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.ExtrudedStructureStyle;

      // Use the first roof and wall material by default.
      RoofMaterial = roofMaterials.Count > 0 ? roofMaterials[0] : null;
      WallMaterial = wallMaterials.Count > 0 ? wallMaterials[0] : null;

      applyFixedHeight.isOn = StyleConfiguration.ApplyFixedHeight;
      fixedHeightSlider.value = StyleConfiguration.FixedHeight;
      footprintHeightSlider.value = StyleConfiguration.ExtrudedBuildingFootprintHeight;
      Prefab = StyleConfiguration.Prefab;

      isAlignToWorld.isOn = StyleConfiguration.RoofMaterialAlignment ==
                            ExtrudedStructureStyle.RoofMaterialAlignmentType.AlignToWorld;
      UpdateStyle();
    }

    protected override System.Type GetUsageType() {
      return typeof(StructureMetadata.UsageType);
    }

    /// <summary>
    /// Updates the extruded structure style of the base map loader.
    /// These styles are applied to the map on loading.
    /// </summary>
    private void UpdateStyle() {
      // Assigns selected materials to style
      StyleConfiguration =
          new ExtrudedStructureStyle
              .Builder {
                RoofMaterialAlignment =
                    isAlignToWorld.isOn
                        ? ExtrudedStructureStyle.RoofMaterialAlignmentType.AlignToWorld
                        : ExtrudedStructureStyle.RoofMaterialAlignmentType.AlignToDirection,
                ApplyFixedHeight = this.applyFixedHeight.isOn,
                FixedHeight = this.fixedHeightSlider.value,
                ExtrudedBuildingFootprintHeight = this.footprintHeightSlider.value,
                Prefab = this.Prefab,
                RoofMaterial = this.RoofMaterial,
                WallMaterial = this.WallMaterial
              }
              .Build();
    }

    /// <summary>
    /// Triggered when any of the following style parameters has changed:
    /// - apply fixed height
    /// - align to world
    /// - fixed height value
    /// - footprint height value
    /// </summary>
    public void OnParametersValueChanged() {
      UpdateStyle();
    }

    public void OnPrefabSelected(int idx) {
      Debug.Log("Idx:" + idx);

      if (idx < 0 || idx >= prefabs.Count)
        throw new System.Exception("Invalid option selected on prefabs list!");

      // Check the no prefabs option
      if (idx == prefabs.Count - 1)
        Prefab = null;
      else
        Prefab = prefabs[idx];
      UpdateStyle();
    }

    public void OnRoofMaterialSelected(int idx) {
      Debug.Log("OnRoofMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= roofMaterials.Count)
        throw new System.Exception("Invalid option selected on roof materials list!");
      RoofMaterial = roofMaterials[idx];
      UpdateStyle();
    }

    public void OnWallMaterialSelected(int idx) {
      Debug.Log("OnWallMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= wallMaterials.Count)
        throw new System.Exception("Invalid option selected on wall materials list!");
      WallMaterial = wallMaterials[idx];
      UpdateStyle();
    }
  }
}
