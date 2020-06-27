using System;
using System.Collections.Generic;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Style;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class handles configuration parameters for the Water Area Map Feature.
  /// </summary>
  /// <remarks>Area Water does not expose any meta data. Therefore the metadata tab is hidden for
  /// this configuration view.</remarks>
  public class AreaWaterConfigView : BaseConfigView {
    [Tooltip("Reference to the base map loader")]
    public BaseMapLoader BaseMapLoader;

    public List<Toggle> FillMaterialsToggles;
    public List<Material> AreaWaterFillMaterials;
    private Material FillMaterial;

    /// <summary>
    /// Keeps track of this configuration
    /// </summary>
    private AreaWaterStyle StyleConfiguration;

    private SegmentStyle OutlineStyleConfiguration;

    public override void InitConfig() {
      base.InitConfig();

      // Make sure the meta data configuration panel is disabled.
      // At this time there isn't any meta data exposed for the Area Water map feature.
      this.metaDataConfigPanel.SetActive(false);
    }

    public override void ApplyStyle(GameObjectOptions options) {
      // The config panel might not have been initialized yet if it was inactive when this call was
      // made.
      if (StyleConfiguration == null)
        InitMapConfig();

      options.AreaWaterStyle = StyleConfiguration;
    }

    protected override void InitView() {
      // Do nothing, there is no metadata for this map feature.
    }

    // No usage type for AreaWater
    protected override System.Type GetUsageType() {
      throw new NotImplementedException();
    }

    protected override void InitMapConfig() {
      // Select first roof material by default
      for (int i = 0; i < FillMaterialsToggles.Count; i++) {
        FillMaterialsToggles[i].isOn = i == 0;
      }

      StyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.AreaWaterStyle;
      OutlineStyleConfiguration = ExampleDefaults.DefaultGameObjectOptions.SegmentStyle;

      if (AreaWaterFillMaterials != null && AreaWaterFillMaterials.Count > 0) {
        FillMaterial = AreaWaterFillMaterials[0];
      }

      UpdateStyle();
    }

    private void UpdateStyle() {
      StyleConfiguration =
          new AreaWaterStyle
              .Builder {
                Fill = true,
                FillMaterial = this.FillMaterial,
                Outline = false,
                OutlineStyle = this.OutlineStyleConfiguration
              }
              .Build();
    }

    public void OnAreaWaterMaterialSelected(int idx) {
      if (idx < 0 || idx >= AreaWaterFillMaterials.Count)
        throw new System.Exception("Invalid option selected on the segment materials list!");
      FillMaterial = AreaWaterFillMaterials[idx];
      UpdateStyle();
    }
  }
}
