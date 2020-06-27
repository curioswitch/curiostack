using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  public class ModeledStructureConfigView : BaseConfigView {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    [Tooltip("Keeps track of the available choices for prefabs replacement")]
    public List<GameObject> prefabs;

    public List<Toggle> prefabToggles;
    private GameObject Prefab;

    public List<Toggle> MaterialsToggles;
    public List<Material> Materials;

    /// <summary>
    /// When applied to the style, all extruded structures roof materials will be replaced by the
    /// selection.
    /// </summary>
    private Material ModeledStructureMaterial;

    /// <summary>
    /// Keeps track of this configuration
    /// </summary>
    private ModeledStructureStyle StyleConfiguration;

    public override void InitConfig() {
      base.InitConfig();

      // Make sure the meta data configuration panel is disabled.
      // At this time there isn't any meta data exposed for the Area Water map feature.
      this.metaDataConfigPanel.SetActive(false);
    }

    protected override void InitView() {
      base.InitView();

      // This event listener allows us to apply conditional styling.
      // In this case,
      BaseMapLoader.MapsService.Events.ModeledStructureEvents.WillCreate.AddListener(arg0 => {
        // Apply all style parameters directly
        // if this map object verifies filter conditions
        if (useMetaDataAsFilters != null && useMetaDataAsFilters.isOn &&
            EvaluateFilters(arg0.MapFeature.Metadata.Usage.ToString())) {
          arg0.Style = StyleConfiguration;
        }
      });
    }

    public override void ApplyStyle(GameObjectOptions options) {
      Debug.Log("Apply area water styles");

      // The config panel might not have been initialized yet if it was inactive when this call was
      // made.
      if (StyleConfiguration == null)
        InitMapConfig();

      options.ModeledStructureStyle = StyleConfiguration;
    }

    protected override System.Type GetUsageType() {
      return typeof(StructureMetadata.UsageType);
    }

    protected override void InitMapConfig() {
      base.InitMapConfig();

      // Select first roof material by default
      for (int i = 0; i < MaterialsToggles.Count; i++) {
        MaterialsToggles[i].isOn = i == 0;
      }

      // Style configuration has the material from default
      StyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.ModeledStructureStyle;
      Prefab = StyleConfiguration.Prefab;

      if (Materials != null && Materials.Count > 0) {
        ModeledStructureMaterial = Materials[0];
      }

      UpdateStyle();
    }

    private void UpdateStyle() {
      StyleConfiguration =
          new ModeledStructureStyle
              .Builder { Prefab = this.Prefab, Material = this.ModeledStructureMaterial }
              .Build();
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

    public void OnMaterialSelected(int idx) {
      Debug.Log("OnMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= Materials.Count)
        throw new System.Exception("Invalid option selected on the materials list!");
      ModeledStructureMaterial = Materials[idx];
      UpdateStyle();
    }
  }
}
