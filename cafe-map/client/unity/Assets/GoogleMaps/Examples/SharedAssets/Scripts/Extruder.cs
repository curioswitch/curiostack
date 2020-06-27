using System;
using System.Collections.Generic;
using Google.Maps.Feature;
using Google.Maps.Feature.Shape;
using UnityEngine;
using Random = UnityEngine.Random;

namespace Google.Maps.Examples.Shared {
  /// <summary>
  /// Static class containing logic for creating extrusions and lofts from given geometry built by
  /// the Maps SDK for Unity.
  /// </summary>
  public static class Extruder {
    /// <summary>Default thickness of created extrusions.</summary>
    /// <remarks>This value will be used for all extrusions unless a custom value is
    /// given.</remarks>
    private const float DefaultWidth = 1.0f;

    /// <summary>Name given to <see cref="GameObject"/>s created as parapets.</summary>
    private const string ParapetName = "Parapet";

    /// <summary>
    /// Name given to <see cref="GameObject"/>s created as building base decorations.
    /// </summary>
    private const string BorderName = "Border";

    /// <summary>Name given to <see cref="GameObject"/>s created as area outlines.</summary>
    private const string OutlineName = "Outline";

    /// <summary>
    /// Physical scale (in meters) of the <see cref="Material"/> applied to the extrusions.
    /// </summary>
    /// <remarks>
    /// The extrusion generation code assigns UVs based on real world scale in meters, meaning that
    /// two vertices that are a meter apart will have a UV coordinate that differs by 1.
    /// <para>
    /// For example, if you have a texture that corresponds to a 5x5 meter square, this value
    /// should be set to 5.0f. This is an alternative to using a texture tiling of 0.2f on (i.e.
    /// tile 5 times per 1 uv value) on a Unity Standard Material.
    /// </para></remarks>
    private const float UvScale = 1.0f;

    /// <summary>
    /// Two dimensional cross-sections we will use to form flat extrusions around a given shape.
    /// </summary>
    /// <remarks>
    /// See <see cref="ParapetShapes"/> for an explanation of the coordinate system.
    /// </remarks>
    private static readonly Vector2[] BorderShape = { new Vector2(1f, 0f), new Vector2(0f, 0f) };

    /// <summary>
    /// Default height applied to Maps SDK for Unity generated buildings that do not have stored
    /// height information. The chosen value of 10f matches the default value used inside the Maps
    /// SDK for buildings without stored heights. <para> The Maps SDK for Unity default height can
    /// be overriden with styling options, specifically <see
    /// cref="GameObjectOptions.ExtrudedStructureStyle"/>'s ExtrudedBuildingFootprintHeight. If this
    /// default height is overriden when calling <see cref="MapsService.LoadMap"/>, then this new
    /// default height value should also be given when calling
    /// <see cref="Extruder.AddBuildingParapet"/> to make sure that building parapets appear at the
    /// roof-level of all buildings, even if these buildings don't have a stored height.
    /// </para></summary>
    private const float ExtrudedBuildingFootprintHeight = 10.0f;

    /// <summary>
    /// Two dimensional cross-sections that will be used to form the parapets.
    /// </summary>
    /// <remarks>
    /// Each entry defines the cross section of a parapet in a coordinate space relative to the
    /// outer roof-edge of the building, where the positive x-axis points out away from the
    /// building, and the positive y-axis points towards the sky. So, for example: <para>(1, 0) is 1
    /// meter out of the building.</para> <para>(1, -1) is 1 meter out, 1 meter down.</para>
    /// <para>Outlines should be specified with an counterclockwise winding order (assuming +x
    /// right, +y up) to ensure the normals of the generated geometry face in the correct
    /// direction.</para>
    /// </remarks>
    private static readonly Vector2[][] ParapetShapes = {
      // A square parapet running along the outer edge of a roof, not overhanging exterior walls.
      MakeVector2Array(0f, 0f, 0f, 1f, -1f, 1f, -1f, 0f),

      // A square parapet running along the outer edge of a roof, slightly overlapping the roof, and
      // overhanging exterior walls.
      MakeVector2Array(-0.5f, 0f, 1f, 0f, 1f, 1f, -0.5f, 1f, -0.5f, 0f),

      // A stepped parapet that overhangs exterior walls, with the steps facing down towards the
      // ground.
      MakeVector2Array(-1f, 0f, 0.5f, 0f, 0.5f, 0.5f, 1f, 0.5f, 1f, 1.0f, -1f, 1f, -1f, 0f),

      // A stepped parapet that does not overhang exterior walls, with the steps facing upwards
      // towards the sky.
      MakeVector2Array(0f, 0f, 0f, 1f, -0.5f, 1f, -0.5f, 0.5f, -1f, 0.5f, -1f, 0f),

      // A bevelled parapet that overhangs exterior walls, similar to the steps facing upwards but
      // with a slope in place of the middle step.
      MakeVector2Array(0f, -0.5f, 1f, -0.5f, 1f, 0.5f, 0.5f, 1f, 0f, 1f, 0f, -0.5f)
    };

