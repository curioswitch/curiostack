// Worldspace textured shader.
// Diffuse and specular lighting with fog.
Shader "Google/Maps/Shaders/Worldspace Buildings" {
  Properties {
    // Map of regular texture, where white and black map to _Color and _AltColor
    // respectively.
    _MainTex("Texture", 2D) = "white" {}

    // Specular (shiny) texture and shininess (gloss) value.
    _SpecColor("Specular Color", Color) = (1.0, 1.0, 1.0, 1.0)
    _Shininess("Specular Shininess", Float) = 10.0

    // Offset applied to worldspace texture coordinates. Only the first and
    // third (x and z) value of this Vector are used, providing a top-down
    // offset.
    _Offset("Worldspace Offset", Vector) = (0.0, 0.0, 0.0, 0.0)
  }

  SubShader {
    Pass {
      Tags { "LightMode" = "ForwardBase" }

      Cull Back // Cull backfaces.
      ZWrite On // Use Z-buffer.
      ZTest LEqual // Use normal (less-equal) z-depth check.

      CGPROGRAM
      #pragma vertex vert // Vertex shader.
      #pragma fragment frag // Fragment shader.
      #pragma multi_compile_fog // To make fog work.

      #include "UnityCG.cginc" // Standard unity helper functions.
      #include "Lighting.cginc" // Lighting functions.

      // User defined values.
      sampler2D _MainTex;
      uniform float4 _Offset;
      uniform float _Shininess;
      uniform float4 _MainTex_ST;

      // Vertex Shader input.
      struct vertexInput {
        float4 vertex : POSITION; // Vertex worldspace position.
        float3 normal : NORMAL; // Vertex normal vector.
        float2 uv : TEXCOORD0; // Vertex UV coordinates.
      };

      // Fragment Shader Input.
      struct vertexOutput {
        float4 pos : SV_POSITION; // Vertex screenspace position.
        float4 vertex : TEXCOORD0; // Vertex worldspace position.
        float3 normal : NORMAL; // Vertex normal vector.
        float2 uv : TEXCOORD1; // Vertex UV coordinates.
        UNITY_FOG_COORDS(2) // Per-vertex fog as TEXCOORD2.
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
        output.normal
            = normalize(mul(float4(v.normal, 0.0), modelMatrixInverse).xyz);

        // Convert worldspace position to uv coordinates. Translate x position
        // to u coordinates, and both z and y positions to v coordinates. This
        // is so the texture will tile both top-down (x and z positions) and
        // with height (y position). Also apply the given texture offset.
        output.uv = float2(
            (output.vertex.x + _Offset.x) / _MainTex_ST.x,
            (output.vertex.z + _Offset.z) / _MainTex_ST.y
            + output.vertex.y / _MainTex_ST.y);

        // Apply fog.
        UNITY_TRANSFER_FOG(output, output.pos);
        return output;
      }

      // Fragment Shader.
      fixed4 frag(vertexOutput input) : SV_Target {
        // Start with given diffuse texture and specular color.
        fixed3 diffColor = tex2D(_MainTex, input.uv);
        fixed3 specColor = _SpecColor;

        // Calculate lighting.
        float3 normalDirection = normalize(input.normal);
        float3 viewDirection =
            normalize(_WorldSpaceCameraPos - input.vertex.xyz);
        float3 lightDirection;
        float attenuation;

        // If light is directional, have no attenuation (no falloff).
        if (0.0 == _WorldSpaceLightPos0.w) {
          attenuation = 1.0;
          lightDirection = normalize(_WorldSpaceLightPos0.xyz);
        } else { // For point or spot light, use linear attenuation.
          float3 vertexToLightSource
              = _WorldSpaceLightPos0.xyz - input.vertex.xyz;
          float distance = length(vertexToLightSource);
          attenuation = 1.0 / max(distance, 0.0001);
          lightDirection = normalize(vertexToLightSource);
        }

        // Calculate ambient and diffuse lighting.
        float3 ambientLighting = UNITY_LIGHTMODEL_AMBIENT.rgb * diffColor;

        float3 diffuseReflection
            = attenuation * _LightColor0.rgb * diffColor
            * max(0.0, dot(normalDirection, lightDirection));

        // If light source is on the wrong side, have no specular light.
        float3 specularReflection;
        if (dot(normalDirection, lightDirection) < 0.0) {
           specularReflection = float3(0.0, 0.0, 0.0);
        } else {
          specularReflection = attenuation * _LightColor0.rgb
              * specColor * pow(max(0.0, dot(reflect(-lightDirection,
              normalDirection), viewDirection)), _Shininess);
        }

        // Bring together lighting.
        fixed4 finalColor = fixed4(
            ambientLighting + diffuseReflection + specularReflection, 1.0);

        // Apply fog.
        UNITY_APPLY_FOG(input.fogCoord, finalColor);
        return finalColor;
      }
      ENDCG
    }
  }

  // Fallback to default diffuse textured shader.
  Fallback "Specular"
}
