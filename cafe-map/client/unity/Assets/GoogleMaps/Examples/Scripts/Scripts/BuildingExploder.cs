using System.Collections.Generic;
using UnityEngine;

namespace GoogleMaps.Examples.Scripts {
  /// <summary>
  /// Script to blow a building up into cubes.
  /// </summary>
  public class BuildingExploder : MonoBehaviour {
    /// <summary>
    /// Maximum chunks to spawn when a building explodes.
    /// </summary>
    private const int MAX_CHUNKS_PER_BUILDING = 50;

    /// <summary>
    /// The material to apply to spawned chunks.
    /// </summary>
    public Material ChunkMaterial;

    /// <summary>
    /// Estimate the volume of the building.
    /// </summary>
    /// <returns></returns>
    private float EstimateVolume() {
      Renderer buildingRenderer = gameObject.GetComponent<Renderer>();
      Bounds bounds = buildingRenderer.bounds;
      Vector3 size = bounds.size;

      return size.x * size.y * size.z;
    }

    /// <summary>
    /// Spawn and throw a chunk with the specified volume.
    /// </summary>
    /// <param name="explosionPosition">The position of the explosion force.</param>
    /// <param name="volume">The volume of the chunk.</param>
    private void ThrowChunk(Vector3 explosionPosition, float volume) {
      float dims = Mathf.Pow(volume, 1.0f/3.0f);

      GameObject chunk = GameObject.CreatePrimitive(PrimitiveType.Cube);
      chunk.transform.localScale = Vector3.one * dims;
      chunk.transform.position = gameObject.transform.position;
      Rigidbody rigidbody = chunk.AddComponent<Rigidbody>();
      rigidbody.AddExplosionForce(10f, explosionPosition, 50f, 5f);

      Renderer chunkRenderer = chunk.GetComponent<Renderer>();
      chunkRenderer.sharedMaterial = ChunkMaterial;
    }

    /// <summary>
    /// Throw out randomly sized chunks until we've ejected the estimated volume of the
    /// building.
    /// </summary>
    /// <param name="explosionPosition">The position of the explosion.</param>
    public void Explode(Vector3 explosionPosition) {
      float volumeRemaining = EstimateVolume();

      System.Random random = new System.Random();
      int chunks = 0;
      while ((volumeRemaining > 0) && (chunks < MAX_CHUNKS_PER_BUILDING)) {
        float chunkVolume = 0.5f + random.Next(0, 30);
        chunkVolume = Mathf.Min(chunkVolume, volumeRemaining);
        ThrowChunk(explosionPosition, chunkVolume);
        volumeRemaining -= chunkVolume;
        chunks++;
      }

      Destroy(gameObject);
    }
  }
}