    /// <summary>Returns a cross-section to be used for an outline with a given width.</summary>
    private static Vector2[] OutlineCrossSectionWithWidth(float width) {
      return new Vector2[] { new Vector2(width / 2, 0f), new Vector2(-width / 2, 0f) };
    }

    /// <summary>Adds a extruded border around the base of a given building.</summary>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="buildingShape">
    /// The Maps SDK for Unity <see cref="MapFeature"/> data defining this building's shape and
    /// height.
    /// </param>
    /// <param name="borderMaterial">
    /// The <see cref="Material"/> to apply to the extrusion once it is created.
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created extrusion geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para></returns>
    public static GameObject[] AddBuildingBorder(
        GameObject buildingGameObject, ExtrudedArea buildingShape, Material borderMaterial,
        float thickness = DefaultWidth) {
      // Create list to store all created borders.
      List<GameObject> extrudedBorders = new List<GameObject>();

      for (int i = 0; i < buildingShape.Extrusions.Length; i++) {
        // Use general-purpose building-extrusion function to add border around building.
        AddBuildingExtrusion(
            buildingGameObject,
            borderMaterial,
            buildingShape.Extrusions[i],
            BorderShape,
            0f,
            ref extrudedBorders,
            false,
            i,
            buildingShape.Extrusions.Length,
            thickness);
      }

      // Return all created extrusions.
      return extrudedBorders.ToArray();
    }

    /// <summary>
    /// Adds a parapet of a randomly chosen cross-section to the given building.
    /// </summary>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="buildingShape">
    /// The Maps SDK for Unity <see cref="MapFeature"/> data defining this building's shape and
    /// height.
    /// </param>
    /// <param name="parapetMaterial">
    /// The <see cref="Material"/> to apply to the parapet once it is created.
    /// </param>
    /// <param name="defaultBuildingHeight">
    /// Default height applied to Maps SDK for Unity generated buildings that do not have stored
    /// height information. If left blank, a value of 10f matches the default value used inside the
    /// Maps SDK for buildings without stored heights.
    /// <para>
    /// The Maps SDK for Unity default height can be overriden with styling options, specifically
    /// <see cref="GameObjectOptions.ExtrudedStructureStyle"/>'s ExtrudedBuildingFootprintHeight. If
    /// this default height is overriden when calling <see cref="MapsService.LoadMap"/>, then this
    /// new default height value should also be used here to make sure that building parapets appear
    /// at the roof-level of all buildings, even if these buildings don't have a stored height.
    /// </para></param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created parapet geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para></returns>
    public static GameObject[] AddRandomBuildingParapet(
        GameObject buildingGameObject, ExtrudedArea buildingShape, Material parapetMaterial,
        float defaultBuildingHeight = ExtrudedBuildingFootprintHeight) {
      return AddBuildingParapet(
          buildingGameObject, buildingShape, parapetMaterial, defaultBuildingHeight, null);
    }

    /// <summary>Updates the parapet decoration on a building.</summary>
    /// <remarks>
    /// This method removes any existing parapet decoration and builds a new parapet child object in
    /// the same manner as the <see cref="AddRandomBuildingParapet"/> method. No attempt is made to
    /// retain the same parapet type. In a more sophisticated implementation, the assigned parapet
    /// type would be stored on the building GameObject to be retrieved here.
    /// </remarks>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="buildingShape">
    /// The Maps SDK for Unity <see cref="MapFeature"/> data defining this building's shape and
    /// height.
    /// </param>
    /// <param name="parapetMaterial">
    /// The <see cref="Material"/> to apply to the parapet once it is created.
    /// </param>
    /// <param name="defaultBuildingHeight">
    /// Default height applied to Maps SDK for Unity generated buildings that do not have stored
    /// height information. If left blank, a value of 10f matches the default value used inside the
    /// SDK for buildings without stored heights.
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created parapet geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para></returns>
    public static GameObject[] UpdateBuildingParapet(
        GameObject buildingGameObject, ExtrudedArea buildingShape, Material parapetMaterial,
        float defaultBuildingHeight = ExtrudedBuildingFootprintHeight) {
      RemoveCurrentParapet(buildingGameObject);

      return AddRandomBuildingParapet(
          buildingGameObject, buildingShape, parapetMaterial, defaultBuildingHeight);
    }

    /// <summary>
    /// Removes any child object from the supplied GameObject where the child's name indicates it is
    /// a parapet decoration.
    /// </summary>
    /// <param name="buildingGameObject">The object from which to remove existing parapet(s)</param>
    private static void RemoveCurrentParapet(GameObject buildingGameObject) {
      for (int i = 0; i < buildingGameObject.transform.childCount; i++) {
        GameObject child = buildingGameObject.transform.GetChild(i).gameObject;

        if (child.name == ParapetName) {
          GameObject.Destroy(child);
        }
      }
    }

