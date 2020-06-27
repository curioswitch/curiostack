// Un-textured Color Shader.
// Version without lighting.
Shader "Google/Maps/Shaders/Color, Unlit" {
  Properties {
    // Regular (diffuse) color.
    _Color("Color", Color) = (1.0, 1.0, 1.0, 1.0)
  }

  SubShader {
    Pass {
      Cull Back // Cull backfaces.
      ZWrite On // Use Z-buffer.
      ZTest LEqual // Use normal (less-equal) z-depth check.

      CGPROGRAM
      #pragma vertex vert // Vertex shader.
      #pragma fragment frag // Fragment shader.
      #pragma multi_compile_fog // To make fog work.

      #include "UnityCG.cginc" // Standard unity helper functions.

      // User defined values.
      uniform float4 _Color;

      // Vertex Shader input.
      struct vertexInput {
        float4 vertex : POSITION; // Vertex worldspace position.
      };

      // Fragment Shader Input.
      struct vertexOutput {
        float4 pos : SV_POSITION; // Vertex screenspace position.
        float4 vertex : TEXCOORD0; // Vertex worldspace position.
        UNITY_FOG_COORDS(1) // Per-vertex fog as TEXCOORD1.
      };

      // Vertex Shader.
      vertexOutput vert(vertexInput v) {
        // Computer screenspace position.
        vertexOutput output;
        output.pos = UnityObjectToClipPos(v.vertex);

        // Get vertex worldspace position and normals for
        // lighting calculation in fragment shader.
        float4x4 modelMatrix = unity_ObjectToWorld;
        float4x4 modelMatrixInverse = unity_WorldToObject;
        output.vertex = mul(modelMatrix, v.vertex);

        // Apply fog.
        UNITY_TRANSFER_FOG(output, output.pos);
        return output;
      }

      // Fragment Shader.
      fixed4 frag(vertexOutput input) : SV_Target {
        // Apply fog to color and return.
        fixed4 color = _Color;
        UNITY_APPLY_FOG(input.fogCoord, color);
        return color;
      }
      ENDCG
    }
  }

  // Fallback to default diffuse textured shader.
  Fallback "Unlit/Color"
}
