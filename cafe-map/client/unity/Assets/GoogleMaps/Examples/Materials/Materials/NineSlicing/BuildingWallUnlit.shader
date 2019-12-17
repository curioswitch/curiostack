// 9-sliced Building Shader.
// Version without lighting.
Shader "Google/Maps/Shaders/Wall (9 Sliced), Unlit" {
  Properties {
    // Regular (diffuse) texture.
    _MainTex("Texture", 2D) = "white" {}

    // =============== NINE SLICING ============================================
    // ShaderLab properties for performing nine-slicing using
    // GoogleMapsShaderLib.
    // =========================================================================

    // Size of the textures used in the shader, in Unity units.
    _TexWidth("Width", Float) = 1
    _TexHeight("Height", Float) = 1

    // Number of divisions to use when dividing texture.
    _TexDivisions("Divisions", Int) = 8
    // This value is not strictly required for Nine Slicing to work, but is used
    // for convenience.
    //
    // Nine Slicing divides a texture by cutting it 4 times to form 9 areas
    // (include corners):
    //
    //                 ╎             ╎
    //        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎    Upper    ╎        ┃
    //        ┃        ╎             ╎        ┃
    //      ╌╌┃╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌┃╌╌
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎             ╎        ┃
    //        ┃  Left  ╎    Middle   ╎  Right ┃
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎             ╎        ┃
    //      ╌╌┃╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌┃╌╌
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎    Lower    ╎        ┃
    //        ┃        ╎             ╎        ┃
    //        ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    //                 ╎             ╎
    //
    // The placement of these cuts is determined using u and v values between 0
    // and 1. For example, if we wanted to place each cut a quarter of the way
    // into the image, we'd give the following u and v values:
    //   Left = 0.25.
    //   Right = 0.75.
    //   Lower = 0.25.
    //   Upper = 0.75.
    //
    //  (0, 1)         ╎             ╎         (1, 1)
    //        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎     0.75    ╎        ┃
    //        ┃        ╎             ╎        ┃
    //      ╌╌┃╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌┃╌╌
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎             ╎        ┃
    //        ┃  0.25  ╎             ╎  0.75  ┃
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎             ╎        ┃
    //      ╌╌┃╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌╌╌╌╌╌╎╌╌╌╌╌╌╌╌┃╌╌
    //        ┃        ╎             ╎        ┃
    //        ┃        ╎     0.25    ╎        ┃
    //        ┃        ╎             ╎        ┃
    //        ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    //  (0, 0)         ╎             ╎         (1, 0)
    //
    // The problem is that even though we want all 4 cuts to be the same
    // distance from their respective sides, we need to give different different
    // values to achieve this (2x 0.25 and 2x 0.75). This is unintuitive.
    //
    // Additionally, these u and v values are always going to be divisions of 1,
    // e.g. 1/4, 1/8, 1/3, and it's much easier to be able to give these as
    // fractions than to have to continually calculate and recalculate them as
    // decimals. For example, if we wanted to have each cut be a sixth of the
    // way into the image, we could say:
    //   Left = 0.1667.
    //   Right = 0.8333.
    //   Lower = 0.1667.
    //   Upper = 0.8333.
    //
    // But it would be much easier to say Left = Right = Lower = Upper = 1/6.
    //
    // This is why a 'number of divisions' is given, to allow these cuts to be
    // more intuitively specified as fractions of 1, all from the closest edge
    // with no additional conversions of calculations required.

    // Nine-slicing bounds. As described above, bounds are multiples of the
    // specified number of divisions. For example, if divisions is 4, then a
    // left bound of 1 will be 1/4 from the left edge (or 0.25).
    _TexLeft("Left", Int) = 0
    _TexRight("Right", Int) = 0
    _TexLower("Lower", Int) = 0
    _TexUpper("Upper", Int) = 0

    // =========================================================================
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

      // 9 slicing functions.
      #include "Assets/GoogleMaps/Materials/GoogleMapsShaderLib.cginc"

      // User defined values.
      sampler2D _MainTex;
      float4 _MainTex_ST;
      uniform float _TexWidth, _TexHeight, _TexDivisions, _TexLower, _TexUpper;
      uniform float _TexLeft, _TexRight;

      // Vertex Shader input.
      struct vertexInput {
        float4 vertex : POSITION; // Vertex worldspace position.
        float4 uv : TEXCOORD0; // Nine slicing, vertex UV coordinates, of the
      };                       // form (x, y, face width, face height).

      // Fragment Shader Input.
      struct vertexOutput {
        float4 pos : SV_POSITION; // Vertex screenspace position.
        float4 vertex : TEXCOORD1; // Vertex worldspace position.
        float4 bounds : TEXCOORD2; // 9 slicing bounds.
        UNITY_FOG_COORDS(3) // Per-vertex fog as TEXCOORD3.
        float4 uv : TEXCOORD0; // Vertex UV coordinates, of the form (x, y,
      };                       // face width, face height).

      // Vertex Shader.
      vertexOutput vert(vertexInput v) {
        // Computer screenspace position.
        vertexOutput output;
        output.pos = UnityObjectToClipPos(v.vertex);

        // Generate texture coordinates.
        output.uv = v.uv;

        // Get vertex worldspace position and normals for
        // lighting calculation in fragment shader.
        float4x4 modelMatrix = unity_ObjectToWorld;
        float4x4 modelMatrixInverse = unity_WorldToObject;
        output.vertex = mul(modelMatrix, v.vertex);

        // Compute bounds (computed here to avoid unncessary calculations per
        // fragment).
        output.bounds = float4(
          _TexLeft / _TexDivisions,
          1.0 - _TexRight / _TexDivisions,
          _TexLower / _TexDivisions,
          1.0 - _TexUpper / _TexDivisions);

        // Apply fog.
        UNITY_TRANSFER_FOG(output, output.pos);
        return output;
      }

      // Fragment Shader.
      fixed4 frag(vertexOutput input) : SV_Target {
        // ==================== NINE SLICING =======================
        // Apply nine-slicing to the UV coordinates of the fragment.
        // =========================================================

        nineSliceResult nineSliced = nineSlice(
          float2(input.uv[0], input.uv[1]), // Position of fragment within quad.
          float2(input.uv[2], input.uv[3]), // Size of the quad.
          float2(_TexWidth, _TexHeight), // Size of the texture.
          input.bounds.x, input.bounds.y, // Left/Right nine-slicing bounds.
          input.bounds.z, input.bounds.w // Lower/Upper nine-slicing bounds.
        );

        // =========================================================

        // Apply fog to textured color, and return.
        fixed4 color = tex2D(_MainTex, nineSliced.uv);
        UNITY_APPLY_FOG(input.fogCoord, color);
        return color;
      }
      ENDCG
    }
  }

  // Fallback to default diffuse textured shader.
  Fallback "Unlit/Texture"
}
