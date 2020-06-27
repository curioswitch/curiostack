// Maps Unity SDK Standard Shader.
// Diffuse and specular lighting with fog and shadows (casting and receiving).
Shader "Google/Maps/Shaders/Standard" {
  Properties {
    _MainTex("Texture", 2D) = "white" {}
    _Color("Color", Color) = (1.0, 1.0, 1.0, 1.0)
    _Shininess("Specular Shininess", Float) = 10.0
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
      #include "AutoLight.cginc" // Shadow functions and macros.

      // User defined values.
      sampler2D _MainTex;
      float4 _MainTex_ST;
      uniform float4 _Color;
      uniform float _Shininess;

      // Vertex Shader input.
      struct vertexInput {
        float4 vertex : POSITION; // Vertex worldspace position.
        float2 uv : TEXCOORD0; // Vertex UV coordinates.
        float3 normal : NORMAL; // Vertex normal vector.
      };

      // Fragment Shader Input.
      struct vertexOutput {
        float4 pos : SV_POSITION; // Vertex screenspace position.
        float3 normal : NORMAL; // Vertex normal vector.
        float2 uv : TEXCOORD0; // Vertex UV coordinates.
        float4 vertex : TEXCOORD1; // Vertex worldspace position.
        SHADOW_COORDS(2) // Shadow data as TEXCOORD2.
        UNITY_FOG_COORDS(3) // Per-vertex fog as TEXCOORD3.
      };

      // Vertex Shader.
      // Note that input must be called 'v' for TRANSFER_SHADOW to work.
      vertexOutput vert(vertexInput v) {
        // Computer screenspace position.
        vertexOutput output;
        output.pos = UnityObjectToClipPos(v.vertex);

        // Generate texture coordinates.
        output.uv = v.uv;

        // Get vertex worldspace position and normals for lighting calculation
        // in fragment shader.
        float4x4 modelMatrix = unity_ObjectToWorld;
        float4x4 modelMatrixInverse = unity_WorldToObject;

        output.vertex = mul(modelMatrix, v.vertex);
        output.normal = normalize(mul(float4(v.normal, 0.0),
            modelMatrixInverse).xyz);

        // Compute shadows data (requires input to be 'v').
        TRANSFER_SHADOW(output)

        // Apply fog.
        UNITY_TRANSFER_FOG(output, output.pos);
        return output;
      }

      // Fragment Shader.
      fixed4 frag(vertexOutput input) : SV_Target {
        // Start with color.
        fixed3 diffColor = tex2D(_MainTex, input.uv) * _Color;
        fixed3 specColor = _Color;

        // Calculate shadow value.
        fixed shadow = SHADOW_ATTENUATION (input);

        // Calculate lighting.
        float3 normalDirection = normalize(input.normal);
        float3 viewDirection = normalize(_WorldSpaceCameraPos
            - input.vertex.xyz);
        float3 lightDirection;
        float attenuation;

        // If light is directional, have no attenuation (no falloff).
        if (0.0 == _WorldSpaceLightPos0.w) {
          attenuation = 1.0;
          lightDirection = normalize(_WorldSpaceLightPos0.xyz);
        } else { // For point or spot light, use linear attenuation.
          float3 vertexToLightSource = _WorldSpaceLightPos0.xyz
              - input.vertex.xyz;
          float distance = length(vertexToLightSource);
          attenuation = 1.0 / distance;
          lightDirection = normalize(vertexToLightSource);
        }

        // Calculate ambient and diffuse lighting.
        float3 ambientLighting = UNITY_LIGHTMODEL_AMBIENT.rgb
            * diffColor;

        float3 diffuseReflection = shadow * attenuation * _LightColor0.rgb
            * diffColor * max(0.0, dot(normalDirection, lightDirection));

        // If light source is on the wrong side, have no specular light.
        float3 specularReflection;
        if (dot(normalDirection, lightDirection) < 0.0) {
           specularReflection = float3(0.0, 0.0, 0.0);
        } else {
          specularReflection =
              shadow * attenuation * _LightColor0.rgb * specColor *
              pow(max(0.0, dot(reflect(-lightDirection, normalDirection),
              viewDirection)), _Shininess);
        }

        // Collection lighting together.
        fixed4 finalColor = fixed4(ambientLighting + diffuseReflection
            + specularReflection, 1.0);

        // Apply fog.
        UNITY_APPLY_FOG(input.fogCoord, finalColor);
        return finalColor;
      }
      ENDCG
    }

    // Pull in shadow caster pass from VertexLit built in-shader.
    // Remove to make this receive shadows only (but not cast them).
    UsePass "Legacy Shaders/VertexLit/SHADOWCASTER"
  }

  // Fallback to default diffuse textured shader.
  Fallback "Standard"
}
