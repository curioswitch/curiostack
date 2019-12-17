// BaseMap shader for use on ground/water regions.
// Unlit version.
Shader "Google/Maps/Shaders/BaseMap Textured, Unlit" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)
    _MainTex ("Albedo (RGB)", 2D) = "white" {}
  }
  SubShader {
    Tags { "RenderType"="Opaque" }

    LOD 200

    // Basemap renders multiple coincident ground plane features so we have to
    // disable z testing (make it always succeed) to allow for overdraw.
    ZTest Always

    CGPROGRAM
    #pragma surface surf NoLighting noambient noshadow alpha:blend

    // Input parameters.
    sampler2D _MainTex;
    fixed4 _Color;

    // Vertex input.
    struct Input {
      half2 uv_MainTex;
    };

    // Custom lighting model (which just returns unlit color).
    fixed4 LightingNoLighting(SurfaceOutput surface, fixed3 lightDirection,
        fixed attenutation) {
      fixed4 color;
      color.rgb = surface.Albedo;
      color.a = surface.Alpha;
      return color;
    }

    // Surface shader itself.
    void surf (Input input, inout SurfaceOutput output) {
      fixed4 color = tex2D(_MainTex, input.uv_MainTex) * _Color;
      output.Albedo = color.rgb;
      output.Alpha = color.a;
    }
    ENDCG
  }
  Fallback "Unlit/Texture"
}