    /// <summary>
    /// Adds a parapet of a specifically chosen cross-section to the given building.
    /// </summary>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="buildingShape">
    /// The Maps SDK for Unity <see cref="MapFeature"/> data defining this building's shape and
    /// height.
    /// </param>
    /// <param name="parapetMaterial">
    /// The <see cref="Material"/> to apply to the parapet once it is created.
    /// </param>
    /// <param name="parapetType">
    /// Optional index of parapet to cross-section to use. Will use a randomly chosen cross-section
    /// if no index given, or if given index is invalid (in which case an error will also be
    /// printed).
    /// </param>
    /// <param name="defaultBuildingHeight">
    /// Default height applied to Maps SDK for Unity generated buildings that do not have stored
    /// height information. If left blank, a value of 10f matches the default value used inside the
    /// Maps SDK for buildings without stored heights.
    /// <para>
    /// The Maps SDK for Unity default height can be overriden with styling options, specifically
    /// <see cref="GameObjectOptions.ExtrudedStructureStyle"/>'s ExtrudedBuildingFootprintHeight. If
    /// this default height is overriden when calling <see cref="MapsService.LoadMap"/>, then this
    /// new default height value should also be used here to make sure that building parapets appear
    /// at the roof-level of all buildings, even if these buildings don't have a stored height.
    /// </para></param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created parapet geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para></returns>
    public static GameObject[] AddBuildingParapet(
        GameObject buildingGameObject, ExtrudedArea buildingShape, Material parapetMaterial,
        int parapetType, float defaultBuildingHeight = ExtrudedBuildingFootprintHeight) {
      return AddBuildingParapet(
          buildingGameObject, buildingShape, parapetMaterial, defaultBuildingHeight, parapetType);
    }

    /// <summary>
    /// Adds a parapet of a randomly chosen cross-section to the given building.
    /// </summary>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="buildingShape">
    /// The Maps SDK for Unity <see cref="MapFeature"/> data defining this building's shape and
    /// height.
    /// </param>
    /// <param name="parapetMaterial">
    /// The <see cref="Material"/> to apply to the parapet once it is created.
    /// </param>
    /// <param name="defaultBuildingHeight">
    /// Default height applied to Maps SDK for Unity generated buildings that do not have stored
    /// height information. If left blank, a value of 10f matches the default value used inside the
    /// Maps SDK for buildings without stored heights.
    /// <para>
    /// The Maps SDK for Unity default height can be overriden with styling options, specifically
    /// <see cref="GameObjectOptions.ExtrudedStructureStyle"/>'s ExtrudedBuildingFootprintHeight. If
    /// this default height is overriden when calling <see cref="MapsService.LoadMap"/>, then this
    /// new default height value should also be used here to make sure that building parapets appear
    /// at the roof-level of all buildings, even if these buildings don't have a stored height.
    /// </para></param>
    /// <param name="parapetType">
    /// Optional index of parapet to cross-section to use. Will use a randomly chosen cross-section
    /// if no index given, or if given index is invalid (in which case an error will also be
    /// printed).
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created parapet geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para></returns>
    private static GameObject[] AddBuildingParapet(
        GameObject buildingGameObject, ExtrudedArea buildingShape, Material parapetMaterial,
        float defaultBuildingHeight, int? parapetType) {
      // Create list to store all created parapets.
      List<GameObject> extrudedParapets = new List<GameObject>();

      for (int i = 0; i < buildingShape.Extrusions.Length; i++) {
        // Use ExtrudedBuildingFootPrintHeight constant for buildings that don't have any specified
        // height. The Maps SDK for Unity currently generates geometry using the default height, but
        // does not modify the actual MapFeature data passed to the callback. This may be addressed
        // in future Maps SDK for Unity releases.
        float height = buildingShape.Extrusions[i].MaxZ > 0.1f ? buildingShape.Extrusions[i].MaxZ
                                                               : defaultBuildingHeight;

        // If a specific parapet type was given, verify it is valid.
        if (parapetType.HasValue) {
          if (parapetType.Value < 0 || parapetType.Value >= ParapetShapes.Length) {
            int invalidParapetType = parapetType.Value;
            parapetType = Random.Range(0, ParapetShapes.Length);

            Debug.LogErrorFormat(
                "{0} parapetType index given to {1}.AddBuildingParapet.\nValid " +
                    "indices are in the range of 0 to {2} based on {3} cross-sections defined in " +
                    "{1} class.\nDefaulting to randomly chosen parapetType index of {4}.",
                invalidParapetType < 0 ? "Negative" : "Invalid",
                typeof(Extruder),
                ParapetShapes.Length - 1,
                ParapetShapes.Length,
                parapetType.Value);
          }
        } else {
          // If no parapet type given, choose one at random.
          parapetType = Random.Range(0, ParapetShapes.Length);
        }

        // Use general-purpose building-extrusion function to add parapet around building. Do this
        // with a randomly chosen parapet shape for more variation throughout all created building
        // parapets.
        AddBuildingExtrusion(
            buildingGameObject,
            parapetMaterial,
            buildingShape.Extrusions[i],
            ParapetShapes[parapetType.Value],
            height,
            ref extrudedParapets,
            true,
            i,
            buildingShape.Extrusions.Length,
            DefaultWidth);
      }

      // Return all created parapets.
      return extrudedParapets.ToArray();
    }

