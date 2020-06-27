using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Used to check whether the <see cref="GameObject"/> is in a fenced area.
  /// </summary>
  public class FenceChecker : MonoBehaviour {
    /// <summary>
    /// Returns true if the <see cref="GameObject"/> is protected by a fence, false otherwise.
    /// </summary>
    public bool Fenced() {
      Collider gameObjectCollider = gameObject.GetComponent<Collider>();
      Bounds gameObjectBounds = gameObjectCollider.bounds;
      Collider[] fenceColliders = Physics.OverlapBox(
          gameObjectBounds.center, gameObjectBounds.extents);

      foreach (Collider fenceCollider in fenceColliders) {
        Fence fence = fenceCollider.gameObject.GetComponent<Fence>();
        if (fence != null) {
          return true;
        }
      }

      return false;
    }
  }
}
