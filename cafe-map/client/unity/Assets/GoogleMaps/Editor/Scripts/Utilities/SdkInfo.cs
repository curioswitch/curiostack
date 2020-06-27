using System;
using UnityEditor;
using Google.Maps;
using UnityEngine;

public class SdkInfo : EditorWindow {
  private const int WindowWidth = 400;

  private const int WindowHeight = 200;

  /// <summary>
  /// Style applied to Title text.
  /// </summary>
  private static GUIStyle TitleLabelStyle = new GUIStyle(EditorStyles.boldLabel);

  /// <summary>
  /// Style applied to the version label.
  /// </summary>
  private static GUIStyle VersionLabelStyle = new GUIStyle(EditorStyles.label);

  /// <inheritdoc/>
  [MenuItem("Maps SDK for Unity/About")]
  private static void Init() {
    SetStyles();

    SdkInfo window = (SdkInfo) EditorWindow.GetWindow(typeof(SdkInfo));
    window.Show();
  }

  /// <summary>
  /// Set styles for Window UI.
  /// </summary>
  private static void SetStyles() {
    // Title label style.
    TitleLabelStyle.fontSize = 25;
    TitleLabelStyle.alignment = TextAnchor.MiddleCenter;
    TitleLabelStyle.margin.top = 20;
    TitleLabelStyle.margin.bottom = 20;

    // Version label style.
    VersionLabelStyle.alignment = TextAnchor.MiddleCenter;
    VersionLabelStyle.margin.top = 5;
    VersionLabelStyle.margin.bottom = 5;
  }

  /// <inheritdoc/>
  private void OnGUI() {
    // Set basic window properties.
    this.titleContent.text = "About Maps SDK for Unity";
    this.minSize = new Vector2(WindowWidth, WindowHeight);
    this.maxSize = new Vector2(WindowWidth, WindowHeight);

    GUILayout.BeginVertical();
    GUILayout.FlexibleSpace();

    // Title.
    GUILayout.Label("Google Maps SDK for Unity", TitleLabelStyle);

    // Version.
    Version version = typeof(MapsService).Assembly.GetName().Version;
    GUILayout.Label(string.Format("Version: {0}", version), VersionLabelStyle);

    GUILayout.FlexibleSpace();
    GUILayout.EndVertical();
  }
}