    /// <summary>
    /// Adds a extruded shape for a given <see cref="ExtrudedArea.Extrusion"/> of a given building.
    /// </summary>
    /// <param name="buildingGameObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this building.
    /// </param>
    /// <param name="extrusionMaterial">
    /// The <see cref="Material"/> to apply to the extrusion once it is created. </param>
    /// <param name="extrusion">
    /// Current <see cref="ExtrudedArea.Extrusion"/> to extrude in given building.
    /// </param>
    /// Newly created <see cref="GameObject"/>s containing created extrusion geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given building.
    /// </para>
    /// <param name="crossSection">The 2D crossSection of the shape to loft along the path.</param>
    /// <param name="yOffset">
    /// Amount to raise created shape vertically (e.g. amount to move parapet upwards so it sits at
    /// the top of a building).
    /// </param>
    /// <param name="extrusions">
    /// Reference to list used to store all extruded geometry created by this function.
    /// </param>
    /// <param name="isParapet">
    /// Whether or not desired extrusion is a parapet (used in error message if a problem occurs).
    /// </param>
    /// <param name="extrusionIndex">
    /// Index of current extrusion (used in error message if a problem occurs).
    /// </param>
    /// <param name="totalExtrusions">
    /// Total extrusions for this building (used in error message if a problem occurs).
    /// </param>
    /// <param name="thickness">Thickness of extrusion.</param>
    private static void AddBuildingExtrusion(
        GameObject buildingGameObject, Material extrusionMaterial, ExtrudedArea.Extrusion extrusion,
        Vector2[] crossSection, float yOffset, ref List<GameObject> extrusions, bool isParapet,
        int extrusionIndex, int totalExtrusions, float thickness) {
      // Build an extrusion in local space (at origin). Note that GenerateBoundaryEdges currently
      // incorrectly handles some pathological cases, for example, building chunks with a single
      // edge starting and ending outside the enclosing tile, so some very occasional misaligned
      // extrusions are to be expected.
      List<Area.EdgeSequence> loops = PadEdgeSequences(extrusion.FootPrint.GenerateBoundaryEdges());

      for (int i = 0; i < loops.Count; i++) {
        // Try to make extrusion.
        GameObject extrusionGameObject;
        String objectName = isParapet ? ParapetName : BorderName;

        if (CanMakeLoft(
                loops[i].Vertices.ToArray(),
                extrusionMaterial,
                crossSection,
                thickness,
                objectName,
                out extrusionGameObject)) {
          // Parent the extrusion to the building object.
          extrusionGameObject.transform.parent = buildingGameObject.transform;

          // Move created extrusion to align with the building in world space (offset vertically if
          // required).
          extrusionGameObject.transform.localPosition = Vector3.up * yOffset;

          // Add to list of extrusions that will be returned for this building.
          extrusions.Add(extrusionGameObject);
        } else {
          // If extrusion failed for any reason, print an error to this effect.
          Debug.LogErrorFormat(
              "{0} class was not able to create a {1} for building \"{2}\", " +
                  "parent \"{3}\", extrusion {4} of {5}, loop {6} of {7}.\nFailure was caused by " +
                  "there being not enough vertices to make a {0}.",
              typeof(Extruder),
              isParapet ? ParapetName : BorderName,
              buildingGameObject.name,
              buildingGameObject.transform.parent.name,
              extrusionIndex + 1,
              totalExtrusions,
              i + 1,
              loops.Count);
        }
      }
    }

    /// <summary>
    /// Adds an extruded outline around the edge of an area.
    /// </summary>
    /// <remarks>
    /// This method is included for backwards compatibility. It will possibly outline internal edges
    /// in the area. For displaying a stroked outline, it's likely better to use the
    /// <see cref="AddAreaExternalOutline"/> method.
    /// </remarks>
    /// <param name="areaObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this area.
    /// </param>
    /// <param name="extrusionMaterial">
    /// The <see cref="Material"/> to apply to the outline once it has been created.
    /// </param>
    /// <param name="area">
    /// The area to be outlined.
    /// </param>
    /// <param name="outlineWidth">
    /// The width (in Unity world coordinates) of the extruded outline..
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created outline geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given area.
    /// </para></returns>
    public static GameObject[] AddAreaOutline(
        GameObject areaObject, Material extrusionMaterial, Area area, float outlineWidth) {
      return ExtrudeEdgeSequences(
          areaObject, extrusionMaterial, area.GenerateBoundaryEdges(), outlineWidth);
    }

    /// <summary>
    /// Adds an extruded outline around the edge of an area. This is useful for displaying a stroke
    /// on an area.
    /// </summary>
    /// <param name="areaObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> containing this area.
    /// </param>
    /// <param name="extrusionMaterial">
    /// The <see cref="Material"/> to apply to the outline once it has been created.
    /// </param>
    /// <param name="area">
    /// The area to be extruded.
    /// </param>
    /// <param name="outlineWidth">
    /// The width (in Unity world coordinates) of the extruded outline..
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created outline geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each part of the given area.
    /// </para></returns>
    public static GameObject[] AddAreaExternalOutline(
        GameObject areaObject, Material extrusionMaterial, Area area, float outlineWidth) {
      return ExtrudeEdgeSequences(
          areaObject, extrusionMaterial, area.GenerateExternalBoundaryEdges(), outlineWidth);
    }

