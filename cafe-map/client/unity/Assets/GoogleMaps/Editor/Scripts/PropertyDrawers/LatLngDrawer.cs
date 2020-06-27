using System.Globalization;
using Google.Maps.Coord;
using UnityEditor;
using UnityEngine;

namespace Google.Maps.Editor.PropertyDrawers {
  /// <summary>
  /// A custom property drawer for <see cref="LatLng"/>, that allows you to supply a LatLng as a
  /// comma separated pair of numbers.
  /// </summary>
  [CustomPropertyDrawer(typeof(LatLng))]
  public class LatLngDrawer : PropertyDrawer {
    /// <summary>
    /// The available on-screen width to the right of the property drawer label for controls. Used
    /// to calculate the height of the property drawer.
    /// </summary>
    private float DrawerWidth;

    /// <summary>
    /// The <see cref="GUIContent"/> for the instructions on entering a <see cref="LatLng"/> into
    /// the property drawer.
    /// </summary>
    private readonly GUIContent InstructionsContent =
        new GUIContent("Enter a comma separated latitude and longitude:");

    /// <summary>
    /// The default content of the text field which allows the user to enter a <see cref="LatLng"/>.
    /// </summary>
    private readonly GUIContent TextFieldContent = new GUIContent("");

    /// <summary>
    /// Previous text field string.
    /// </summary>
    private string PreviousString = null;

    /// <inheritdoc/>
    public override void OnGUI(Rect position, SerializedProperty property, GUIContent label) {
      EditorGUI.BeginProperty(position, label, property);

      // Draw property name.
      position = EditorGUI.PrefixLabel(position, GUIUtility.GetControlID(FocusType.Passive), label);
      DrawerWidth = position.width;

      // Draw instructions.
      EditorGUI.LabelField(position, InstructionsContent);

      // Position text field.
      position.y += EditorStyles.label.CalcHeight(InstructionsContent, position.width);
      position.height = EditorStyles.textField.CalcHeight(TextFieldContent, position.width);

      // Draw text field and update LatLng if valid.
      string newLatLng =
          EditorGUI.TextField(position, TextFieldContent, LatLngPropertyToString(property));

      if (!newLatLng.Equals(PreviousString)) {
        PreviousString = newLatLng;
        TrySetLatLng(newLatLng, property);
      }

      EditorGUI.EndProperty();
    }

    /// <inheritdoc/>
    public override float GetPropertyHeight(SerializedProperty property, GUIContent label) {
      return EditorStyles.label.CalcHeight(InstructionsContent, DrawerWidth) +
             EditorStyles.textField.CalcHeight(TextFieldContent, DrawerWidth);
    }

    /// <summary>
    /// Attempts to extract a numerical latitude/longitude from a comma-separated string in the
    /// format of "lat, lng". Valid output, when found, is written to the <see cref="lat"/> and
    /// <see cref="lng"/> arguments respectively. If parts of the string contain invalid data, or
    /// the string is incorrectly formatted, the latitude and/or longitude will be set to 0.
    /// </summary>
    /// <remarks>
    /// In the event the provided string is only partially incorrect, useful information is
    /// extracted where possible. For example, if <see cref="latLngString"/> was set to
    /// "15, bad_input", the latitude would still be set to 15. This is done to reduce information
    /// loss in the face of human error.
    /// </remarks>
    /// <param name="latLngString">
    /// The comma separated latitude/longitude string in the format "lat, lng".
    /// </param>
    /// <param name="lat">
    /// The parsed latitude. This will be 0 if the latitude in <see cref="latLngString"/> was
    /// malformed.
    /// </param>
    /// <param name="lng">
    /// The parsed longitude. This will be 0 if the longitude in <see cref="latLngString"/> was
    /// malformed.
    /// </param>
    internal void TryParseLatLng(string latLngString, out double lat, out double lng) {
      // Split on comma to extract individual components.
      string[] components = latLngString.Split(',');

      lat = 0;
      lng = 0;

      // Attempt to parse as many components as we have available, trimming off excess comma
      // separated values. This is done so that a user does not lose information if they can only
      // paste the latitude and longitude separately.
      //
      // The below calls to double.TryParse provides equivalent formatting to
      // double.TryParse(string, out double), but it enforces a particular culture/locale instead of
      // relying on the system locale, which can produce unexpected results if the system is not set
      // to English. In this case the invariant culture is used as it is geared towards English
      // formatting, but independent of any country, producing reliable numerical formatting.
      if (components.Length >= 1) {
        double.TryParse(
            components[0],
            NumberStyles.Float | NumberStyles.AllowThousands,
            CultureInfo.InvariantCulture,
            out lat);
      }

      if (components.Length >= 2) {
        double.TryParse(
            components[1],
            NumberStyles.Float | NumberStyles.AllowThousands,
            CultureInfo.InvariantCulture,
            out lng);
      }
    }

    /// <summary>
    /// Converts a latitude and longitude pair of doubles to a string representation suitable for
    /// display in the Unity inspector.
    /// </summary>
    /// <param name="lat">The latitude to convert.</param>
    /// <param name="lng">The longitude to convert.</param>
    /// <returns>A string in the format "lat, lng".</returns>
    internal string LatLngToString(double lat, double lng) {
      // The below calls to string.Format provides equivalent formatting to
      // string.Format(string, args[]), but it enforces a particular culture/locale instead of
      // relying on the system locale, which can produce unexpected results if the system is not set
      // to English. In this case the invariant culture is used as it is geared towards English
      // formatting, but independent of any country, producing reliable numerical formatting.
      return string.Format(CultureInfo.InvariantCulture, "{0}, {1}", lat, lng);
    }

    /// <summary>
    /// Attempts to set the underlying <see cref="LatLng"/> property from a comma separated string
    /// of numbers in the format "lat, lng". If parts of the string contain invalid data, or the
    /// string is incorrectly formatted, the latitude and/or longitude will be set to 0.
    /// </summary>
    /// <param name="latLngString">
    /// The comma separate latitude/longitude string in the format "lat, lng".
    /// </param>
    /// <param name="property">
    /// The <see cref="SerializedProperty"/> containing the <see cref="LatLng"/>.
    /// </param>
    private void TrySetLatLng(string latLngString, SerializedProperty property) {
      double lat, lng;
      TryParseLatLng(latLngString, out lat, out lng);

      property.FindPropertyRelative("_Lat").doubleValue = lat;
      property.FindPropertyRelative("_Lng").doubleValue = lng;
    }

    /// <summary>
    /// Converts a <see cref="LatLng"/> to a <see cref="string"/> representation suitable for
    /// display in the Unity inspector.
    /// </summary>
    /// <param name="latLng">The <see cref="LatLng"/> to convert.</param>
    /// <returns>A string in the format "lat, lng".</returns>
    private string LatLngPropertyToString(SerializedProperty property) {
      double lat = property.FindPropertyRelative("_Lat").doubleValue;
      double lng = property.FindPropertyRelative("_Lng").doubleValue;

      return LatLngToString(lat, lng);
    }
  }
}
