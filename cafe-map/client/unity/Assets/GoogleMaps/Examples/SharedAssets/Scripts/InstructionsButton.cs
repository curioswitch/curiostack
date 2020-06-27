using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Script for hiding this <see cref="GameObject"/> at the press of the required
  /// <see cref="Button"/>.
  /// </summary>
  [RequireComponent(typeof(Button))]
  public sealed class InstructionsButton : MonoBehaviour {
    [Tooltip(
        "Optional Slider to extend upwards to fill the empty space left by this Button when it " +
        "is hidden.")]
    public Slider Slider;

    /// <summary>
    /// Connect <see cref="Button"/> press to hiding of this <see cref="GameObject"/>.
    /// </summary>
    private void Start() {
      // Get required Button, and connect to hiding of this GameObject (and optionally extending
      // given Slider).
      Button button = GetComponent<Button>();

      button.onClick.AddListener(() => {
        // Make this Button non-interactive so it cannot be clicked again.
        button.interactable = false;

        // See if a Slider component has been given as a parameter.
        if (Slider != null) {
          // Adjust Slider's height by the height of this Button's displayed image, so that as we
          // hide the Button, the Slider's height is extended over the empty space left by the
          // Button.
          float buttonHeight = button.targetGraphic.rectTransform.rect.height;
          RectTransform sliderRect = Slider.GetComponent<RectTransform>();
          sliderRect.offsetMax = sliderRect.offsetMax + Vector2.up * buttonHeight;
        }

        // Hide this Button.
        gameObject.SetActive(false);
      });
    }
  }
}