    /// <summary>
    /// Extrudes a list of edge sequences outwards.
    /// </summary>
    /// <param name="areaObject">
    /// The Maps SDK for Unity created <see cref="GameObject"/> to which the extruded outline should
    /// be added.
    /// </param>
    /// <param name="extrusionMaterial">
    /// The <see cref="Material"/> to apply to the outline once it has been created.
    /// </param>
    /// <param name="edgeSequences">
    /// The edge sequences to be extruded. These should come from
    /// <see cref="Area.GenerateBoundaryEdges"/> or <see cref="Area.GenerateExternalBoundaryEdges"/>
    /// </param>
    /// <param name="outlineWidth">
    /// The width (in Unity world coordinates) of the extruded outline..
    /// </param>
    /// <returns>
    /// Newly created <see cref="GameObject"/>s containing created outline geometry.
    /// <para>
    /// One <see cref="GameObject"/> will be returned for each edge sequence.
    /// </para></returns>
    private static GameObject[] ExtrudeEdgeSequences(
        GameObject areaObject, Material extrusionMaterial, List<Area.EdgeSequence> edgeSequences,
        float outlineWidth) {
      List<Area.EdgeSequence> loops = PadEdgeSequences(edgeSequences);
      List<GameObject> result = new List<GameObject>();
      Vector2[] crossSection = OutlineCrossSectionWithWidth(outlineWidth);

      for (int i = 0; i < loops.Count; i++) {
        // Try to make extrusion.
        GameObject extrusionGameObject;

        if (CanMakeLoft(
                loops[i].Vertices.ToArray(),
                extrusionMaterial,
                crossSection,
                DefaultWidth,
                OutlineName,
                out extrusionGameObject)) {
          // Parent the extrusion to the area object.
          extrusionGameObject.transform.parent = areaObject.transform;

          // Add to list of extrusions that will be returned for this area.
          result.Add(extrusionGameObject);
        } else {
          // If extrusion failed for any reason, print an error to this effect.
          Debug.LogErrorFormat(
              "{0} class was not able to create an outline for area \"{1}\", " +
                  "loop {2} of {3}.\nFailure was caused by there being not enough vertices to " +
                  "make an outline.",
              typeof(Extruder),
              areaObject.name,
              i + 1,
              loops.Count);
        }
      }

      return result.ToArray();
    }

    /// <summary>
    /// Returns a canonical representation of the supplied <see cref="Area.EdgeSequence"/>s to
    /// facilitate easy creation of, e.g., parapets for both open and closed edge sequences.
    /// </summary>
    /// <remarks>
    /// <para>
    /// The padded edge sequences returned by this method are designed so that a continuous sequence
    /// exists from Vertices[1], to Vertices[Vertices.Count - 2] (of the returned EdgeSequence) with
    /// adjacent vertices providing proper edge tangent directions.
    /// </para><para>
    /// For closed sequences, this simply duplicates the vertices either side of the starting/ending
    /// vertex. For open sequences, new vertices are added by parallel extension of the first and
    /// last edge. This means that open sequences will have flat ends, while closed sequences can
    /// generate geometry with properly mitred first and last edge vertices.
    /// </para>
    /// </remarks>
    /// <param name="edgeSequences">The edge sequences to canonicalize.</param>
    /// <returns>Padded copies of supplied edge sequences.</returns>
    private static List<Area.EdgeSequence> PadEdgeSequences(List<Area.EdgeSequence> edgeSequences) {
      List<Area.EdgeSequence> result = new List<Area.EdgeSequence>(edgeSequences.Count);

      foreach (Area.EdgeSequence sequence in edgeSequences) {
        // Filter out any pathological sequences.
        if (sequence.Vertices.Count < 2) {
          continue;
        }

        List<Vector2> vertices = new List<Vector2>();
        vertices.AddRange(sequence.Vertices);
        int vertexCount = vertices.Count;
        Vector2 start;
        Vector2 end;

        if (vertexCount > 2 && vertices[0] == vertices[vertexCount - 1]) {
          start = vertices[vertexCount - 2];
          end = vertices[1];
        } else {
          start = vertices[0] - (vertices[1] - vertices[0]).normalized;

          end = vertices[vertexCount - 1] +
                (vertices[vertexCount - 1] - vertices[vertexCount - 2]).normalized;
        }

        vertices.Insert(0, start);
        vertices.Add(end);
        result.Add(new Area.EdgeSequence(vertices));
      }

      return result;
    }

