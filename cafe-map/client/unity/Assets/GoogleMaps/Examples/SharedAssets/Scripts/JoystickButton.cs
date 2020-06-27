using UnityEngine.UI;

namespace Google.Maps.Demos.Utilities {
  /// <summary>
  /// Helper class to expose the IsPressed status from the standard button.
  /// </summary>
  public class JoystickButton : Button {
    /// <summary>
    /// Checks if the button is continuously pressed.
    /// </summary>
    /// <returns>A boolean indicating if the button is currently pressed.</returns>
    public bool IsButtonPressed() {
      return IsPressed();
    }
  }
}
