using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature;
using Google.Maps.Feature.Style;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  public class RegionConfigView : BaseConfigView {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    public List<Toggle> FillMaterialsToggles;
    public List<Material> FillMaterials;
    private Material FillMaterial;

    private SegmentStyle OutlineStyleConfiguration;

    /// <summary>
    /// Keeps track of this configuration
    /// </summary>
    private RegionStyle StyleConfiguration;

    public override void ApplyStyle(GameObjectOptions options) {
      Debug.Log("Apply region styles");

      // The config panel might not have been initialized yet if it was inactive when this call was
      // made.
      if (StyleConfiguration == null)
        InitMapConfig();

      options.RegionStyle = StyleConfiguration;
    }

    protected override void InitView() {
      base.InitView();

      // This event listener allows us to apply conditional styling.
      // In this case,
      BaseMapLoader.MapsService.Events.RegionEvents.WillCreate.AddListener(arg0 => {
        // Apply all style parameters directly
        // if this map object verifies filter conditions
        if (useMetaDataAsFilters != null && useMetaDataAsFilters.isOn &&
            EvaluateFilters(arg0.MapFeature.Metadata.Usage.ToString())) {
          arg0.Style = StyleConfiguration;
        }
      });
    }

    // No usage type for AreaWater
    protected override System.Type GetUsageType() {
      return typeof(RegionMetadata.UsageType);
    }

    protected override void InitMapConfig() {
      base.InitMapConfig();

      // Select first roof material by default
      for (int i = 0; i < FillMaterialsToggles.Count; i++) {
        FillMaterialsToggles[i].isOn = i == 0;
      }

      StyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.RegionStyle;
      OutlineStyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.SegmentStyle;

      if (FillMaterials != null && FillMaterials.Count > 0) {
        FillMaterial = FillMaterials[0];
      }

      UpdateStyle();
    }

    private void UpdateStyle() {
      StyleConfiguration =
          new RegionStyle
              .Builder {
                Fill = true,
                FillMaterial = this.FillMaterial,
                Outline = false,
                OutlineStyle = this.OutlineStyleConfiguration
              }
              .Build();
    }

    public void OnRegionMaterialSelected(int idx) {
      Debug.Log("OnRegionMaterialSelected Idx:" + idx);

      if (idx < 0 || idx >= FillMaterials.Count)
        throw new System.Exception("Invalid option selected on the segment materials list!");
      FillMaterial = FillMaterials[idx];
      UpdateStyle();
    }
  }
}