    /// <summary>
    /// Create extrusion geometry using the supplied footprintVertices.
    /// </summary>
    /// <param name="footprintVertices">The 2D corners of the building footprint.</param>
    /// <param name="extrusionMaterial"><see cref="Material"/> to apply to created
    /// extrusion.</param> <param name="crossSection">The 2D crossSection of the shape to loft along
    /// the path.</param> <param name="thickness">Thickness of loft.</param> <param
    /// name="loftName">The name that should be given to the created game object.</param> <param
    /// name="createdLoft"> Created extrusion <see cref="GameObject"/>, at the origin with no
    /// parent, or null if lofting failed for any reason.
    /// </param>
    /// <returns>Whether or not loft could be created.</returns>
    private static bool CanMakeLoft(
        Vector2[] footprintVertices, Material extrusionMaterial, Vector2[] crossSection,
        float thickness, string loftName, out GameObject createdLoft) {
      Vector3[] vertices;
      int[] indices;
      Vector2[] uvs;

      // Attempt to make loft from given data.
      if (CanLoft(footprintVertices, crossSection, thickness, out vertices, out indices, out uvs)) {
        GameObject extrusion = new GameObject(loftName);
        MeshFilter meshFilter = extrusion.AddComponent<MeshFilter>();
        MeshRenderer meshRenderer = extrusion.AddComponent<MeshRenderer>();

        // Add a mesh cleaner to force GC on the instantiated mesh when the GameObject is destroyed.
        // Note that a bulk deletion of meshes results in a slight stuttering of the dynamic
        // loading/unloading of new map regions.
        // A proper solution would be to use an object pool and recycle mesh objects as needed.
        extrusion.AddComponent<MeshCleaner>();

        meshRenderer.material = extrusionMaterial;

        Mesh mesh = new Mesh { vertices = vertices, triangles = indices, uv = uvs };
        mesh.RecalculateNormals();
        meshFilter.mesh = mesh;
        createdLoft = extrusion;

        return true;
      }

      // If have reached this point then lofting failed.
      createdLoft = null;

      return false;
    }

