// BaseMap shader for use on ground/water regions.
// Unlit version without texture (just an input color).
Shader "Google/Maps/Shaders/BaseMap Color, Unlit" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
  }
  SubShader {
    Tags { "RenderType"="Opaque" }

    LOD 200

    // Basemap renders multiple coincident ground plane features so we have to
    // disable z testing (make it always succeed) to allow for overdraw.
    ZTest Always

    CGPROGRAM
    #pragma surface surf NoLighting noambient noshadow

    // Input parameters.
    fixed4 _Color;

    // Vertex input.
    struct Input {
      half2 uv_MainTex;
    };

    // Custom lighting model (which just returns unlit color).
    fixed4 LightingNoLighting(SurfaceOutput surface, fixed3 lightDirection,
        fixed attenutation) {
      return fixed4(surface.Albedo, surface.Alpha);
    }

    // Surface shader itself.
    void surf (Input input, inout SurfaceOutput output) {
      output.Albedo = _Color;

    }
    ENDCG
  }
  Fallback "Unlit/Texture"
}
