using UnityEngine;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// This component is used to cleanup meshes (if any) when the associated <see cref="GameObject"/>
  /// is destroyed.
  /// </summary>
  /// <remarks>
  /// This is done to prevent a memory leak that happens with decorations (such as parapets) as we
  /// are dynamically loading/unloading map regions.
  /// </remarks>
  public class MeshCleaner : MonoBehaviour {
    private void OnDestroy() {
      // Does this GameObject have a mesh filter?
      // We destroy the current mesh (by accessing sharedMesh)
      MeshFilter mf = this.gameObject.GetComponent<MeshFilter>();

      if (mf != null && mf.sharedMesh) {
        Destroy(mf.sharedMesh);
      }
    }
  }
}