    /// <summary>
    /// Creates a 3d "loft" of a shape along a path by running a given crossSection along the path.
    /// </summary>
    /// <remarks>
    /// Lofting refers to running a shape along a path, creating a 3d volume based on where the
    /// shape has travelled. For example, lofting a circle straight upwards would give a cylinder,
    /// while lofting a circle around another, larger circle would give a donut. In both cases the
    /// volume is formed by the journey of the lofted-circle along its given path. <para> Returns 3D
    /// mesh data (vertices, triangles, indices) returned in the supplied output parameter arrays.
    /// </para></remarks>
    /// <param name="paddedPath">
    /// <para>A padded version of the path along which to loft the given cross-section. This is the
    /// path along which to extrude the supplied cross-section with ghost vertices added at the
    /// beginning and end. These ghost vertices are only used to determine the direction of the path
    /// at the start and end vertices. For a closed path, these should be copies of the second to
    /// last and second vertices. For an open path the prepended ghost vertex should be the
    /// reflection of the second vertex through the first path vertex (paddedPath[0] = 2 * path[0] -
    /// path[1]), and the appended ghost vertex should be the reflection of the second to last path
    /// vertex around the last path vertex (paddedPath[last + 2] = 2 * path[last] - path[last - 1]).
    /// See <see cref="PadEdgeSequences"/> for how this is done.  This pre-padding model provides
    /// simple, unified handling of both open and closed paths, allowing lofting to work on
    /// incomplete building chunks at the edge of the loaded map region.
    /// </para><para>This path takes the form of an array of vertices, for example, defining the
    /// top-down shape of a building, such that traversing these vertices allows traversing
    /// counter-clockwise around the base of the  building. These vertices are in Vector2 format,
    /// where x and y represent x and z coordinates (i.e. 2d coordinates in a ground-plane at y =
    /// 0).
    /// </para>
    /// </param>
    /// <param name="crossSection">
    /// The 2D cross-section of the shape to loft along the given path. Much like the path, this
    /// cross-section takes the form of an array of vertices, such that traversing these vertices
    /// allows traversing counter-clockwise around the flat shape to loft. These vertices are in
    /// Vector2 format, where x and y represent coordinates in a flat plane comprising just the
    /// shape to loft.
    /// </param>
    /// <param name="thickness">Thickness of loft.</param>
    /// <param name="vertices">Outputted mesh vertices.</param>
    /// <param name="triangleIndices">Outputted mesh triangle indices.</param>
    /// <param name="uvs">Outputted mesh UVs.</param>
    /// <returns>Whether or not lofting succeeded.</returns>.
    private static bool CanLoft(
        Vector2[] paddedPath, Vector2[] crossSection, float thickness, out Vector3[] vertices,
        out int[] triangleIndices, out Vector2[] uvs) {
      // Make sure there are enough vertices to create a loft.
      if (paddedPath.Length < 2 || crossSection.Length < 2) {
        vertices = null;
        triangleIndices = null;
        uvs = null;

        return false;
      }

      // Determine the total number of vertices and triangles needed to create the lofted volume.
      // Note that the total number of triangle indices is based on the fact that each combination
      // of path-segment and cross-section segment generates a quad of two triangles, requiring 6
      // triangle indices each to represent.
      int segments = paddedPath.Length - 3;
      int totalTriangleIndices = (crossSection.Length - 1) * segments * 6;
      int trianglesPerSegment = crossSection.Length * 2 - 2;
      int verticesPerJunction = crossSection.Length * 2 - 2;
      int totalVertices = verticesPerJunction * (segments + 1);

      // Create arrays to hold vertices, uvs and triangle-indices that will be used to create the
      // lofted volume.
      vertices = new Vector3[totalVertices];
      uvs = new Vector2[totalVertices];
      triangleIndices = new int[totalTriangleIndices];

      // Perform actual lofting.
      int vertexIndex = 0;
      int triIndex = 0;
      int startCorner = 1;

      // The path has been padded by this point with ghost vertices at the beginning and end to
      // simplify the calculation of previous and next directions (see PadEdgeSequences), so we
      // ignore the first and last vertices in the following loop, only considering vertices from
      // the original path.
      for (int cornerInPath = startCorner; cornerInPath < paddedPath.Length - 1; cornerInPath++) {
        // Note that we refer to these vertices as corners, because even though they are three
        // vertices in the given path, it's better to think of what they actually represent: three
        // adjacent corners of a building's base.
        Vector3 currentCorner =
            new Vector3(paddedPath[cornerInPath].x, 0f, paddedPath[cornerInPath].y);

        Vector3 nextCorner =
            new Vector3(paddedPath[cornerInPath + 1].x, 0f, paddedPath[cornerInPath + 1].y);

        Vector3 previousCorner =
            new Vector3(paddedPath[cornerInPath - 1].x, 0f, paddedPath[cornerInPath - 1].y);

        // Get the directions from the current corner to the next and previous corners.
        Vector3 directionToPrevious = (previousCorner - currentCorner).normalized;
        Vector3 directionToNext = (nextCorner - currentCorner).normalized;

        // The path we are lofting is assumed to be in counterclockwise winding order (assuming +x
        // right, +y up). We can calculate whether the path turns left or right placing the previous
        // and next direction vectors on the ground plane and calculating the cross product of next
        // direction with previous direction. Unity uses a left handed coordinate system, so we know
        // that if this vector points down (y < 0), the vectors represent a left turn in the path.
        Vector3 turnCross = Vector3.Cross(directionToNext, directionToPrevious);
        bool isLeftTurn = turnCross.y < 0;

        // Add the previous and next edges to get their bisector -- a line that cuts this corner in
        // half. For very nearly parallel edges, the next and previous directions will face in
        // almost exactly opposite directions so the sum will be degenerately small. To avoid
        // numerical issues, we simply take the right hand perpendicular of the nextEdge.
        Vector3 bisector = directionToNext + directionToPrevious;

        // Consider lines to be colinear if the angle is less than approximately 1 degree.
        if (bisector.magnitude > 0.015f) {
          bisector.Normalize();
        } else {
          // Cross product with down gives right perpendicular in Unity's left handed coordinates.
          bisector = Vector3.Cross(directionToNext, Vector3.down);
          isLeftTurn = false;
        }

        // We wish to use the right bisector as the x-axis for the coordinates in the cross section
        // of the loft. If the path turns left at the current point, the bisector of the inner angle
        // will also point left, so we need to reverse it.
        // Note that for lofting around buildings, the path of the walls is counterclockwise, when
        // viewed from above, so the rightBisector will point away from the building.
        Vector3 rightBisector;

        if (isLeftTurn) {
          rightBisector = -bisector;
        } else {
          rightBisector = bisector;
        }

        // Given the previous edge, imagine a new, extruded edge, parallel to the previous edge but
        // extruded outwards by the desired extrusion width. If we make a similar extruded edge
        // pushed out from the next edge, and find where these two edges intersect, we get a new,
        // extruded corner - the corner of an extrusion that is of uniform length all the way
        // around the shape.
        //
        //                       Previous Edge, Extruded
        //     Extruded Corner ●──────────○──────────────────────────
        //                    ╱           ╎_|
        //                   ╱            ╎
        //                  ╱             ╎
        //    Next Edge,   ╱              ╎         Extrusion Width
        //      Extruded  ╱               ╎
        //               ╱                ╎
        //              ╱                 ╎
        //             ╱                  |      Previous Edge
        //            ╱    Current Corner ●───────────────────────
        //           ╱                   ╱
        //          ╱                   ╱
        //         ╱                   ╱  Next Edge
        //        ╱                   ╱
        //
        // We could calculate these lines and find their intersection. But vectors give us a
        // shortcut.
        //
        // To do this, we get the right hand bisector of this corner (an outward pointing line that
        // cuts the corner in half, such that the angle between this bisector and the previous edge
        // is the same as the angle between this bisector and the next edge). If we can determine
        // the length of this bisector, we will know exactly how far to travel along it from the
        // current corner to the new, extruded corner.
        //
        //                       Previous Edge, Extruded
        //     Extruded Corner ●────────○──────────────────────────
        //                    ╱ ╲       |
        //                   ╱   ╲      │
        //                  ╱     ╲     │
        //                 ╱       ╲    │ E
        //                ╱      B  ╲   │
        //               ╱           ╲  │
        //              ╱             ╲ │_
        //             ╱               ╲│ │
        //            ╱  Current Corner ●───────────────────────>
        //           ╱                 ╱   Previous direction vector
        //          ╱                 ╱
        //         ╱                 ╱
        //        ╱                 ╱
        //
        // In this diagram 'E' is the 'extrusion vector', the vector between the current corner and
        // the previous, extruded edge (whose length |E| is the desired extrusion width). We can get
        // this extrusion vector using the cross product of the previous direction vector (pointing
        // back along the previous edge) with the up vector to find the left hand perpendicular
        // vector of the previous direction vector as shown above. (Note that Unity uses a left
        // handed coordinate system, giving us the left hand perpendicular from the cross product)
        Vector3 normalizedExtrusionVector =
            Vector3.Cross(directionToPrevious, Vector3.up).normalized;

        // In the diagram above, if we project the vector B onto a unit vector in the direction of
        // E (unitE) we get the vector E. This implies that the length of this projected vector is
        // the same as the length of E, which is just the extrusion distance. This gives us:
        //   B . unitE = |E|
        // which, given that B is just a unit vector in the direction of B times |B|, gives:
        //   (|B| unitB) . unitE = |E|
        // giving:
        //  |B| (unitB . unitE) = |E|
        // leading to:
        //  |B| = |E| / (unitB . unitE)
        //
        // We know |E| is just the given thickness, and have calculated unitE as
        // normalizeExtrusionVector and unitB as rightBisector, which allows us to calculate the
        // length of B as distanceToExtrude as follows.
        float distanceToExtrude = thickness / Vector3.Dot(normalizedExtrusionVector, rightBisector);

        // Make sure this extrusion distance is not unrealistically long (can occur for extremely
        // sharp angles) by limiting it to twice the desired Extrusion Width.
        if (distanceToExtrude > 2f * thickness) {
          distanceToExtrude = 2f * thickness;
        }

        // Create a copy of the cross-section shape using a coordinate system where the x-axis is
        // the rightBisector and the y-axis is the up vector. This aligns the shape to the plane
        // bisecting the joint angle in the path. When all these cross-sections are joined, they
        // will form a loft of the given cross-section around the given shape.
        for (int pointInCrossSection = 0; pointInCrossSection < crossSection.Length;
             pointInCrossSection++) {
          // Align the values of this cross-section point to this corner - i.e. convert from a 2D,
          // x, y shape into a 3D x, y, z shape aligned to the desired extrusion direction. Also
          // multiply the x values by the extrusion distance, to make sure that the loft is
          // stretched to the desired extrusion distance away from all sides of the shape.
          Vector3 pv = crossSection[pointInCrossSection].x * rightBisector * distanceToExtrude;
          Vector3 uv = crossSection[pointInCrossSection].y * Vector3.up;
          Vector3 vertex = currentCorner + pv + uv;
          vertices[vertexIndex] = vertex;
          uvs[vertexIndex] = new Vector3(vertex.x + vertex.y, vertex.z + vertex.y) / UvScale;

          // Generate triangle-indices needed to connect this edge of this cross-section to the
          // same edge of the next cross-section.
          if (pointInCrossSection > 0 && cornerInPath > startCorner) {
            triangleIndices[triIndex++] = SafeMod(totalVertices, vertexIndex - 1);

            triangleIndices[triIndex++] =
                SafeMod(totalVertices, vertexIndex - trianglesPerSegment - 1);
            triangleIndices[triIndex++] = SafeMod(totalVertices, vertexIndex);
            triangleIndices[triIndex++] = SafeMod(totalVertices, vertexIndex);

            triangleIndices[triIndex++] =
                SafeMod(totalVertices, vertexIndex - trianglesPerSegment - 1);
            triangleIndices[triIndex++] = SafeMod(totalVertices, vertexIndex - trianglesPerSegment);
          }

          // Copy vertices so can have un-smoothed normals. In order to have normals be unique to
          // each face, each corner must have multiple vertices, one for each face.
          if (pointInCrossSection > 0 && pointInCrossSection < crossSection.Length - 1) {
            vertices[vertexIndex + 1] = vertices[vertexIndex];
            uvs[vertexIndex + 1] = uvs[vertexIndex];
            vertexIndex++;
          }

          // Move to the next vertex to store in the generated loft.
          vertexIndex++;
        }
      }

      // If have reached this point then have successfully created loft.
      return true;
    }

    /// <summary>A version of mod that works for negative values.</summary>
    /// <remarks>
    /// This function ensures that returned modulated value will always be positive for values
    /// greater than the negative of the modulus argument.
    /// </remarks>
    /// <param name="mod">The modulus argument.</param>
    /// <param name="val">The value to modulate.</param>
    /// <returns>A range safe version of value % mod.</returns>
    private static int SafeMod(int mod, int val) {
      return (val + mod) % mod;
    }

    /// <summary>
    /// Convert a given array of floats into an array of <see cref="Vector2"/>s.
    /// </summary>
    /// <remarks>
    /// Each pair of arguments becomes one Vector2, with an exception triggered if the number of
    /// floats given is not even.
    /// </remarks>
    private static Vector2[] MakeVector2Array(params float[] floats) {
      // Confirm an even number of floats have been given.
      if (floats.Length % 2 != 0) {
        throw new ArgumentException("Arguments must be provided in pairs");
      }

      // Return each pair of floats as one element of an array of Vector2's.
      Vector2[] vectors = new Vector2[floats.Length / 2];

      for (int i = 0; i < floats.Length; i += 2) {
        vectors[i / 2] = new Vector2(floats[i], floats[i + 1]);
      }

      return vectors;
    }
  }
}
