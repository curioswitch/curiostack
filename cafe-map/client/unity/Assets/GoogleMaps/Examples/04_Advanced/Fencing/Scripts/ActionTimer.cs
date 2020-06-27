using System;
using UnityEngine;

namespace Google.Maps.Examples {
  /// <summary>
  /// Component that performs a timed action on a <see cref="GameObject"/>.
  /// </summary>
  public class ActionTimer : MonoBehaviour {
    /// <summary>
    /// Action to perform after time expires.
    /// </summary>
    public Action Action;

    /// <summary>
    /// Time until expiry.
    /// </summary>
    public float Expiry;

    /// <summary>
    /// Run down expiry and trigger the action when the expiry is zero or less.
    /// </summary>
    public void Update() {
      Expiry -= Time.deltaTime;
      if (Expiry < 0) {
        Action();
        enabled = false;
      }
    }
  }
}
