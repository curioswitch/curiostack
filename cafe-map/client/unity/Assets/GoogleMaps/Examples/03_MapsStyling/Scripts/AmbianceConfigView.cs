using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples {
  /// <summary>
  /// This class handles the selection of a ground material to the current scene.
  /// </summary>
  public class AmbianceConfigView : MonoBehaviour {
    public GameObject ground;
    public List<Material> FillMaterials;
    private Material FillMaterial;

    public void OnGroundMaterialSelected(int idx) {
      if (idx < 0 || idx >= FillMaterials.Count)
        throw new System.Exception("Invalid option selected on the materials list!");

      Renderer r = ground.GetComponent<Renderer>();

      if (r != null) {
        r.material = FillMaterials[idx];
      }
    }
  }
}
