using UnityEngine;
using UnityEngine.UI;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// This class controls the close and open panel animations.
  /// There are two animations provided based on the device orientation.
  /// In Portrait mode, we open the panel until the opposite edge,
  /// while in Landscape mode, we limit the opening to half of the screen.
  /// The reason is purely esthetic. In landscape mode we limit the amount of empty white space
  /// on the panel.
  /// Both animations are setup on the same controller. We just use different trigger keys.
  /// </summary>
  public class PanelAnimationController : MonoBehaviour {
    /// <summary>
    /// Reference to the close panel button
    /// </summary>
    public Button ClosePanelButton;
    /// <summary>
    /// Reference to the open panel button
    /// </summary>
    public Button OpenPanelButton;
    /// <summary>
    /// Reference to the animator component that drives the animation state machines for both
    /// vertical and horizontal screen orientations.
    /// </summary>
    public Animator Animator;

    /// <summary>
    /// Trigger key used in specific state machines configured in animator.
    /// </summary>
    private string TriggerKey = "closeV";

    /// <summary>
    /// Used to detect changes in screen orientation.
    /// When this happens, we close the panel to prevent confusion with the two state machines
    /// implemented in the associated animator. Closing the panel resets the current state machine
    /// to a common idle state.
    /// </summary>
    private ScreenOrientation CurrentScreenOrientation;

    /// <summary>
    /// On start, check the availability of the animator, open and close buttons.
    /// </summary>
    void Start() {
      Debug.Assert(Animator, "Missing close panel animation!");
      Debug.Assert(ClosePanelButton, "Missing close panel button!");
      Debug.Assert(OpenPanelButton, "Missing open panel button!");

      CurrentScreenOrientation = Screen.orientation;
    }

    /// <summary>
    /// On updates, apply the correct trigger key based on the screen orientation.
    /// </summary>
    void Update() {
      if (CurrentScreenOrientation != Screen.orientation) {
        OnPanelClose();
        CurrentScreenOrientation = Screen.orientation;
      } else {
        if (Screen.orientation == ScreenOrientation.Portrait) {
          TriggerKey = "closeV";
        } else {
          TriggerKey = "closeH";
        }
      }
    }

    /// <summary>
    /// Trigger the animation and update the open/close buttons.
    /// </summary>
    public void OnPanelOpen() {
      // Slide panel left
      Animator.SetBool(TriggerKey, false);
      ShowOpenPanelButton(false);
    }

    /// <summary>
    /// Trigger the animation and update the open/close buttons.
    /// </summary>
    public void OnPanelClose() {
      // Slide panel right
      Animator.SetBool(TriggerKey, true);
      ShowOpenPanelButton(true);
    }

    /// <summary>
    /// Hides/shows either close or open buttons.
    /// </summary>
    /// <param name="isOpened">Indicates if the panel should be opened or close.</param>
    public void ShowOpenPanelButton(bool isOpened) {
      ClosePanelButton.gameObject.SetActive(!isOpened);
      OpenPanelButton.gameObject.SetActive(isOpened);
    }
  }
}
