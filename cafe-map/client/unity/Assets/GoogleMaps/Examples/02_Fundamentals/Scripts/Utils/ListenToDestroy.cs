using UnityEngine;
using UnityEngine.Events;

namespace Google.Maps.Examples {
  /// <summary>
  /// Dispatches a Unity Event when the <see cref="GameObject"/> is destroyed.
  ///
  /// </summary>
  public class ListenToDestroy : MonoBehaviour {
    public MapFeatureDestroyedEvent MapFeatureDestroyed;

    // The place Id associated to this GameObject
    public string PlaceId;

    // Update is called once per frame
    private void OnDestroy() {
      // Notify all listeners that this GameObject is about to be destroyed
      if (MapFeatureDestroyed != null) {
        MapFeatureDestroyed.Invoke(PlaceId);
      }
    }
  }

  [System.Serializable]
  public class MapFeatureDestroyedEvent : UnityEvent<string> {}
}
